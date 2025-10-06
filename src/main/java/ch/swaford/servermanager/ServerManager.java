package ch.swaford.servermanager;

import ch.swaford.servermanager.classement.*;
import ch.swaford.servermanager.clientinterface.ClientCommands;
import ch.swaford.servermanager.clientinterface.ClientKeyManager;
import ch.swaford.servermanager.clientinterface.ClientSetup;
import ch.swaford.servermanager.clientinterface.JourneyMapPlugin;
import ch.swaford.servermanager.networktransfer.*;
import ch.swaford.servermanager.shop.BuyShopInterface;
import ch.swaford.servermanager.shop.SellShopInterface;
import ch.swaford.servermanager.shop.ShopCommands;
import ch.swaford.servermanager.shop.ShopManager;
import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.api.v2.client.model.MapPolygon;
import journeymap.api.v2.client.model.ShapeProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/*
 * Classe principale du mod ServerManager
 *
 * Rôles principaux :
 * - Enregistre les événements communs et spécifiques au client.
 * - Initialise et enregistre les payloads réseau personnalisés (communication client ↔ serveur).
 * - Crée et vérifie l’existence des fichiers JSON nécessaires au démarrage du serveur
 *   (players, bank_accounts, factions, claims, server_data).
 * - Met à jour les différents gestionnaires (FactionManager, ClaimManager, ServerDataManager,
 *   PlayerDataBase, BankManager) au lancement.
 * - Gère les événements liés aux joueurs :
 *      -> Lors de la connexion d’un joueur, ajoute ses données si elles n’existent pas déjà.
 *      -> Met à jour les statistiques de connexion quotidienne dans ServerDataManager.
 *
 * Communication réseau :
 * - RequestPlayerData (client → serveur) → réponse avec ServerCientDataPayload.
 * - RequestFactionData (client → serveur) → réponse avec ServerFactionDataPayload.
 */


