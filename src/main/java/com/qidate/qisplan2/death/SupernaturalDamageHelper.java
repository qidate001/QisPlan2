package com.qidate.qisplan2.death;

import com.qidate.qisplan2.core.ModTags;
import net.minecraft.world.damagesource.DamageSource;

public final class SupernaturalDamageHelper {

    private SupernaturalDamageHelper() {
    }

    /**
     * 判断 DamageSource 是否属于灵异伤害。
     *
     * 灵异伤害不会受到驭鬼者的普通伤害减免。
     */
    public static boolean isSupernatural(
            DamageSource source
    ) {
        return source.is(
                ModTags.DamageTypes.SUPERNATURAL
        );
    }
}