package com.qidate.qisplan2.ghost.possession.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record PossessedGhostData(
        PossessedGhostState state
) {

    /*
     * ============================================================
     * 持久化
     * ============================================================
     */

    public static final Codec<PossessedGhostData> CODEC =
            RecordCodecBuilder.create(instance ->
                    instance.group(

                            PossessedGhostState.CODEC
                                    .fieldOf("state")
                                    .forGetter(
                                            PossessedGhostData::state
                                    )

                    ).apply(
                            instance,
                            PossessedGhostData::new
                    )
            );


    /*
     * ============================================================
     * 网络同步
     * ============================================================
     */

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            PossessedGhostData
            > STREAM_CODEC =
            StreamCodec.composite(

                    PossessedGhostState.STREAM_CODEC,
                    PossessedGhostData::state,

                    PossessedGhostData::new
            );


    /*
     * ============================================================
     * 创建
     * ============================================================
     */

    public static PossessedGhostData create(
            double initialStrength
    ) {

        return new PossessedGhostData(
                PossessedGhostState.create(
                        initialStrength
                )
        );
    }


    /*
     * ============================================================
     * 状态替换
     * ============================================================
     *
     * 以后 PossessedGhostData 增加其他字段时，
     * 这里可以保证 setState 不会把那些字段丢掉。
     */

    public PossessedGhostData withState(
            PossessedGhostState state
    ) {

        return new PossessedGhostData(
                state
        );
    }
}