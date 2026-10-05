package com.qidate.qisplan2.ghost.possession.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 一只被驾驭的厉鬼当前的通用状态。
 *
 * <p>
 * 这里保存的是所有厉鬼都拥有的状态，
 * 与具体厉鬼的专属数据无关。
 * </p>
 *
 * <p>
 * 这些数据全部属于玩家的持久化 Attachment，
 * 因此玩家下线、重新上线以及服务器重启后都会保留。
 * </p>
 */
public record PossessedGhostState(

        /*
         * ============================================================
         * 复苏
         * ============================================================
         *
         * 0.0 = 尚未复苏
         * 1.0 = 完全复苏
         */
        double revival,

        /*
         * ============================================================
         * 当前强度
         * ============================================================
         *
         * 强度是所有厉鬼都拥有的通用状态。
         *
         * 它不是“本质强度”，
         * 也不是固定不变的属性。
         *
         * 厉鬼可以随着游戏进程增强或削弱。
         */
        double strength,

        /*
         * ============================================================
         * 浅死机
         * ============================================================
         */
        double shallowStun,

        /*
         * ============================================================
         * 普通死机剩余 Tick
         * ============================================================
         */
        long stunTicks,

        /*
         * ============================================================
         * 永久死机
         * ============================================================
         */
        boolean permanentStun,

        /*
         * ============================================================
         * 上一次主动能力使用时间
         * ============================================================
         *
         * 使用 Minecraft GameTime。
         */
        long lastAbilityUseTick

) {

    /**
     * 浅死机最大值。
     */
    public static final double MAX_SHALLOW_STUN = 100.0D;


    /*
     * ============================================================
     * 数据校正
     * ============================================================
     */

    public PossessedGhostState {

        revival =
                Math.clamp(
                        revival,
                        0.0D,
                        1.0D
                );

        strength =
                Math.max(
                        0.0D,
                        strength
                );

        shallowStun =
                Math.clamp(
                        shallowStun,
                        0.0D,
                        MAX_SHALLOW_STUN
                );

        stunTicks =
                Math.max(
                        0L,
                        stunTicks
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
                                    .fieldOf("strength")
                                    .forGetter(
                                            PossessedGhostState::strength
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
                                    )

                    ).apply(
                            instance,
                            PossessedGhostState::new
                    )
            );


    /*
     * ============================================================
     * 网络同步
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
                    PossessedGhostState::strength,

                    ByteBufCodecs.DOUBLE,
                    PossessedGhostState::shallowStun,

                    ByteBufCodecs.VAR_LONG,
                    PossessedGhostState::stunTicks,

                    ByteBufCodecs.BOOL,
                    PossessedGhostState::permanentStun,

                    ByteBufCodecs.VAR_LONG,
                    PossessedGhostState::lastAbilityUseTick,

                    PossessedGhostState::new
            );


    /*
     * ============================================================
     * 创建初始状态
     * ============================================================
     */

    /**
     * 创建一只刚刚被驾驭的厉鬼的初始状态。
     *
     * @param initialStrength 初始强度
     */
    public static PossessedGhostState create(
            double initialStrength
    ) {

        return new PossessedGhostState(
                0.0D,
                initialStrength,
                0.0D,
                0L,
                false,
                Long.MIN_VALUE
        );
    }


    /*
     * ============================================================
     * 状态判断
     * ============================================================
     */

    /**
     * 是否处于普通死机。
     */
    public boolean isStunned() {

        return !permanentStun
                && stunTicks > 0;
    }


    /**
     * 是否处于永久死机。
     */
    public boolean isPermanentlyStunned() {

        return permanentStun;
    }


    /**
     * 是否处于任何形式的死机。
     */
    public boolean isAnyStun() {

        return permanentStun
                || stunTicks > 0;
    }
}