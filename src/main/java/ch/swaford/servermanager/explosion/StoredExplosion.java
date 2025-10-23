package ch.swaford.servermanager.explosion;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public record StoredExplosion(
        List<BlockState> palette,
        List<StoredBlock> blockList,
        ResourceKey<Level> dimension
) {}
