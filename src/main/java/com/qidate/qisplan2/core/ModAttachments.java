package com.qidate.qisplan2.core;

import com.mojang.serialization.Codec;
import com.qidate.qisplan2.ghost.layer.GhostLayerData;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.partition.PartitionReturnData;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostData;
import com.qidate.qisplan2.ghost.possession.manager.SuppressionAllocation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import static com.qidate.qisplan2.core.ModRegistries.ATTACHMENT_TYPES;

public class ModAttachments {

    private ModAttachments() {}

    public static void init() {
        /*
         * 故意留空。
         *
         * 调用这个方法本身，就会强制 JVM
         * 在正确的时机完成 ModAttachments 的静态初始化。
         */
    }

    /**
     * 旧驭鬼存档迁移入口。运行时统一使用 PLAYER_GHOST_MODULES，此附件仅用于读取旧存档。
     *
     * Key：
     *     鬼的 ResourceLocation
     *
     * Value：
     *     该鬼的完整 PossessedGhostData
     *
     * 因此玩家可以同时驾驭多只鬼。
     */
    public static final DeferredHolder<
            AttachmentType<?>,
            AttachmentType<Map<ResourceLocation, PossessedGhostData>>
            > POSSESSED_GHOSTS =
            ATTACHMENT_TYPES.register(
                    "possessed_ghosts",
                    () -> AttachmentType
                            .<Map<ResourceLocation, PossessedGhostData>>builder(
                                    (Supplier<
                                            Map<ResourceLocation, PossessedGhostData>
                                            >)
                                            HashMap::new
                            )
                            .serialize(
                                    Codec.unboundedMap(
                                            ResourceLocation.CODEC,
                                            PossessedGhostData.CODEC
                                    )
                            )
                            .sync(
                                    ByteBufCodecs.map(
                                            HashMap::new,
                                            ResourceLocation.STREAM_CODEC,
                                            PossessedGhostData.STREAM_CODEC,
                                            32
                                    )
                            )
                            .build()
            );


    /** 玩家直接保存模块实例，与实体、方块和物品使用同一种数据结构。 */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<List<GhostModuleData.Entry>>> PLAYER_GHOST_MODULES =
            ATTACHMENT_TYPES.register("player_ghost_modules", () -> AttachmentType
                    .<List<GhostModuleData.Entry>>builder(() -> List.of())
                    .serialize(GhostModuleData.CODEC)
                    .sync(ByteBufCodecs.fromCodecWithRegistries(GhostModuleData.CODEC))
                    .build());

    // 必死诅咒层数（0~10，同步到客户端供骷髅条显示）
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> DEATH_CURSE_COUNT =
            ATTACHMENT_TYPES.register(
                    "death_curse_count",
                    () -> AttachmentType.builder(() -> 0)
                            .serialize(Codec.INT)        // 存档用
                            .sync(ByteBufCodecs.VAR_INT) // 同步到客户端用
                            .build()
            );


    // 划分维度数据
    public static final DeferredHolder<
            AttachmentType<?>,
            AttachmentType<PartitionReturnData>
            > PARTITION_RETURN_DATA =
            ATTACHMENT_TYPES.register(
                    "partition_return_data",
                    () ->
                            AttachmentType
                                    .builder(
                                            () ->
                                                    PartitionReturnData.EMPTY
                                    )
                                    .serialize(
                                            PartitionReturnData.CODEC
                                    )
                                    .build()
            );

    // 实体鬼域所在层数
    public static final DeferredHolder<
            AttachmentType<?>,
            AttachmentType<GhostLayerData>
            > GHOST_LAYER =
            ATTACHMENT_TYPES.register(
                    "ghost_layer",
                    () ->
                            AttachmentType
                                    .builder(
                                            () ->
                                                    GhostLayerData.DEFAULT
                                    )
                                    .serialize(
                                            GhostLayerData.CODEC
                                    )
                                    .sync(
                                            GhostLayerData.STREAM_CODEC
                                    )
                                    .copyOnDeath()
                                    .build()
            );

    // 实体肉身鬼域抵抗
    public static final DeferredHolder<AttachmentType<?>,AttachmentType<Integer>> GHOST_RESISTANCE =
            ATTACHMENT_TYPES.register(
                    "ghost_resistance",
                    () ->
                            AttachmentType
                                    .builder(
                                            () -> 0
                                    )
                                    .serialize(
                                            Codec.INT
                                    )
                                    .sync(
                                            ByteBufCodecs.VAR_INT
                                    )
                                    .copyOnDeath()
                                    .build()
            );

    public static final DeferredHolder<
            AttachmentType<?>,
            AttachmentType<Map<ResourceLocation, Map<ResourceLocation, List<SuppressionAllocation>>>>
            > GHOST_SUPPRESSION_ALLOCATION =
            ATTACHMENT_TYPES.register(
                    "ghost_suppression_allocation",
                    () -> AttachmentType
                            .<Map<ResourceLocation, Map<ResourceLocation, List<SuppressionAllocation>>>>builder(
                                    (Supplier<
                                            Map<ResourceLocation, Map<ResourceLocation, List<SuppressionAllocation>>>
                                            >) HashMap::new
                            )
                            .serialize(
                                    Codec.unboundedMap(
                                            ResourceLocation.CODEC,
                                            Codec.unboundedMap(
                                                    ResourceLocation.CODEC,
                                                    SuppressionAllocation.CODEC.listOf()
                                            )
                                    )
                            )
                            .sync(
                                    ByteBufCodecs.map(
                                            HashMap::new,
                                            ResourceLocation.STREAM_CODEC,

                                            ByteBufCodecs.map(
                                                    HashMap::new,
                                                    ResourceLocation.STREAM_CODEC,

                                                    SuppressionAllocation.STREAM_CODEC
                                                            .apply(
                                                                    ByteBufCodecs.list()
                                                            ),

                                                    32
                                            ),

                                            32
                                    )
                            )
                            .copyOnDeath()
                            .build()
            );
}
