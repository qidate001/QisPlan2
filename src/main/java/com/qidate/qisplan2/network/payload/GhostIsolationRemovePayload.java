package com.qidate.qisplan2.network.payload;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * S2C：从客户端移除一个灵异隔绝区域。
 *
 * <p>
 * 客户端只需要 Region 的 UUID，
 * 因此不需要传输任何空间数据。
 * </p>
 */
public record GhostIsolationRemovePayload(
        UUID regionId
) implements CustomPacketPayload {

    /**
     * 数据包类型。
     */
    public static final Type<GhostIsolationRemovePayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_isolation_remove"
                    )
            );

    /**
     * 数据包序列化器。
     *
     * <p>
     * 只需要同步 Region UUID。
     * </p>
     */
    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            GhostIsolationRemovePayload
            > STREAM_CODEC = StreamCodec.of(

            (buf, payload) -> {

                buf.writeUUID(
                        payload.regionId()
                );
            },

            buf -> new GhostIsolationRemovePayload(
                    buf.readUUID()
            )
    );

    /**
     * 获取该数据包的类型。
     *
     * @return 数据包类型
     */
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}