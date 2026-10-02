package com.qidate.qisplan2.network.payload;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 清空客户端全部灵异隔绝区域数据的网络 Payload。
 *
 * <p>
 * 主要用于玩家切换维度时，
 * 清理客户端旧维度留下的灵异隔绝区域缓存。
 * </p>
 */
public record GhostIsolationClearPayload()
        implements CustomPacketPayload {

    /**
     * Payload 类型。
     */
    public static final Type<GhostIsolationClearPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_isolation_clear"
                    )
            );

    /**
     * 网络编解码器。
     */
    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            GhostIsolationClearPayload
            > STREAM_CODEC = StreamCodec.unit(
            new GhostIsolationClearPayload()
    );

    /**
     * 获取 Payload 类型。
     *
     * @return Payload 类型
     */
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}