package com.qidate.qisplan2.ghost;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record PossessedGhostState(
        double revival,
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

    public static PossessedGhostState create() {
        return new PossessedGhostState(
                0.0D,
                Long.MIN_VALUE
        );
    }
}