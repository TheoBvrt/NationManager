package ch.swaford.servermanager;

import ballistix.api.event.BlastDamageEvent;
import ballistix.api.event.BlastEvent;
import ballistix.api.event.LaunchEvent;
import ch.swaford.servermanager.classement.ClassementManager;
import ch.swaford.servermanager.classement.ClassementCache;
import ch.swaford.servermanager.explosion.ExplosionManager;
import ch.swaford.servermanager.explosion.StoredBlock;
import ch.swaford.servermanager.networktransfer.ServerClaimDataPayload;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.scores.*;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.util.*;

/*
 * Classe FactionCommands
 *
 * Cette classe gère :
 * - Les événements liés aux joueurs :
 *      -> Vérification du propriétaire du chunk courant (updateChunkOwner).
 *      -> Mise à jour du scoreboard personnel toutes les secondes (updateScoreBoard).
 * - L’enregistrement des commandes du jeu (via l’événement RegisterCommandsEvent).
 *
 * Commandes principales disponibles :
 *   /nation create <name>     → Crée une nouvelle nation.
 *   /nation delete <name>     → Supprime la nation du joueur (si propriétaire).
 *   /nation leave             → Quitte la nation courante (sauf si propriétaire).
 *   /nation setowner <player> → Transfère la propriété à un autre membre.
 *   /nation addplayer <player>→ Ajoute un joueur à la nation (si OP/owner).
 *   /nation removeplayer <p>  → Retire un joueur de la nation.
 *   /nation info [nation]     → Affiche les infos de sa propre nation ou d’une autre.
 *   /nation list              → Liste les 10 premières nations.
 *   /nation promote <player>  → Passe un membre en officier.
 *   /nation demote <player>   → Rétrograde un officier en membre.
 *   /nation deposit <amount>  → Dépose de l’argent dans la banque de la nation.
 *   /nation withdraw <amount> → Retire de l’argent (si propriétaire/officier).
 *   /nation balance           → Affiche le solde de la nation.
 *   /nation claim             → Réclame un chunk pour la nation (si possible).
 *   /nation unclaim           → Abandonne un chunk possédé par la nation.
 *
 * Notes :
 * - Le prix de claim de base est défini par claimPrice (100 par défaut).
 * - En cas de surclaim (prise de chunk déjà occupé), le coût dépend du rapport
 *   de puissance entre la nation attaquante et défendante.
 * - Les permissions sont strictes :
 *      -> Owner = tous les droits.
 *      -> Officiers = gestion de territoire, ajout/retrait joueurs, finances.
 *      -> Membres = droits limités (pas de gestion).
 * - Les messages envoyés aux joueurs sont formatés avec des couleurs (§).
 */


public class FactionCommands {
    private final Map<UUID, ChunkPos> chunkPosMap = new HashMap<>();
    int claimPrice = ServerVariable.claimBasePrice;

    @SubscribeEvent
    public void onEntityKilled(LivingDeathEvent event) {
        LivingEntity livingEntity = event.getEntity();
        DamageSource damageSource = event.getSource();
        if (!(damageSource.getEntity() instanceof Player)) return;

        if (!(livingEntity instanceof Player target)) return;

        if (PlayerDataBase.playerHasFaction(target.getStringUUID())) {
            int balance = FactionManager.getBalance(target.getStringUUID());
            int amount = (int)(balance * ServerVariable.killPlayerMalusPercent);
            if (balance >= amount && balance > 0) {
                FactionManager.subMoney(PlayerDataBase.getPlayerFaction(target.getStringUUID()), amount);
            }
        }
    }

    @SubscribeEvent
    public void onBlastKill(BlastDamageEvent.BlastDamageMissileEvent event) {
        LivingEntity livingEntity = event.entity;

        if (!(livingEntity instanceof Player target)) return;
        if (PlayerDataBase.playerHasFaction(target.getStringUUID())) {
            String factionName = PlayerDataBase.getPlayerFaction(target.getStringUUID());
            int balance = FactionManager.getBalance(factionName);
            int amount = (int)(balance * ServerVariable.killPlayerMalusPercent);
            if (balance >= amount && balance > 0) {
                FactionManager.subMoney(factionName, amount);
            }
        }
    }

    @SubscribeEvent
    public void onLaunch(LaunchEvent.LaunchMissileEvent event) {
        MinecraftServer server = event.world.getServer();
        ChunkPos chunkPos = new ChunkPos(event.startPos);
        String claimOwner = ClaimManager.getClaimOwner(chunkPos.x, chunkPos.z);
        server.getPlayerList().broadcastSystemMessage(
                Component.literal(String.format(
                        "§7Lancement de missile détecté depuis §e[%s]§7.",
                        claimOwner
                )),
                false // false = message non "system" (pas dans la console)
        );
    }

