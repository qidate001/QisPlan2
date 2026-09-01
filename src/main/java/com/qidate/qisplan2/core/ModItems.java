package com.qidate.qisplan2.core;

import com.mojang.serialization.Codec;
import com.qidate.qisplan2.item.*;
import net.minecraft.Util;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.EnumMap;
import java.util.List;

import static com.qidate.qisplan2.QisPlan2.MODID;
import static com.qidate.qisplan2.core.ModEntities.*;
import static com.qidate.qisplan2.core.ModEntities.CLOSING_GHOST;
import static com.qidate.qisplan2.core.ModEntities.OPENING_GHOST;
import static com.qidate.qisplan2.core.ModRegistries.*;

public class ModItems {

    private ModItems() {}

    public static void init() {
        /*
         * 故意留空。
         *
         * 调用这个方法本身，就会强制 JVM
         * 在正确的时机完成 ModItems 的静态初始化。
         */
    }



    // 鬼金币
    public static final DeferredItem<GhostCoin> GHOST_COIN =
            ITEMS.register(
                    "ghost_coin",
                    () -> new GhostCoin(new Item.Properties())
            );

    // 鬼石指
    public static final DeferredItem<Item> GHOST_STONE_FINGER =
            ITEMS.registerSimpleItem(
                    "ghost_stone_finger"
            );

    // 香火灰
    public static final DeferredItem<Item> INCENSE_ASH =
            ITEMS.registerSimpleItem(
                    "incense_ash"
            );

    // 鬼寿衣
    public static final DeferredHolder<
            ArmorMaterial,
            ArmorMaterial
            > GHOST_SHROUD_MATERIAL =
            ARMOR_MATERIALS.register(
                    "ghost_shroud",
                    () -> new ArmorMaterial(
                            Util.make(
                                    new EnumMap<>(
                                            ArmorItem.Type.class
                                    ),
                                    map -> {
                                        map.put(
                                                ArmorItem.Type.BOOTS,
                                                0
                                        );

                                        map.put(
                                                ArmorItem.Type.LEGGINGS,
                                                0
                                        );

                                        map.put(
                                                ArmorItem.Type.CHESTPLATE,
                                                4
                                        );

                                        map.put(
                                                ArmorItem.Type.HELMET,
                                                0
                                        );

                                        map.put(
                                                ArmorItem.Type.BODY,
                                                0
                                        );
                                    }
                            ),
                            10,
                            SoundEvents.ARMOR_EQUIP_LEATHER,
                            () -> Ingredient.EMPTY,
                            List.of(
                                    new ArmorMaterial.Layer(
                                            ResourceLocation.fromNamespaceAndPath(
                                                    MODID,
                                                    "ghost_shroud"
                                            )
                                    )
                            ),
                            0.0F,
                            0.0F
                    )
            );


    public static final DeferredItem<GhostShroudItem> GHOST_SHROUD =
            ITEMS.register(
                    "ghost_shroud",
                    () -> new GhostShroudItem(
                            GHOST_SHROUD_MATERIAL,
                            ArmorItem.Type.CHESTPLATE,
                            new Item.Properties()
                                    .stacksTo(1)
                    )
            );


    // 鬼书
    public static final DeferredItem<GhostBookItem> GHOST_BOOK =
            ITEMS.register(
                    "ghost_book",
                    () -> new GhostBookItem(
                            new Item.Properties()
                                    .stacksTo(1)
                    )
            );

    // 鬼伞
    public static final DeferredHolder<
            DataComponentType<?>,
            DataComponentType<Boolean>
            > GHOST_UMBRELLA_OPEN =
            DATA_COMPONENTS.register(
                    "ghost_umbrella_open",
                    () -> DataComponentType.<Boolean>builder()
                            .persistent(Codec.BOOL)
                            .networkSynchronized(
                                    ByteBufCodecs.BOOL
                            )
                            .build()
            );

