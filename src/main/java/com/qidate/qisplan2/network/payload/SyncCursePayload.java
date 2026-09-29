package com.qidate.qisplan2.network.payload;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * S2C：同步当前玩家的全部诅咒状态。
 */
public record SyncCursePayload(
        List<CurseData> curses
) implements CustomPacketPayload {

    public static final Type<
            SyncCursePayload
            > TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            "qisplan2",
                            "sync_curses"
                    )
            );


    /**
     * 单个诅咒的客户端展示数据。
     */
    public record CurseData(
            UUID id,
            ResourceLocation type,
            int strength,
            int remainingTicks
    ) {

        public static final StreamCodec<
                RegistryFriendlyByteBuf,
                CurseData
                > STREAM_CODEC =
                StreamCodec.composite(
                        UUIDUtil.STREAM_CODEC,
                        CurseData::id,

                        ResourceLocation.STREAM_CODEC,
                        CurseData::type,

                        ByteBufCodecs.INT,
                        CurseData::strength,

                        ByteBufCodecs.INT,
                        CurseData::remainingTicks,

                        CurseData::new
                );
    }


    /**
     * 整个诅咒列表的数据编解码器。
     */
    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            SyncCursePayload
            > STREAM_CODEC =
            ByteBufCodecs.collection(
                    ArrayList::new,
                    CurseData.STREAM_CODEC
            ).map(
                    list -> new SyncCursePayload(list),
                    payload -> new ArrayList<>(payload.curses())
            );


    @Override
    public Type<? extends SyncCursePayload> type() {
        return TYPE;
    }
}