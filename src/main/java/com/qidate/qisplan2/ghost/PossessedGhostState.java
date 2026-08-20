package com.qidate.qisplan2.ghost;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record PossessedGhostState(
        /**
         * 复苏值：
         * 0.0 ~ 1.0 = 0% ~ 100%
         */
        double revival,

        /**
         * 浅死机值。
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
        long lastAbilityUseTick
) {

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
                                    )

                    ).apply(
                            instance,
                            PossessedGhostState::new
                    )
            );


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

                    PossessedGhostState::new
            );


    public static PossessedGhostState create() {
        return new PossessedGhostState(
                0.0D,
                0.0D,
                0L,
                false,
                Long.MIN_VALUE
        );
    }


    /**
     * 当前是否处于普通死机状态。
     */
    public boolean isStunned() {
        return !permanentStun && stunTicks > 0;
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
        return permanentStun || stunTicks > 0;
    }
}