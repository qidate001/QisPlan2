package com.qidate.qisplan2.ghost;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record PossessedGhostState(

        /**
         * 复苏值：
         *
         * 0.0 ~ 1.0 = 0% ~ 100%
         */
        double revival,

        /**
         * 浅死机值：
         *
         * 1 点 = 抵消 1% 的复苏增长。
         */
        double shallowStun,

        /**
         * 普通死机剩余时间。
         *
         * 单位：tick。
         */
        long stunTicks,

        /**
         * 是否永久死机。
         */
        boolean permanentStun,

        /**
         * 上次使用能力的时间。
         */
        long lastAbilityUseTick,

        /**
         * 厉鬼本质强度。
         *
         * 这是“这只鬼本身有多强”。
         *
         * 它不是当前实际使用强度。
         *
         * 它可以随着玩家成长、特殊事件、
         * 吞噬、融合等机制不断增加。
         */
        double intrinsicStrength
) {

    /**
     * 浅死机值上限。
     */
    public static final double MAX_SHALLOW_STUN =
            100.0D;


    /**
     * 状态构造时统一限制数值范围。
     */
    public PossessedGhostState {

        /*
         * 复苏值限制在 0~1。
         */
        revival =
                Math.clamp(
                        revival,
                        0.0D,
                        1.0D
                );

        /*
         * 浅死机值限制在 0~100。
         */
        shallowStun =
                Math.clamp(
                        shallowStun,
                        0.0D,
                        MAX_SHALLOW_STUN
                );

        /*
         * 死机时间不能为负数。
         */
        stunTicks =
                Math.max(
                        0L,
                        stunTicks
                );

        /*
         * 本质强度不能为负数。
         */
        intrinsicStrength =
                Math.max(
                        0.0D,
                        intrinsicStrength
                );
    }


    /*
     * ============================================================
     * Codec
     * ============================================================
     */

    public static final Codec<PossessedGhostState> CODEC =
            RecordCodecBuilder.create(instance ->
                    instance.group(

                            Codec.DOUBLE
                                    .fieldOf("revival")
                                    .forGetter(
                                            PossessedGhostState::revival
                                    ),

                            Codec.DOUBLE
                                    .fieldOf("shallow_stun")
                                    .forGetter(
                                            PossessedGhostState::shallowStun
                                    ),

                            Codec.LONG
                                    .optionalFieldOf(
                                            "stun_ticks",
                                            0L
                                    )
                                    .forGetter(
                                            PossessedGhostState::stunTicks
                                    ),

                            Codec.BOOL
                                    .optionalFieldOf(
                                            "permanent_stun",
                                            false
                                    )
                                    .forGetter(
                                            PossessedGhostState::permanentStun
                                    ),

                            Codec.LONG
                                    .optionalFieldOf(
                                            "last_ability_use_tick",
                                            Long.MIN_VALUE
                                    )
                                    .forGetter(
                                            PossessedGhostState::lastAbilityUseTick
                                    ),

                            /*
                             * 厉鬼本质强度。
                             *
                             * optional 是为了兼容旧存档。
                             */
                            Codec.DOUBLE
                                    .optionalFieldOf(
                                            "intrinsic_strength",
                                            1.0D
                                    )
                                    .forGetter(
                                            PossessedGhostState::intrinsicStrength
                                    )

                    ).apply(
                            instance,
                            PossessedGhostState::new
                    )
            );


    /*
     * ============================================================
     * 网络 StreamCodec
     * ============================================================
     */

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            PossessedGhostState
            > STREAM_CODEC =
            StreamCodec.composite(

                    ByteBufCodecs.DOUBLE,
                    PossessedGhostState::revival,

                    ByteBufCodecs.DOUBLE,
                    PossessedGhostState::shallowStun,

                    ByteBufCodecs.VAR_LONG,
                    PossessedGhostState::stunTicks,

                    ByteBufCodecs.BOOL,
                    PossessedGhostState::permanentStun,

                    ByteBufCodecs.VAR_LONG,
                    PossessedGhostState::lastAbilityUseTick,

                    ByteBufCodecs.DOUBLE,
                    PossessedGhostState::intrinsicStrength,

                    PossessedGhostState::new
            );


    /*
     * ============================================================
     * 创建初始状态
     * ============================================================
     */

    /**
     * 创建一只刚被驾驭时的状态。
     *
     * 初始复苏为 0。
     * 初始浅死机为 0。
     * 初始没有死机。
     *
     * 本质强度由具体 Ability 提供。
     */
    public static PossessedGhostState create(
            double intrinsicStrength
    ) {

        return new PossessedGhostState(
                0.0D,
                0.0D,
                0L,
                false,
                Long.MIN_VALUE,
                intrinsicStrength
        );
    }


    /*
     * ============================================================
     * 死机判断
     * ============================================================
     */

    /**
     * 当前是否处于普通死机。
     */
    public boolean isStunned() {

        return !permanentStun
                && stunTicks > 0;
    }


    /**
     * 当前是否永久死机。
     */
    public boolean isPermanentlyStunned() {

        return permanentStun;
    }


    /**
     * 当前是否处于任意死机状态。
     */
    public boolean isAnyStun() {

        return permanentStun
                || stunTicks > 0;
    }
}