    private void performExplosion(int radius, ServerLevel level, BlockPos pos)
    {
        List<BlockState> palette = new ArrayList<>();
        List<StoredBlock> blocks = new ArrayList<>();

        BlockPos center = pos;
        float randomness = 0.85f;
        int maxDepth = 3;
        Set<Block> blacklist = Set.of(
                Blocks.BEDROCK,
                Blocks.END_PORTAL_FRAME,
                Blocks.COMMAND_BLOCK,
                Blocks.BEACON,
                Blocks.ENDER_CHEST
        );
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dy < -maxDepth) continue;
                    double distanceSq = dx * dx + dy * dy + dz * dz;
                    if (distanceSq <= radius * radius) {

                        BlockPos targetPos = center.offset(dx, dy, dz);
                        BlockState state = level.getBlockState(targetPos);

                        if (!state.isAir() && !blacklist.contains(state.getBlock())) {
                            // ✅ on capture avant destruction
                            if (!palette.contains(state)) {
                                palette.add(state);
                            }
                            int id = palette.indexOf(state);

                            // Récupérer le NBT si BlockEntity
                            CompoundTag nbt = null;
                            BlockEntity be = level.getBlockEntity(targetPos);
                            if (be != null) {
                                nbt = be.saveWithFullMetadata(level.registryAccess());
                            }

                            blocks.add(new StoredBlock(
                                    targetPos.getX(),
                                    targetPos.getY(),
                                    targetPos.getZ(),
                                    id,
                                    nbt
                            ));
                        }

//                        if (!state.isAir() && !blacklist.contains(state.getBlock())) {
//                            if (dy == -maxDepth) {
//                                if (level.random.nextFloat() < randomness) {
//                                    if (!palette.contains(state)) {
//                                        palette.add(state);
//                                    }
//                                    int id = palette.indexOf(state);
//                                    blocks.add(new StoredBlock(
//                                            targetPos.getX(),
//                                            targetPos.getY(),
//                                            targetPos.getZ(),
//                                            id,
//                                            null
//                                    ));
//                                    level.destroyBlock(targetPos, true);
//                                }
//                            } else {
//                                if (!palette.contains(state)) {
//                                    palette.add(state);
//                                }
//                                int id = palette.indexOf(state);
//                                blocks.add(new StoredBlock(
//                                        targetPos.getX(),
//                                        targetPos.getY(),
//                                        targetPos.getZ(),
//                                        id,
//                                        null
//                                ));
//                                level.destroyBlock(targetPos, true);
//                            }
//                        }
                    }
                }
            }
        }
        for (StoredBlock block : blocks) {
            BlockPos targetPos = new BlockPos(block.x(), block.y(), block.z());
            if (!blacklist.contains(level.getBlockState(targetPos).getBlock())) {
                level.destroyBlock(targetPos, true);
            }
        }
        ExplosionManager.createExplosion(palette, blocks, level.dimension());
    }

    @SubscribeEvent
    public void onBlastEvent(BlastEvent.PostBlastEvent event) {
        if (event.world.isClientSide) return;
        ChunkPos chunkPos = new ChunkPos(event.iExplosion.position);
        String claimOwner = ClaimManager.getClaimOwner(chunkPos.x, chunkPos.z);

        ResourceLocation id = event.iExplosion.getBlastType().id();

        if (id.toString().equals("ballistix:obsidian")) {
            performExplosion(ServerVariable.obsidianBlastBlockDamageRadius, (ServerLevel) event.world, event.iExplosion.position) ;
        } else if (id.toString().equals("ballistix:thermobaric")) {
            //performExplosion(ServerVariable.thermobaricBlastBlockDamageRadius, (ServerLevel) event.world, event.iExplosion.position) ;
        }

        if (claimOwner.equals("server")) {
            return;
        }
        int balance = FactionManager.getBalance(claimOwner);
        int amount = 0;
        if (id.toString().equals("ballistix:obsidian")) {
            amount = (int)(balance * ServerVariable.obsidianBlastMalusPercent);
        } else if (id.toString().equals("ballistix:thermobaric")) {
            amount = (int)(balance * ServerVariable.thermobaricBlastMalusPercent);
        }
        if (balance >= amount && balance > 0) {
            FactionManager.subMoney(claimOwner, amount);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        updateChunkOwner(player);
        if (player.tickCount % 20 == 0) {
           updateScoreBoard(player);
        }
    }

    private void updateScoreBoard(ServerPlayer player) {
        Scoreboard scoreboard = player.getScoreboard();

        String objectiveName = "personal_" + player.getStringUUID().substring(0, 8);
        Objective old = scoreboard.getObjective(objectiveName);

        if (old  != null) {
            scoreboard.removeObjective(old);
        }
        /*
        Objective objective = scoreboard.addObjective(
                objectiveName,
                ObjectiveCriteria.DUMMY,
                Component.literal("§f§l§nCapitalisons"),
                ObjectiveCriteria.RenderType.INTEGER,
                false,
                null
        );
        scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, objective);

        int money = EconomyManager.getPlayerBalance(player.getStringUUID());
        String factionName = PlayerDataBase.getPlayerFaction(player.getStringUUID());

        setLine(scoreboard, objective, "                             ", 11);
        setLine(scoreboard, objective, "§f§l⋙ Argent", 10);
        setLine(scoreboard, objective, "    §e" + String.format(Locale.FRANCE, "%,d", money) + " €", 9);
        setLine(scoreboard, objective, " ", 8);
        if (PlayerDataBase.playerHasFaction(player.getStringUUID())) {
            int factionPower = FactionManager.getPower(factionName);
            int factionBalance = FactionManager.getBalance(factionName);
            setLine(scoreboard, objective, "§f§l⋙ Nation", 7);
            setLine(scoreboard, objective, "    §e" + factionName, 6);
            setLine(scoreboard, objective, "  ", 5);
            setLine(scoreboard, objective, "§f§l⋙ Infos nation", 4);
            setLine(scoreboard, objective, "    §e" + String.format(Locale.FRANCE, "%,d", factionPower) + " puissances", 3);
            setLine(scoreboard, objective, "    §e" + String.format(Locale.FRANCE, "%,d", factionBalance) + " €", 2);
            setLine(scoreboard, objective, "   ", 1);
        }*/
    }


    // Outil pour ajouter une ligne
    private void setLine(Scoreboard scoreboard, Objective objective, String text, int line) {
        ScoreHolder holder = ScoreHolder.forNameOnly(text);
        ScoreAccess access = scoreboard.getOrCreatePlayerScore(holder, objective);
        access.set(line);
    }

    private void updateChunkOwner(ServerPlayer player) {
        ChunkPos currentChunk = player.chunkPosition();

        ItemStack item = player.getMainHandItem();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item.getItem());
        if (!player.serverLevel().dimension().equals(Level.OVERWORLD))
            return;
        if (id.equals(ResourceLocation.parse("ballistix:radargun"))) {
            return;
        }

        if (ClaimManager.checkIfChunkIsClaimed(currentChunk.x, currentChunk.z)) {
            String playerFactionName = PlayerDataBase.getPlayerFaction(player.getStringUUID());
            String chunkFactionName = ClaimManager.getClaimOwner(currentChunk.x, currentChunk.z);
            int powerFaction = FactionManager.getPower(playerFactionName);
            int targetFactionPower = FactionManager.getPower(chunkFactionName);
            int newClaimPrice = claimPrice * (FactionManager.getPower(playerFactionName) / targetFactionPower);
            if (playerFactionName.equals(chunkFactionName)) {
                player.displayClientMessage(Component.literal("§e" + chunkFactionName), true);
            }
            else {
                if (targetFactionPower > powerFaction) {
                    player.displayClientMessage(Component.literal("§cClaim impossible"), true);
                }
                else
                    player.displayClientMessage(Component.literal("§e" + chunkFactionName +  " prix de surclaim : " + NumberFormat.getInstance(Locale.FRENCH).format(newClaimPrice) + "€"), true);
            }
        } else {
            String playerFactionName = PlayerDataBase.getPlayerFaction(player.getStringUUID());
            if (playerFactionName.equals("server"))
                player.displayClientMessage(Component.literal("§flibre"), true);
            else
                player.displayClientMessage(Component.literal("§flibre : " + NumberFormat.getInstance(Locale.FRENCH).format(FactionManager.getClaimPrice(playerFactionName)) + "€"), true);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("nation")
                .requires(source -> source.hasPermission(0))
                .then(Commands.literal("generate")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("count",  IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    FactionManager.adminGenerateNation(5);
                                    ClassementManager.updateNationList();
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(ctx -> {
                                    ServerPlayer sourcePlayer = ctx.getSource().getPlayerOrException();
                                    if (!PlayerDataBase.playerHasFaction(sourcePlayer.getStringUUID())) {
                                        String newFactionName = StringArgumentType.getString(ctx, "name");
                                        if (!FactionManager.factionExist(newFactionName) && !newFactionName.equalsIgnoreCase("server")) {
                                            if (newFactionName.length() > 30) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§cNom de nation trop long, 30 caractères max")
                                                );
                                                return 0;
                                            }
                                            if (newFactionName.isEmpty()) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§cNom de faction invalide")
                                                );
                                                return 0;
                                            }
                                            int color = FactionManager.getUniqueColor();
                                            FactionData factionData = new FactionData(newFactionName, sourcePlayer.getStringUUID(), color);
                                            FactionManager.createNewFaction(factionData);
                                            PlayerDataBase.setPlayerFaction(sourcePlayer.getStringUUID(), factionData.name);
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal(Message.SUCCESS_NATION_CREATE + newFactionName),true
                                            );
                                        } else {
                                            ctx.getSource().sendFailure(
                                                    Component.literal(Message.ERROR_FACTION_ALREADY_EXIST)
                                            );
                                        }
                                    } else {
                                        ctx.getSource().sendFailure(
                                                Component.literal(Message.ERROR_PLAYER_ALREADY_HAS_FACTION )
                                        );
                                        return 0;
                                    }
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("delete")
                        .then(Commands.argument("name_of_your_nation", StringArgumentType.string())
                                .executes(ctx -> {
                                    ServerPlayer sourcePlayer = ctx.getSource().getPlayerOrException();
                                    String factionName = StringArgumentType.getString(ctx, "name_of_your_nation");
                                    String currentPlayerFaction = PlayerDataBase.getPlayerFaction(sourcePlayer.getStringUUID());

                                    if (FactionManager.playerIsOwner(sourcePlayer.getStringUUID(), currentPlayerFaction)) {
                                        if (!factionName.equals(currentPlayerFaction)) {
                                            ctx.getSource().sendFailure(
                                                    Component.literal("§cLe nom de la faction ne correspond pas !")
                                            );
                                            return 0;
                                        }
                                        FactionManager.deleteFaction(factionName);
                                        ServerClaimDataPayload payload = new ServerClaimDataPayload(ClaimManager.getClaimList());
                                        PacketDistributor.sendToAllPlayers(payload);
                                        ctx.getSource().sendSuccess(
                                                () -> Component.literal(Message.SUCCESS_NATION_DELETE),true
                                        );
                                        return 1;
                                    } else {
                                        ctx.getSource().sendFailure(
                                                Component.literal(Message.ERROR_FACTION_PERMISSION)
                                        );
                                        return 0;
                                    }
                                })
                        )
                )
                .then(Commands.literal("saverank")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            ClassementManager.updateNationList();
                            return 1;
                        })
                )
                .then(Commands.literal("leave")
                        .executes(ctx -> {
                            ServerPlayer sourcePlayer = ctx.getSource().getPlayerOrException();
                            String sourcePlayerUuid = sourcePlayer.getStringUUID();
                            String currentPlayerFaction = PlayerDataBase.getPlayerFaction(sourcePlayerUuid);
                            if (!FactionManager.playerIsOwner(sourcePlayerUuid, currentPlayerFaction)) {
                                if (currentPlayerFaction.equalsIgnoreCase("server")) {
                                    ctx.getSource().sendFailure(
                                            Component.literal(Message.ERROR_PLAYER_DONT_HAVE_FACTION)
                                    );
                                } else {
                                    FactionManager.removePlayer(sourcePlayerUuid, currentPlayerFaction);
                                    PlayerDataBase.removePlayerFaction(sourcePlayerUuid);
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("§eVous venez de quitter " + currentPlayerFaction), true
                                    );
                                }
                            } else {
                                ctx.getSource().sendFailure(
                                        Component.literal(Message.ERROR_YOU_CANT_QUIT_YOUR_FACTION )
                                );
                            }
                            return 1;
                        })
                )
                .then(Commands.literal("setowner")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer sourcePlayer = ctx.getSource().getPlayerOrException();
                                    ServerPlayer targetPlayer =  EntityArgument.getPlayer(ctx, "player");
                                    String currentPlayerFaction = PlayerDataBase.getPlayerFaction(sourcePlayer.getStringUUID());
                                    if (FactionManager.playerIsOwner(sourcePlayer.getStringUUID(), currentPlayerFaction)) {
                                        if (sourcePlayer.getStringUUID().equalsIgnoreCase(targetPlayer.getStringUUID())) {
                                            ctx.getSource().sendFailure(
                                                    Component.literal("§cVous ne pouvez pas vous céder la propriété de votre propre nation")
                                            );
                                            return 0;
                                        }
                                        if (FactionManager.playerIsInFaction(targetPlayer.getStringUUID(), currentPlayerFaction)) {
                                            FactionManager.setFactionOwner(targetPlayer.getStringUUID(), currentPlayerFaction);
                                            PlayerDataBase.setPlayerFaction(targetPlayer.getStringUUID(), currentPlayerFaction);
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("§eVous avez transféré la propriété de votre nation"), true
                                            );
                                        } else {
                                            ctx.getSource().sendFailure(
                                                    Component.literal("§cVous ne pouvez pas céder la propriété de votre nation à un joueur externe")
                                            );
                                            return 0;
                                        }
                                    } else {
                                        ctx.getSource().sendFailure(
                                                Component.literal(Message.ERROR_PLAYER_DONT_HAVE_FACTION)
                                        );
                                        return 0;
                                    }
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer sourcePlayer = ctx.getSource().getPlayerOrException();
                                    ServerPlayer targetPlayer = EntityArgument.getPlayer(ctx, "player");
                                    String currentPlayerFaction = PlayerDataBase.getPlayerFaction(sourcePlayer.getStringUUID());
                                    if (!FactionManager.playerIsOpInFaction(sourcePlayer.getStringUUID(), currentPlayerFaction)) {
                                        ctx.getSource().sendFailure(
                                                Component.literal(Message.ERROR_PLAYER_DONT_HAVE_FACTION)
                                        );
                                        return 0;
                                    }
                                    if (PlayerDataBase.getPlayerFaction(targetPlayer.getStringUUID()).equals("server")) {
//                                        PlayerDataBase.setPlayerFaction(targetPlayer.getStringUUID(), currentPlayerFaction);
//                                        FactionManager.addPlayerToFaction(targetPlayer.getStringUUID(), currentPlayerFaction);
                                        if (FactionManager.playerIsInvited(targetPlayer.getStringUUID(), currentPlayerFaction)) {
                                            ctx.getSource().sendFailure(
                                                    Component.literal("§cVous avez déjà invité ce joueur")
                                            );
                                            return 0;
                                        }
                                        FactionManager.invitePlayerToFaction(targetPlayer.getStringUUID(), currentPlayerFaction);
                                        targetPlayer.sendSystemMessage(
                                                Component.literal("&eVous avez été invité à rejoindre la nation " + currentPlayerFaction)
                                        );

                                        targetPlayer.sendSystemMessage(
                                                Component.literal("§7Cliquez ici pour accepter : ")
                                                        .append(
                                                                Component.literal("§a[Rejoindre la nation]")
                                                                        .withStyle(style -> style
                                                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nation join " + currentPlayerFaction))
                                                                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                                                        Component.literal("§7Exécuter /nation join " + currentPlayerFaction)))
                                                                        )
                                                        )
                                        );
                                        ctx.getSource().sendSuccess(
                                                () -> Component.literal("§eVous avez invité " + targetPlayer.getName().getString() + " à rejoindre votre nation"), true
                                        );
                                    } else {
                                        ctx.getSource().sendFailure(
                                                Component.literal("§cLe joueur est déjà dans une nation")
                                        );
                                        return 0;
                                    }
                                    return 1;
                                })
                        )
                )
                        .then(Commands.literal("join")
                                .then(Commands.argument("nation", StringArgumentType.string())
                                        .executes(ctx -> {
                                            ServerPlayer sourcePlayer = ctx.getSource().getPlayerOrException();
                                            String currentPlayerFaction = PlayerDataBase.getPlayerFaction(sourcePlayer.getStringUUID());
                                            String targetFaction = StringArgumentType.getString(ctx, "nation");
                                            if (!currentPlayerFaction.equals("server")) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§cvous appartenez déjà à une nation")
                                                );
                                                return 0;
                                            }
                                            if (FactionManager.playerIsInvited(sourcePlayer.getStringUUID(), targetFaction)) {
                                                FactionManager.deleteInvitation(sourcePlayer.getStringUUID(), targetFaction);
                                                PlayerDataBase.setPlayerFaction(sourcePlayer.getStringUUID(), targetFaction);
                                                FactionManager.addPlayerToFaction(sourcePlayer.getStringUUID(), targetFaction);
                                                ctx.getSource().sendSuccess(
                                                        () -> Component.literal("§eVous avez rejoint la nation " + targetFaction), true
                                                );
                                                return 1;
                                            } else {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§cAucune invitation")
                                                );
                                                return 0;
                                            }
                                        })
                                )
                        )
                .then(Commands.literal("removeplayer")
                        .then(Commands.argument("player", StringArgumentType.string())
                                .executes(ctx -> {
                                    ServerPlayer sourcePlayer = ctx.getSource().getPlayerOrException();
                                    String targetPlayerName = StringArgumentType.getString(ctx, "player");
                                    String targetPlayerUuid = PlayerDataBase.getPlayerUuidByName(targetPlayerName);
                                    String currentPlayerFaction = PlayerDataBase.getPlayerFaction(sourcePlayer.getStringUUID());
                                    if (!FactionManager.playerIsOpInFaction(sourcePlayer.getStringUUID(), currentPlayerFaction)) {
                                        ctx.getSource().sendFailure(
                                                Component.literal(Message.ERROR_PLAYER_DONT_HAVE_FACTION)
                                        );
                                        return 0;
                                    }
                                    if (FactionManager.playerIsInFaction(targetPlayerUuid, currentPlayerFaction)) {
                                        FactionManager.removePlayer(targetPlayerUuid, currentPlayerFaction);
                                        PlayerDataBase.removePlayerFaction(targetPlayerUuid);
                                        ctx.getSource().sendSuccess(
                                                () -> Component.literal("§eLe joueur " + targetPlayerName + " viens de quitter la nation"), true
                                        );
                                        return 1;
                                    } else {
                                        ctx.getSource().sendFailure(
                                                Component.literal("§eLe joueur ne fait pas parti de votre nation")
                                        );
                                        return 0;
                                    }
                                })
                        )
                )
                .then(Commands.literal("info")
                        .executes(ctx -> {
                            ServerPlayer sourcePlayer = ctx.getSource().getPlayerOrException();
                            String currentPlayerFaction = PlayerDataBase.getPlayerFaction(sourcePlayer.getStringUUID());
                            if (PlayerDataBase.playerHasFaction(sourcePlayer.getStringUUID())) {
                                ctx.getSource().sendSuccess(
                                        () -> Component.literal(Objects.requireNonNull(FactionManager.getFactionInfos(currentPlayerFaction))), false
                                );
                                return 1;
                            } else {
                                ctx.getSource().sendFailure(
                                        Component.literal(Message.ERROR_PLAYER_DONT_HAVE_FACTION)
                                );
                                return 0;
                            }
                        })
                        .then(Commands.argument("nation", StringArgumentType.string())
                                .executes(ctx -> {
                                    String targetNation = StringArgumentType.getString(ctx, "nation");
                                    if (FactionManager.factionExist(targetNation)) {
                                        ctx.getSource().sendSuccess(
                                                () -> Component.literal(Objects.requireNonNull(FactionManager.getFactionInfos(targetNation))), false
                                        );
                                        return 1;
                                    } else {
                                        ctx.getSource().sendFailure(
                                                Component.literal("§eNation introuvable")
                                        );
                                        return 0;
                                    }
                                })
                        )
                )
                .then(Commands.literal("description")
                        .then(Commands.argument("text", StringArgumentType.string())
                                .executes(ctx -> {
                                    String description =  StringArgumentType.getString(ctx, "text");
                                    ServerPlayer sourcePlayer = ctx.getSource().getPlayerOrException();
                                    if (PlayerDataBase.playerHasFaction(sourcePlayer.getStringUUID())) {
                                        String factionName = PlayerDataBase.getPlayerFaction(sourcePlayer.getStringUUID());
                                        if (FactionManager.playerIsOpInFaction(sourcePlayer.getStringUUID(), factionName)) {
                                            if (description.length() > 140) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§eDescription trop longue, max 140 charactères")
                                                );
                                                return 0;
                                            } else {
                                                FactionManager.setFactionDesription(description, factionName);
                                                ctx.getSource().sendSuccess(
                                                        () -> Component.literal("§eDescription mise à jour"), false
                                                );
                                                return 1;
                                            }
                                        } else {
                                            ctx.getSource().sendFailure(
                                                    Component.literal(Message.ERROR_FACTION_PERMISSION )
                                            );
                                            return 0;
                                        }
                                    } else {
                                        ctx.getSource().sendFailure(
                                                Component.literal(Message.ERROR_PLAYER_DONT_HAVE_FACTION)
                                        );
                                        return 0;
                                    }
                                })
                        )
                )
                .then(Commands.literal("setflag")
                        .executes(ctx -> {
                            ServerPlayer sourcePlayer = ctx.getSource().getPlayerOrException();
                            if (PlayerDataBase.playerHasFaction(sourcePlayer.getStringUUID())) {
                                String currentPlayerFaction = PlayerDataBase.getPlayerFaction(sourcePlayer.getStringUUID());
                                if (FactionManager.playerIsOpInFaction(sourcePlayer.getStringUUID(), currentPlayerFaction)) {
                                    ItemStack itemStack = Objects.requireNonNull(ctx.getSource().getPlayer()).getMainHandItem();
                                    if (!(itemStack.getItem() instanceof BannerItem)) {
                                        ctx.getSource().sendFailure(
                                                Component.literal("§cVous devez avoir une bannière en main")
                                        );
                                        return 0;
                                    } else {
                                        ServerLevel level = sourcePlayer.serverLevel();
                                        CompoundTag tag = (CompoundTag) itemStack.save(level.registryAccess(), new CompoundTag());
                                        FactionManager.setBannerTag(tag.toString(), currentPlayerFaction);
                                        ctx.getSource().sendSuccess(
                                                () -> Component.literal("§cDrapeau de nation mis à jour"), false
                                        );
                                        return 1;
                                    }
                                } else {
                                    ctx.getSource().sendFailure(
                                            Component.literal(Message.ERROR_FACTION_PERMISSION )
                                    );
                                    return 0;
                                }
                            } else {
                                ctx.getSource().sendFailure(
                                        Component.literal(Message.ERROR_PLAYER_DONT_HAVE_FACTION)
                                );
                                return 0;
                            }
                        })
                )
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal(FactionManager.getFactionList()), false
                            );
                            return 1;
                        })
                )
                .then(Commands.literal("promote")
                        .then(Commands.argument("player", StringArgumentType.string())
                                .executes(ctx -> {
                                    ServerPlayer sourcePlayer = ctx.getSource().getPlayerOrException();
                                    String targetPlayerName = StringArgumentType.getString(ctx, "player");
                                    String targetPlayerUuid = PlayerDataBase.getPlayerUuidByName(targetPlayerName);
                                    String currentPlayerFaction = PlayerDataBase.getPlayerFaction(sourcePlayer.getStringUUID());
                                    if (FactionManager.playerIsOwner(sourcePlayer.getStringUUID(), currentPlayerFaction)) {
                                        if (!FactionManager.playerIsMember(targetPlayerUuid, currentPlayerFaction)) {
                                            ctx.getSource().sendFailure(
                                                    Component.literal(Message.ERROR_FACTION_PERMISSION)
                                            );
                                            return 0;
                                        }
                                        FactionManager.promotePlayer(targetPlayerUuid, currentPlayerFaction);
                                        ctx.getSource().sendSuccess(
                                                () -> Component.literal("§eLe joueur est désormais officier !"), true
                                        );
                                        return 1;
                                    } else {
                                        ctx.getSource().sendFailure(
                                                Component.literal(Message.ERROR_FACTION_PERMISSION)
                                        );
                                        return 0;
                                    }
                                })
                        )
                )
                .then(Commands.literal("demote")
                        .then(Commands.argument("player", StringArgumentType.string())
                                .executes(ctx -> {
                                    ServerPlayer sourcePlayer = ctx.getSource().getPlayerOrException();
                                    String targetPlayerName = StringArgumentType.getString(ctx, "player");
                                    String targetPlayerUuid = PlayerDataBase.getPlayerUuidByName(targetPlayerName);
                                    String currentPlayerFaction = PlayerDataBase.getPlayerFaction(sourcePlayer.getStringUUID());
                                    if (FactionManager.playerIsOwner(sourcePlayer.getStringUUID(), currentPlayerFaction)) {
                                        if (!FactionManager.playerIsOfficer(targetPlayerUuid, currentPlayerFaction)) {
                                            ctx.getSource().sendFailure(
                                                    Component.literal("§eLe joueur n'est pas officier !")
                                            );
                                            return 0;
                                        }
                                        FactionManager.demotePlayer(targetPlayerUuid, currentPlayerFaction);
                                        ctx.getSource().sendSuccess(
                                                () -> Component.literal("§eLe joueur est désormais membre"), true
                                        );
                                        return 1;
                                    } else {
                                        ctx.getSource().sendFailure(
                                                Component.literal(Message.ERROR_FACTION_PERMISSION)
                                        );
                                        return 0;
                                    }
                                })
                        )
                )
                .then(Commands.literal("deposit")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    ServerPlayer sourcePlayer = ctx.getSource().getPlayerOrException();
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    String currentPlayerFaction = PlayerDataBase.getPlayerFaction(sourcePlayer.getStringUUID());
                                    if (!PlayerDataBase.playerHasFaction(sourcePlayer.getStringUUID())) {
                                        ctx.getSource().sendFailure(
                                                Component.literal(Message.ERROR_PLAYER_DONT_HAVE_FACTION)
                                        );
                                        return 0;
                                    }
                                    if (EconomyManager.getPlayerBalance(sourcePlayer.getStringUUID()) >= amount) {
                                        FactionManager.addMoney(currentPlayerFaction, amount);
                                        EconomyManager.subMoney(sourcePlayer.getStringUUID(), amount);
                                        ctx.getSource().sendSuccess(
                                                () -> Component.literal("§eVous venez de déposer " + amount + " euros sur le compte de votre nation"), true
                                        );
                                        return 1;
                                    } else {
                                        ctx.getSource().sendFailure(
                                                Component.literal("§cVous n'avez pas assez d'argent")
                                        );
                                        return 0;
                                    }
                                })
                        )
                )
