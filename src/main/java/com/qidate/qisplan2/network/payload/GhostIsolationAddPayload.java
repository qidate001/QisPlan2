package com.qidate.qisplan2.network.payload;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.isolation.GhostIsolationCuboid;
import com.qidate.qisplan2.ghost.isolation.GhostIsolationRegion;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * S2C：同步一个灵异隔绝区域到客户端。
 *
 * <p>
 * 一个 Region 可能由多个 Cuboid 组成，
 * 因此一个网络包直接携带整个 Region 的所有 Cuboid。
 * </p>
 *
 * <p>
 * 客户端收到数据后，
 * 会将其转换为 ClientGhostIsolationRegion。
 * </p>
 */
public record GhostIsolationAddPayload(
        UUID regionId,
        ResourceLocation dimension,
        List<CuboidEntry> cuboids
) implements CustomPacketPayload {

    /**
     * 灵异隔绝区域中的一个长方体网络数据。
     *
     * <p>
     * 网络层不直接传输服务端的 GhostIsolationCuboid，
     * 而是使用这个简单的数据结构进行序列化。
     * </p>
     */
    public record CuboidEntry(
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ
    ) {

        /**
         * 将服务端 Cuboid 转换为网络数据。
         *
         * @param cuboid 服务端灵异隔绝长方体
         * @return 网络传输用的长方体数据
         */
        public static CuboidEntry from(
                GhostIsolationCuboid cuboid
        ) {
            return new CuboidEntry(
                    cuboid.getMin().getX(),
                    cuboid.getMin().getY(),
                    cuboid.getMin().getZ(),
                    cuboid.getMax().getX(),
                    cuboid.getMax().getY(),
                    cuboid.getMax().getZ()
            );
        }
    }

    /**
     * 数据包类型。
     */
    public static final Type<GhostIsolationAddPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            QisPlan2.MODID,
                            "ghost_isolation_add"
                    )
            );

    /**
     * 数据包序列化器。
     *
     * <p>
     * 这里使用 StreamCodec.of 手动读写，
     * 与当前 GhostDomain 网络包保持一致。
     * </p>
     */
    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            GhostIsolationAddPayload
            > STREAM_CODEC = StreamCodec.of(

            (buf, payload) -> {

                buf.writeUUID(
                        payload.regionId()
                );

                buf.writeResourceLocation(
                        payload.dimension()
                );

                buf.writeVarInt(
                        payload.cuboids().size()
                );

                for (CuboidEntry cuboid :
                        payload.cuboids()) {

                    buf.writeInt(
                            cuboid.minX()
                    );

                    buf.writeInt(
                            cuboid.minY()
                    );

                    buf.writeInt(
                            cuboid.minZ()
                    );

                    buf.writeInt(
                            cuboid.maxX()
                    );

                    buf.writeInt(
                            cuboid.maxY()
                    );

                    buf.writeInt(
                            cuboid.maxZ()
                    );
                }
            },

            buf -> {

                UUID regionId =
                        buf.readUUID();

                ResourceLocation dimension =
                        buf.readResourceLocation();

                int count =
                        buf.readVarInt();

                List<CuboidEntry> cuboids =
                        new ArrayList<>(count);

                for (int i = 0; i < count; i++) {

                    cuboids.add(
                            new CuboidEntry(
                                    buf.readInt(),
                                    buf.readInt(),
                                    buf.readInt(),
                                    buf.readInt(),
                                    buf.readInt(),
                                    buf.readInt()
                            )
                    );
                }

                return new GhostIsolationAddPayload(
                        regionId,
                        dimension,
                        cuboids
                );
            }
    );

    /**
     * 从服务端灵异隔绝 Region 创建网络数据包。
     *
     * @param region 服务端灵异隔绝区域
     * @return 用于发送给客户端的数据包
     */
    public static GhostIsolationAddPayload from(
            GhostIsolationRegion region
    ) {

        List<CuboidEntry> cuboids =
                new ArrayList<>(
                        region.getCuboids().size()
                );

        for (GhostIsolationCuboid cuboid :
                region.getCuboids()) {

            cuboids.add(
                    CuboidEntry.from(
                            cuboid
                    )
            );
        }

        return new GhostIsolationAddPayload(
                region.getId(),
                region.getDimension().location(),
                cuboids
        );
    }

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