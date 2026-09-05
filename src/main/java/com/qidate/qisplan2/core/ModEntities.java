package com.qidate.qisplan2.core;

import com.qidate.qisplan2.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Supplier;

import static com.qidate.qisplan2.QisPlan2.MODID;
import static com.qidate.qisplan2.core.ModRegistries.ENTITY_TYPES;

public class ModEntities {

    private ModEntities(){}

    public static void init() {
        /*
         * 故意留空。
         *
         * 调用这个方法本身，就会强制 JVM
         * 在正确的时机完成 ModEntities 的静态初始化。
         */
    }

    // 夜游鬼
    public static final DeferredHolder<EntityType<?>, EntityType<NightWanderer>> NIGHT_WANDERER =
            ENTITY_TYPES.register(
                    "night_wanderer",
                    () -> EntityType.Builder
                            .of(NightWanderer::new, MobCategory.MISC)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(8)
                            .updateInterval(3)
                            .build(
                                    ResourceLocation.fromNamespaceAndPath(
                                            MODID,
                                            "night_wanderer"
                                    ).toString()
                            )
            );

    // 不可视之鬼
    public static final DeferredHolder<
            EntityType<?>,
            EntityType<InvisibleGhost>
            > INVISIBLE_GHOST =
            ENTITY_TYPES.register(
                    "invisible_ghost",
                    () -> EntityType.Builder
                            .of(
                                    InvisibleGhost::new,
                                    MobCategory.MONSTER
                            )
                            .sized(
                                    0.6F,
                                    1.95F
                            )
                            .clientTrackingRange(8)
                            .updateInterval(3)
                            .build(
                                    ResourceLocation
                                            .fromNamespaceAndPath(
                                                    MODID,
                                                    "invisible_ghost"
                                            )
                                            .toString()
                            )
            );

    // 敲门鬼
    public static final Supplier<
            EntityType<KnockingGhost>
            > KNOCKING_GHOST =
            ENTITY_TYPES.register(
                    "knocking_ghost",
                    () -> EntityType.Builder
                            .of(
                                    KnockingGhost::new,
                                    MobCategory.MONSTER
                            )
                            .sized(
                                    0.8F,
                                    1.95F
                            )
                            .clientTrackingRange(64)
                            .build(
                                    ResourceLocation.fromNamespaceAndPath(
                                            MODID,
                                            "knocking_ghost"
                                    ).toString()
                            )
            );

    // 开门鬼
    public static final Supplier<
            EntityType<OpeningGhost>
            > OPENING_GHOST =
            ENTITY_TYPES.register(
                    "opening_ghost",
                    () -> EntityType.Builder
                            .of(
                                    OpeningGhost::new,
                                    MobCategory.MONSTER
                            )
                            .sized(
                                    0.8F,
                                    1.95F
                            )
                            .clientTrackingRange(64)
                            .build(
                                    ResourceLocation.fromNamespaceAndPath(
                                            MODID,
                                            "opening_ghost"
                                    ).toString()
                            )
            );

    // 关门鬼
    public static final Supplier<
            EntityType<ClosingGhost>
            > CLOSING_GHOST =
            ENTITY_TYPES.register(
                    "closing_ghost",
                    () -> EntityType.Builder
                            .of(
                                    ClosingGhost::new,
                                    MobCategory.MONSTER
                            )
                            .sized(
                                    0.8F,
                                    1.95F
                            )
                            .clientTrackingRange(64)
                            .build(
                                    ResourceLocation.fromNamespaceAndPath(
                                            MODID,
                                            "closing_ghost"
                                    ).toString()
                            )
            );

    // 喊人鬼
    public static final Supplier<EntityType<CallingGhost>> CALLING_GHOST =
            ENTITY_TYPES.register(
                    "calling_ghost",
                    () -> EntityType.Builder
                            .of(
                                    CallingGhost::new,
                                    MobCategory.MISC
                            )
                            .sized(
                                    0.6F,
                                    1.8F
                            )
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(
                                    "qisplan2:calling_ghost"
                            )
            );

    // 鬼画
    public static final DeferredHolder<
            EntityType<?>,
            EntityType<GhostPaintingEntity>
            > GHOST_PAINTING =
            ENTITY_TYPES.register(
                    "ghost_painting",
                    () -> EntityType.Builder.<GhostPaintingEntity>of(
                                    GhostPaintingEntity::new,
                                    MobCategory.MISC
                            )
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build(
                                    "qisplan2:ghost_painting"
                            )
            );
}
