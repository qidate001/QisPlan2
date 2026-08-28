package com.qidate.qisplan2.core;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.registries.DeferredHolder;

import static com.qidate.qisplan2.core.ModRegistries.PARTICLE_TYPES;

public class ModParticles {

    private ModParticles() {}

    public static void init() {
        /*
         * 故意留空。
         *
         * 调用这个方法本身，就会强制 JVM
         * 在正确的时机完成 ModParticles 的静态初始化。
         */
    }



    // 鬼黑雨
    public static final DeferredHolder<
            ParticleType<?>,
            SimpleParticleType
            > BLACK_RAIN =
            PARTICLE_TYPES.register(
                    "black_rain",
                    () -> new SimpleParticleType(false)
            );
}
