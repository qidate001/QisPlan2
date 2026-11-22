package com.qidate.qisplan2.ghost.possession.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

/**
 * 驭鬼界面的兼容数据视图，同时保留旧存档解码能力。
 * 当前唯一持久状态为宿主的模块数据；此对象由 PlayerGhostData 投影，
 * 不能作为独立的鬼域声明或第二份运行时状态。
 */
public record PossessedGhostData(
        PossessedGhostState state,
        Optional<PossessedGhostDomainData> domain
) {

    public static final Codec<PossessedGhostData> CODEC =
            RecordCodecBuilder.create(instance ->
                    instance.group(
                            PossessedGhostState.CODEC
                                    .fieldOf("state")
                                    .forGetter(
                                            PossessedGhostData::state
                                    ),

                            PossessedGhostDomainData.CODEC
                                    .optionalFieldOf("domain")
                                    .forGetter(
                                            PossessedGhostData::domain
                                    )
                    ).apply(
                            instance,
                            PossessedGhostData::new
                    )
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, PossessedGhostData> STREAM_CODEC =
            StreamCodec.composite(
                    PossessedGhostState.STREAM_CODEC,
                    PossessedGhostData::state,

                    ByteBufCodecs.optional(
                            PossessedGhostDomainData.STREAM_CODEC
                    ),
                    PossessedGhostData::domain,

                    PossessedGhostData::new
            );

    /**
     * 创建一只刚刚被驾驭的厉鬼。
     *
     * <p>
     * 默认没有鬼域数据。
     * </p>
     */
    public static PossessedGhostData create(
            double initialStrength
    ) {
        return new PossessedGhostData(
                PossessedGhostState.create(initialStrength),
                Optional.empty()
        );
    }

    public PossessedGhostData withState(
            PossessedGhostState state
    ) {
        return new PossessedGhostData(
                state,
                domain
        );
    }

    public PossessedGhostData withDomain(
            PossessedGhostDomainData domain
    ) {
        return new PossessedGhostData(
                state,
                Optional.ofNullable(domain)
        );
    }

    public PossessedGhostData withoutDomain() {
        return new PossessedGhostData(
                state,
                Optional.empty()
        );
    }
}