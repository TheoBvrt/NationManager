package ch.swaford.servermanager.explosion;

import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

public record StoredBlock(
        int x,
        int y,
        int z,
        int paletteId,
        @Nullable CompoundTag nbt
        ) {}
