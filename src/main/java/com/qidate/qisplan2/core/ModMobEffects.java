package com.qidate.qisplan2.core;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;

import static com.qidate.qisplan2.core.ModRegistries.MOB_EFFECTS;

public class ModMobEffects {

    private ModMobEffects() {}

    public static void init() {
        /*
         * 故意留空。
         *
         * 调用这个方法本身，就会强制 JVM
         * 在正确的时机完成 ModMobEffects 的静态初始化。
         */
    }

    // 生签：灵异保护
    public static final DeferredHolder<MobEffect, MobEffect> LIFE_SIGN_PROTECTION =
            MOB_EFFECTS.register(
                    "life_sign_protection",
                    () -> new MobEffect(
                            MobEffectCategory.BENEFICIAL,
                            0xFFFFFF
                    ) {
                    }
            );
}
