package com.qidate.qisplan2.core;

import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.entity.TestGeckoEntity;
import com.qidate.qisplan2.entity.calling.CallingGhost;
import com.qidate.qisplan2.entity.ghostmist.GhostMist;
import com.qidate.qisplan2.entity.nightwanderer.NightWanderer;
import com.qidate.qisplan2.entity.painting.GhostPaintingEntity;
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

    // 所有已迁移的实体厉鬼共用此实体类型，具体身份与行为由其模块决定。
    public static final DeferredHolder<EntityType<?>, EntityType<ModularGhostEntity>> MODULAR_GHOST =
            ENTITY_TYPES.register(
                    "modular_ghost",
                    () -> EntityType.Builder
                            .of(
                                    ModularGhostEntity::new,
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
                                                    "modular_ghost"
                                            )
                                            .toString()
                            )
            );

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

    // 鬼雾
    public static final DeferredHolder<EntityType<?>, EntityType<GhostMist>> GHOST_MIST =
            ENTITY_TYPES.register(
                    "ghost_mist",
                    () -> EntityType.Builder
                            .of(
                                    GhostMist::new,
                                    MobCategory.MONSTER
                            )
                            .sized(
                                    0.6F,
                                    1.8F
                            )
                            .clientTrackingRange(8)
                            .updateInterval(3)
                            .build(
                                    ResourceLocation.fromNamespaceAndPath(
                                            MODID,
                                            "ghost_mist"
                                    ).toString()
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

    // GeckoLib 测试实体
    public static final DeferredHolder<EntityType<?>, EntityType<TestGeckoEntity>> TEST_GECKO =
            ENTITY_TYPES.register(
                    "test_gecko",
                    () -> EntityType.Builder
                            .of(TestGeckoEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(8)
                            .updateInterval(3)
                            .build(
                                    ResourceLocation.fromNamespaceAndPath(
                                            MODID,
                                            "test_gecko"
                                    ).toString()
                            )
            );
}
