package com.qidate.qisplan2.ghost;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record PossessedGhostState(
        double revival,
        double shallowStun,
        long lastAbilityUseTick
) {

    // 数据存档 Codec
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


    // 客户端同步
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
                    PossessedGhostState::lastAbilityUseTick,

                    PossessedGhostState::new
            );


    // 默认状态
    public static PossessedGhostState create() {
        return new PossessedGhostState(
                0.0D,
                0.0D,
                Long.MIN_VALUE
        );
    }
}