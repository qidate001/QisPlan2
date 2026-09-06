package com.qidate.qisplan2.death;

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

        /*
         * ========================================================
         * QisPlan2 灵异伤害
         * ========================================================
         */

        return source.is(ModDamageTypes.GHOST_CARPET)

                || source.is(ModDamageTypes.DEATH_CURSE)

                || source.is(ModDamageTypes.GHOST_STONE_BRICKS)

                || source.is(ModDamageTypes.GHOST_STOVE)

                || source.is(ModDamageTypes.GHOST_GRASS)

                || source.is(ModDamageTypes.GHOST_NIGHT_WANDERER)

                || source.is(ModDamageTypes.INVISIBLE_GHOST)

                || source.is(ModDamageTypes.GHOST_UMBRELLA)

                || source.is(ModDamageTypes.GHOST_PIANO)

                || source.is(ModDamageTypes.KNOCKING_GHOST)

                || source.is(ModDamageTypes.OPENING_GHOST)

                || source.is(ModDamageTypes.CLOSING_GHOST)

                || source.is(ModDamageTypes.GHOST_LAKE_WATER)

                || source.is(ModDamageTypes.GHOST_BLOOD)

                || source.is(ModDamageTypes.GHOST_DOOR_PLATE)

                || source.is(ModDamageTypes.CALLING_GHOST);
    }
}