//                .then(Commands.literal("withdraw")
//                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
//                                .executes(ctx -> {
//                                    ServerPlayer sourcePlayer = ctx.getSource().getPlayerOrException();
//                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
//                                    String currentPlayerFaction = PlayerDataBase.getPlayerFaction(sourcePlayer.getStringUUID());
//                                    if (!PlayerDataBase.playerHasFaction(sourcePlayer.getStringUUID())) {
//                                        ctx.getSource().sendFailure(
//                                                Component.literal(Message.ERROR_PLAYER_DONT_HAVE_FACTION)
//                                        );
//                                        return 0;
//                                    }
//                                    if (FactionManager.playerIsOwner(sourcePlayer.getStringUUID(), currentPlayerFaction) ||
//                                            FactionManager.playerIsOfficer(sourcePlayer.getStringUUID(), currentPlayerFaction)) {
//                                        if (FactionManager.getBalance(currentPlayerFaction) >= amount) {
//                                            EconomyManager.addMoney(sourcePlayer.getStringUUID(), amount);
//                                            FactionManager.subMoney(currentPlayerFaction, amount);
//                                            ctx.getSource().sendSuccess(
//                                                    () -> Component.literal("§eVous venez de retirer " + amount + " euros sur le compte de votre nation"), true
//                                            );
//                                            return 1;
//                                        } else {
//                                            ctx.getSource().sendFailure(
//                                                    Component.literal("§cVotre nation n’a pas suffisamment d’argent")
//                                            );
//                                            return 0;
//                                        }
//                                    } else {
//                                        ctx.getSource().sendFailure(
//                                                Component.literal("§cVous ne pouvez pas retirer d’argent du compte bancaire de votre nation")
//                                        );
//                                        return 0;
//                                    }
//                                })
//                        )
//                )
                .then(Commands.literal("balance")
                        .executes(ctx -> {
                            ServerPlayer sourcePlayer = ctx.getSource().getPlayerOrException();
                            String currentPlayerFaction = PlayerDataBase.getPlayerFaction(sourcePlayer.getStringUUID());
                            if (!PlayerDataBase.playerHasFaction(sourcePlayer.getStringUUID())) {
                                ctx.getSource().sendFailure(
                                        Component.literal(Message.ERROR_PLAYER_DONT_HAVE_FACTION)
                                );
                                return 0;
                            }
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("§eSolde de votre faction : " + FactionManager.getBalance(currentPlayerFaction) + " euros"), false
                            );
                            return 1;
                        })
                )
                .then(Commands.literal("unclaim")
                        .executes(ctx -> {
                            ServerPlayer sourcePlayer = ctx.getSource().getPlayerOrException();
                            String currentPlayerFaction = PlayerDataBase.getPlayerFaction(sourcePlayer.getStringUUID());
                            int chunkX = sourcePlayer.chunkPosition().x;
                            int chunkZ = sourcePlayer.chunkPosition().z;
                            if (PlayerDataBase.playerHasFaction(sourcePlayer.getStringUUID())) {
                                if (FactionManager.playerIsOwner(sourcePlayer.getStringUUID(), currentPlayerFaction) ||
                                        FactionManager.playerIsOfficer(sourcePlayer.getStringUUID(), currentPlayerFaction)) {
                                    if (ClaimManager.checkIfChunkIsClaimed(chunkX, chunkZ) && ClaimManager.getClaimOwner(chunkX, chunkZ).equals(currentPlayerFaction)) {
                                        ClaimManager.deleteClaim(chunkX, chunkZ);
                                        FactionManager.decrementTotalChunk(currentPlayerFaction);
                                        ctx.getSource().sendSuccess(
                                                () -> Component.literal("§eVous n’êtes plus propriétaire de ce territoire !"), true
                                        );
                                        return 1;
                                    } else {
                                        ctx.getSource().sendFailure(
                                                Component.literal("§cCe territoire n’est pas sous votre contrôle !")
                                        );
                                        return 0;
                                    }
                                } else {
                                    ctx.getSource().sendFailure(
                                            Component.literal(Message.ERROR_FACTION_PERMISSION )
                                    );
                                    return 0;
                                }
                            } else {
                                ctx.getSource().sendFailure(
                                        Component.literal(Message.ERROR_PLAYER_DONT_HAVE_FACTION)
                                );
                                return 0;
                            }
                        })
                )
                .then(Commands.literal("claim")
                        .executes(ctx -> {
                            ServerPlayer sourcePlayer = ctx.getSource().getPlayerOrException();
                            String currentPlayerFaction = PlayerDataBase.getPlayerFaction(sourcePlayer.getStringUUID());
                            int chunkX = sourcePlayer.chunkPosition().x;
                            int chunkZ = sourcePlayer.chunkPosition().z;
                            if (!sourcePlayer.serverLevel().dimension().equals(Level.OVERWORLD)) {
                                ctx.getSource().sendFailure(
                                        Component.literal("§cVous ne pouvez pas claim ici")
                                );
                                return 0;
                            }
                            if (PlayerDataBase.playerHasFaction(sourcePlayer.getStringUUID())) {
                                if (FactionManager.playerIsOwner(sourcePlayer.getStringUUID(), currentPlayerFaction) ||
                                        FactionManager.playerIsOfficer(sourcePlayer.getStringUUID(), currentPlayerFaction)) {
                                    int factionBalance = FactionManager.getBalance(currentPlayerFaction);
                                    int factionClaimPrice = FactionManager.getClaimPrice(currentPlayerFaction);
                                    if (!ClaimManager.checkIfChunkIsClaimed(chunkX, chunkZ)) {
                                        if (FactionManager.getTotalChunks(currentPlayerFaction) > 0 && !ClaimManager.claimHaveNeighbors(chunkX, chunkZ, currentPlayerFaction)) {
                                            ctx.getSource().sendFailure(
                                                    Component.literal("§cVous ne pouvez pas réclamer un terrain non limitrophe")
                                            );
                                            return 0;
                                        }
                                        if (factionBalance >= factionClaimPrice) {
                                            //Claim
                                            FactionManager.subMoney(currentPlayerFaction, factionClaimPrice);
                                            ServerDataManager.addBankBalance(factionClaimPrice);
                                            ClaimData newClaimData = new ClaimData(currentPlayerFaction, chunkX, chunkZ, FactionManager.getFactionColor(currentPlayerFaction));
                                            ClaimManager.createClaim(newClaimData);
                                            FactionManager.incrementTotalChunk(currentPlayerFaction);
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("§eVous venez de réclamer le territoire pour " + factionClaimPrice + " euros"), true
                                            );
                                            return 1;
                                        } else {
                                            ctx.getSource().sendFailure(
                                                    Component.literal("§cVotre nation n'a pas assez d'argent pour réclamer un territoire!")
                                            );
                                            return 0;
                                        }
                                    } else {
                                        if (!ClaimManager.getClaimOwner(chunkX, chunkZ).equals(currentPlayerFaction)) {
                                            String defensingFactionName = ClaimManager.getClaimOwner(chunkX, chunkZ);
                                            int attackingFaction = FactionManager.getPower(currentPlayerFaction);
                                            int defensingFaction = FactionManager.getPower(defensingFactionName);
                                            if (!ClaimManager.claimIsBorder(chunkX, chunkZ, defensingFactionName)) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§cAction refusée : ce chunk n’est pas en bordure du territoire ennemi.")
                                                );
                                                return 0;
                                            }
                                            if (attackingFaction > defensingFaction) {
                                                int newClaimPrice = claimPrice * (attackingFaction / defensingFaction);
                                                if (factionBalance >= newClaimPrice) {
                                                    ClaimManager.changeClaimOwner(chunkX, chunkZ, currentPlayerFaction, FactionManager.getFactionColor(currentPlayerFaction));
                                                    FactionManager.subMoney(currentPlayerFaction, newClaimPrice);
                                                    FactionManager.incrementTotalChunk(currentPlayerFaction);
                                                    FactionManager.decrementTotalChunk(defensingFactionName);
                                                    ctx.getSource().sendSuccess(
                                                            () -> Component.literal("§eVous venez de voler le territoire de " + defensingFactionName + " pour " + newClaimPrice + " euros"), true
                                                    );
                                                    return 1;
                                                } else {
                                                    ctx.getSource().sendFailure(
                                                            Component.literal("§cVotre nation à besoin de " + newClaimPrice + "€, pour voler le territoire de " + defensingFactionName)
                                                    );
                                                    return 0;
                                                }
                                            }
                                            else {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§cVous ne pouvez pas voler le territoire d'une nation plus puissante que la votre ! /nation info " + defensingFactionName)
                                                );
                                                return 0;
                                            }
                                        }
                                        ctx.getSource().sendFailure(
                                                Component.literal("§cVous ne pouvez pas réclamer un territoire qui vous appartient déjà !")
                                        );
                                        return 0;
                                    }
                                } else {
                                    ctx.getSource().sendFailure(
                                            Component.literal(Message.ERROR_FACTION_PERMISSION)
                                    );
                                    return 0;
                                }
                            } else {
                                ctx.getSource().sendFailure(
                                        Component.literal(Message.ERROR_PLAYER_DONT_HAVE_FACTION)
                                );
                                return 0;
                            }
                        })
                )
        );
    }
}
