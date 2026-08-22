package com.qidate.qisplan2.block;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class GhostGrassBlock extends Block {

    /**
     * 必死诅咒冷却时间：5 秒
     * Minecraft 默认 20 tick / 秒
     */
    private static final long CURSE_COOLDOWN = 100L;

    /**
     * 存在 PersistentData 中的冷却时间键。
     */
    private static final String COOLDOWN_KEY =
            "qisplan2_ghost_grass_curse";

    /**
     * 灵异袭击强度
     */
    private static final double SUPERNATURAL_ATTACK_STRENGTH = 0.7D;

    public GhostGrassBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity
    ) {
        if (level.isClientSide()) {
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!(entity instanceof LivingEntity livingEntity)) {
            return;
        }

        long currentTime = serverLevel.getGameTime();

        long lastTime = livingEntity.getPersistentData()
                .getLong(COOLDOWN_KEY);

        if (currentTime - lastTime < CURSE_COOLDOWN) {
            return;
        }

        livingEntity.getPersistentData().putLong(
                COOLDOWN_KEY,
                currentTime
        );

        /*
         * ========================================
         * 必死诅咒
         * ========================================
         */
        SupernaturalDeathHandler.tryKill(
                livingEntity,
                ModDamageTypes.ghostGrass(livingEntity),
                SUPERNATURAL_ATTACK_STRENGTH
        );
    }

    @Override
    public float getDestroyProgress(
            BlockState state,
            Player player,
            BlockGetter level,
            BlockPos pos
    ) {
        if (!player.isCreative()) {
            return 0.0F;
        }

        return super.getDestroyProgress(
                state,
                player,
                level,
                pos
        );
    }
}