@Mod(ServerManager.MODID)
public class ServerManager {
    public static final String MODID = "servermanager";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ServerManager(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modEventBus.addListener(ServerManager::registerPayloads);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new EconomyCommands());
        NeoForge.EVENT_BUS.register(new BankCommands());
        NeoForge.EVENT_BUS.register(new FactionCommands());
        NeoForge.EVENT_BUS.register(new TimeManager());
        NeoForge.EVENT_BUS.register(new ShopCommands());
        NeoForge.EVENT_BUS.register(new ClassementEvent());
        NeoForge.EVENT_BUS.register(new ClassementCommand());
        NeoForge.EVENT_BUS.register(new ClaimInteractionProtection());

        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.register(ClientCommands.class);
            NeoForge.EVENT_BUS.register(ClientKeyManager.class);
            modEventBus.addListener(ClientSetup::onClientSetup);
            modEventBus.addListener(ClientSetup::registerKeys);
        }
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("servermanager");

        registrar.playToServer(
                RequestFactionData.ID,
                RequestFactionData.CODEC,
                (payload, context) -> {
                    String factionName = payload.factionName();
                    ServerPlayer player = (ServerPlayer) context.player();
                    if (factionName.equals("server")) {
                        if (PlayerDataBase.playerHasFaction(player.getStringUUID())) {
                            factionName = PlayerDataBase.getPlayerFaction(player.getStringUUID());
                        } else {
                            factionName = null;
                        }
                    } else {
                        if (!FactionManager.factionExist(factionName)) {
                            return;
                        }
                    }
                    System.out.println("Request faction data by " + player.getStringUUID());
                    player.connection.send(new ServerFactionDataPayload(
                            new ServerFactionData(
                                    factionName == null ? "server" : factionName,
                                    factionName == null ? "error" : FactionManager.getFactionOwner(factionName),
                                    factionName == null ? 0 : FactionManager.getBalance(factionName),
                                    factionName == null ? 0 : FactionManager.getBonusPower(factionName),
                                    factionName == null ? 0 :  FactionManager.getPower(factionName),
                                    factionName == null ? 0 : FactionManager.getTotalChunks(factionName),
                                    factionName == null ? List.of() : FactionManager.getFactionOfficers(factionName),
                                    factionName == null ? List.of() : FactionManager.getFactionMembers(factionName),
                                    factionName == null ? 0 : FactionManager.getFactionRank(factionName),
                                    factionName == null ? "error" : FactionManager.getFactionDescription(factionName),
                                    factionName == null ? "" : FactionManager.getBannerTag(factionName),
                                    factionName == null ? new HashMap<>() : FactionManager.getPlayerList(factionName)
                            ),
                            payload.screen(),
                            new ServerClientData(
                                    player.getStringUUID(),
                                    PlayerDataBase.getPlayerNameByUuid(player.getStringUUID()),
                                    PlayerDataBase.getPlayerFaction(player.getStringUUID()),
                                    EconomyManager.getPlayerBalance(player.getStringUUID())
                            )
                    ));
                }
        );

        registrar.playToServer(
                RequestClassementData.ID,
                RequestClassementData.CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        ServerPlayer player = (ServerPlayer) context.player();
                        ServerClassementPayload serverClassementPayload = new ServerClassementPayload(ClassementManager.getClassementDataList());
                        PacketDistributor.sendToPlayer(player, serverClassementPayload);
                    });
                }
        );

        registrar.playToServer(
                RequestPlayerData.ID,
                RequestPlayerData.CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        ServerPlayer player = (ServerPlayer) context.player();
                        ServerCientDataPayload serverCientDataPayload = new ServerCientDataPayload(PlayerDataBase.getServerClientData(player.getStringUUID()), payload.screen());
                        PacketDistributor.sendToPlayer(player, serverCientDataPayload);
                    });
                }
        );

        registrar.playToClient(
                ServerClaimDataPayload.ID,
                ServerClaimDataPayload.CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        ClientCache.setClaimDataList(payload.claims());
                        List<ClaimData> claims = ClientCache.getClaimDataList();
                        var api = JourneyMapPlugin.getJmApi();
                        var mc = Minecraft.getInstance();
                        if (api == null || mc.level == null) {
                            assert mc.player != null;
                            mc.player.sendSystemMessage(
                                    Component.literal("erreur api")
                            );
                            return;
                        }
                        api.removeAll("servermanager");
                        for (ClaimData claim : claims) {
                            int blockX = claim.x * 16;
                            int blockZ = claim.z * 16;
                            ShapeProperties props = new ShapeProperties()
                                    .setFillColor(claim.color)
                                    .setStrokeColor(0x00000000)
                                    .setStrokeWidth(0);

                            MapPolygon poly = new MapPolygon(
                                    new BlockPos(blockX, 64, blockZ),
                                    new BlockPos(blockX + 16, 64, blockZ),
                                    new BlockPos(blockX + 16, 64, blockZ + 16),
                                    new BlockPos(blockX, 64, blockZ + 16)
                            );

                            PolygonOverlay overlay = new PolygonOverlay(
                                    "servermanager",
                                    Level.OVERWORLD,
                                    props,
                                    poly
                            );

                            try {
                                api.show(overlay);
                            } catch (Exception e) {
                                mc.player.sendSystemMessage(
                                        Component.literal("§cErreur: " + e.getMessage())
                                );
                            }
                        }

                    });
                }
        );

        registrar.playToClient(
                ServerShopPayload.ID,
                ServerShopPayload.CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        ClientCache.setServerShopItemDataList(payload.serverShopItemData());
                        if (Minecraft.getInstance().screen instanceof BuyShopInterface buyShopInterface) {
                            buyShopInterface.updatePrice();
                        }
                        if (Minecraft.getInstance().screen instanceof SellShopInterface sellShopInterface) {
                            sellShopInterface.updatePrice();
                        }
                    });
                }
        );

        registrar.playToClient(
                ServerClassementPayload.ID,
                ServerClassementPayload.CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        ClientNetworkHandler.handleServerClassementData(payload.data());
                    });
                }
        );


        registrar.playToClient(
                ServerCientDataPayload.ID,
                ServerCientDataPayload.CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        ClientNetworkHandler.handleServerClientData(payload.data(), payload.screen());
                    });
                }
        );

        registrar.playToClient(
                ServerFactionDataPayload.ID,
                ServerFactionDataPayload.CODEC,
                (payload, context) -> {
                    ClientNetworkHandler.handleServerFactionData(payload.data(), payload.screen(), payload.serverClientData());
                }
        );
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        File players = new File("players.json");
        File bankAccounts = new File("bank_accounts.json");
        File factions = new File("factions.json");
        File claims =  new File("claims.json");
        File serverData = new File("server_data.json");
        File shop = new File("shop.json");
        File classement = new File("classement.json");
        try {
            players.createNewFile();
            bankAccounts.createNewFile();
            factions.createNewFile();
            claims.createNewFile();
            serverData.createNewFile();
            shop.createNewFile();
            classement.createNewFile();
        }catch (IOException e) {
            e.printStackTrace();
        }
        FactionManager.updateJsonFile();
        ClaimManager.updateJsonFile();
        ServerDataManager.updateJsonFile();
        PlayerDataBase.updateJsonFile();
        BankManager.updateJsonFile();
        ShopManager.updateJsonFile();
        ClassementManager.updateJsonFile();
        ClassementManager.updateNationList();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        ClassementCache.SaveClassement();
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        String newUuid = player.getStringUUID();

        if (!PlayerDataBase.playerExists(newUuid)) {
            LOGGER.info("New player");
            PlayerData newPlayerData = new PlayerData (
                    player.getStringUUID(),
                    player.getName().getString()
            );
            PlayerDataBase.addPlayer(newPlayerData);
        }
        if (!ServerDataManager.isPlayerConnectedToday(newUuid)) {
            ServerDataManager.addPlayerConnectedToday(newUuid);
        }
        ServerClaimDataPayload payload = new ServerClaimDataPayload(ClaimManager.getClaimList());
        PacketDistributor.sendToPlayer((ServerPlayer) player, payload);

        ServerShopPayload serverShopPayload = new ServerShopPayload(ShopManager.getServerShopItemData());
        PacketDistributor.sendToPlayer((ServerPlayer) player, serverShopPayload);

        ServerCientDataPayload serverCientDataPayload = new ServerCientDataPayload(PlayerDataBase.getServerClientData(newUuid), -1);
        PacketDistributor.sendToPlayer((ServerPlayer) player, serverCientDataPayload);
    }
}