    public static final DeferredItem<GhostUmbrellaItem>
            GHOST_UMBRELLA =
            ITEMS.register(
                    "ghost_umbrella",
                    () -> new GhostUmbrellaItem(
                            new Item.Properties()
                                    .stacksTo(1)
                    )
            );

    // 必死诅咒之剑
    public static final DeferredItem<DeathCurseSword> DEATH_CURSE_SWORD =
            ITEMS.register(
                    "death_curse_sword",
                    () -> new DeathCurseSword(new Item.Properties())
            );

    // 鬼白粥
    public static final DeferredItem<GhostWhitePorridgeItem>
            GHOST_WHITE_PORRIDGE =
            ITEMS.register(
                    "ghost_white_porridge",
                    () -> new GhostWhitePorridgeItem(
                            new Item.Properties()
                                    .stacksTo(1)
                    )
            );

    // 棺材钉
    public static final DeferredItem<CoffinNailItem>
            COFFIN_NAIL =
            ITEMS.register(
                    "coffin_nail",
                    () -> new CoffinNailItem(
                            new Item.Properties()
                                    .stacksTo(1)
                    )
            );

    // 鬼画
    public static final DeferredItem<Item> GHOST_PAINTING =
            ITEMS.register(
                    "ghost_painting",
                    () -> new GhostPaintingItem(
                            new Item.Properties()
                    )
            );

    // 夜游鬼刷怪蛋
    public static final DeferredItem<SpawnEggItem> NIGHT_WANDERER_SPAWN_EGG =
            ITEMS.register(
                    "night_wanderer_spawn_egg",
                    () -> new SpawnEggItem(
                            NIGHT_WANDERER.get(),
                            0x191919, // 基础颜色
                            0x6B6B6B, // 斑点颜色
                            new Item.Properties()
                    )
            );


    // 不可见之鬼刷怪蛋
    public static final DeferredItem<SpawnEggItem> INVISIBLE_GHOST_SPAWN_EGG =
            ITEMS.register(
                    "invisible_ghost_spawn_egg",
                    () -> new SpawnEggItem(
                            INVISIBLE_GHOST.get(),
                            0x191919, // 基础颜色
                            0x6B6B6B, // 斑点颜色
                            new Item.Properties()
                    )
            );


    // 敲门鬼刷怪蛋
    public static final DeferredItem<SpawnEggItem> KNOCKING_GHOST_SPAWN_EGG =
            ITEMS.register(
                    "knocking_ghost_spawn_egg",
                    () -> new SpawnEggItem(
                            KNOCKING_GHOST.get(),
                            0x191919, // 基础颜色
                            0x6B6B6B, // 斑点颜色
                            new Item.Properties()
                    )
            );


    // 开门鬼刷怪蛋
    public static final DeferredItem<SpawnEggItem>
            OPENING_GHOST_SPAWN_EGG =
            ITEMS.register(
                    "opening_ghost_spawn_egg",
                    () -> new SpawnEggItem(
                            OPENING_GHOST.get(),
                            0x191919,
                            0x4A90E2,
                            new Item.Properties()
                    )
            );

    // 关门鬼刷怪蛋
    public static final DeferredItem<SpawnEggItem>
            CLOSING_GHOST_SPAWN_EGG =
            ITEMS.register(
                    "closing_ghost_spawn_egg",
                    () -> new SpawnEggItem(
                            CLOSING_GHOST.get(),
                            0x191919,
                            0xB44AFF,
                            new Item.Properties()
                    )
            );

    // 喊人鬼刷怪蛋
    public static final DeferredItem<SpawnEggItem>
            CALLING_GHOST_SPAWN_EGG =
            ITEMS.register(
                    "calling_ghost_spawn_egg",
                    () -> new SpawnEggItem(
                            CALLING_GHOST.get(),
                            0x191919,
                            0xB44AFF,
                            new Item.Properties()
                    )
            );
}
