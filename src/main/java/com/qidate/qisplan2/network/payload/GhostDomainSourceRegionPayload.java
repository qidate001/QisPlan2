package com.qidate.qisplan2.network.payload;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * 鬼域 Source 所在灵异隔绝 Region 同步数据包。
 *
 * <p>
 * 服务端使用本数据包通知客户端：
 * 某个 GhostDomain 的 Source
 * 当前所在的灵异隔绝 Region 发生了变化。
 *
 * <p>
 * Region UUID 是服务端与客户端之间
 * 用于识别同一个灵异隔绝空间的真实身份。
 *
 * <p>
 * 如果 Source 当前位于开放空间，
 * 则 {@code regionId} 为 {@code null}。
 *
 * <p>
 * 注意：
 * 这里同步的是 Region UUID，
 * 而不是客户端 GPU 使用的临时 Region Index。
 */
public record GhostDomainSourceRegionPayload(
        UUID domainId,
        UUID regionId
) implements CustomPacketPayload {

    public static final Type<
            GhostDomainSourceRegionPayload
            > TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_domain_source_region"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            GhostDomainSourceRegionPayload
            > STREAM_CODEC = StreamCodec.of(

            (buf, payload) -> {

                /*
                 * ====================================================
                 * 鬼域 UUID
                 * ====================================================
                 */
                buf.writeUUID(
                        payload.domainId()
                );

                /*
                 * ====================================================
                 * Source 所在的 Region UUID。
                 *
                 * null 表示 Source 当前位于开放空间。
                 *
                 * Minecraft 的 Buffer 没有直接写入
                 * nullable UUID 的专用方法，
                 * 因此使用一个布尔值记录是否存在 Region。
                 * ====================================================
                 */
                buf.writeBoolean(
                        payload.regionId() != null
                );

                if (payload.regionId() != null) {

                    buf.writeUUID(
                            payload.regionId()
                    );
                }
            },

            buf -> {

                UUID domainId =
                        buf.readUUID();

                boolean hasRegion =
                        buf.readBoolean();

                UUID regionId =
                        hasRegion
                                ? buf.readUUID()
                                : null;

                return new GhostDomainSourceRegionPayload(
                        domainId,
                        regionId
                );
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}