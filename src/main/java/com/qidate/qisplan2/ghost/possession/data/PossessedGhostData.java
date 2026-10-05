package com.qidate.qisplan2.ghost.possession.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

/**
 * ========================================
 * 单只被驾驭厉鬼的完整持久数据
 * ========================================
 *
 * <p>
 * PossessedGhostData 表示玩家所驾驭的一只厉鬼
 * 的完整持久记录。
 * </p>
 *
 * <p>
 * state：
 *     所有厉鬼共有的通用状态。
 *
 * domain：
 *     鬼域专属持久数据。
 *
 * 并不是所有厉鬼都拥有鬼域，
 * 因此 domain 使用 Optional 表示。
 * </p>
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