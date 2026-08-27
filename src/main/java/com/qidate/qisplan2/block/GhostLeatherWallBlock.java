package com.qidate.qisplan2.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class GhostLeatherWallBlock
        extends Block {

    public static final MapCodec<GhostLeatherWallBlock> CODEC =
            simpleCodec(
                    GhostLeatherWallBlock::new
            );

    public GhostLeatherWallBlock(
            BlockBehaviour.Properties properties
    ) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }
}