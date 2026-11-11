package com.qidate.qisplan2.ghost.module.host;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

/** 地毯袭击的粒子与声音表现；蔓延由通用方块蔓延接口负责。 */
public final class GhostCarpetWorldEffects {
    /** 世界效果只通过静态入口执行。 */
    private GhostCarpetWorldEffects() {}

    /** 在袭击目标处播放原有灵魂粒子与音效，仅供已获准的能力调用。 */
    public static void feedback(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level)) return;
        level.sendParticles(ParticleTypes.SOUL, target.getX(), target.getY() + 0.5, target.getZ(),
                20, 0.4, 0.5, 0.4, 0.05);
        level.playSound(null, target.blockPosition(), SoundEvents.SOUL_ESCAPE.value(),
                SoundSource.BLOCKS, 1, 0.8F);
    }

}
