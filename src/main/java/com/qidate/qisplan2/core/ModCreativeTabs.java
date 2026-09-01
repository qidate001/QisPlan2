package com.qidate.qisplan2.core;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;

import static com.qidate.qisplan2.core.ModBlocks.*;
import static com.qidate.qisplan2.core.ModBlocks.GHOST_DOOR_ITEM;
import static com.qidate.qisplan2.core.ModBlocks.GHOST_GRASS_ITEM;
import static com.qidate.qisplan2.core.ModBlocks.GHOST_LEATHER_BOX_ITEM;
import static com.qidate.qisplan2.core.ModBlocks.GHOST_LEATHER_WALL_ITEM;
import static com.qidate.qisplan2.core.ModBlocks.GHOST_PIANO;
import static com.qidate.qisplan2.core.ModItems.*;
import static com.qidate.qisplan2.core.ModItems.INCENSE_ASH;
import static com.qidate.qisplan2.core.ModRegistries.CREATIVE_MODE_TABS;

public class ModCreativeTabs {

    private ModCreativeTabs() {}

    public static void init() {
        /*
         * 故意留空。
         *
         * 调用这个方法本身，就会强制 JVM
         * 在正确的时机完成 ModCreativeTabs 的静态初始化。
         */
    }

    // 创造物品栏（齐计划2：鬼）
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> QIS_PLAN_GHOST_TAB =
            CREATIVE_MODE_TABS.register("qis_plan_ghost", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.qisplan2.qis_plan_ghost"))
                    .icon(() -> GHOST_CARPET_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(DEATH_CURSE_SWORD);
                        output.accept(GHOST_CARPET_ITEM);
                        output.accept(GHOST_STONE_BRICKS_ITEM);
                        output.accept(GHOST_STOVE_ITEM);
                        output.accept(GHOST_DOOR_ITEM);
                        output.accept(GHOST_GRASS_ITEM);
                        output.accept(GHOST_SHROUD);
                        output.accept(GHOST_BOOK);
                        output.accept(GHOST_PIANO);
                        output.accept(GHOST_WHITE_PORRIDGE);
                        output.accept(GHOST_UMBRELLA);
                        output.accept(GHOST_LEATHER_BOX_ITEM);
                        output.accept(GHOST_LEATHER_WALL_ITEM);
                        output.accept(COFFIN_NAIL);
                        output.accept(GHOST_DOOR_PLATE_ITEM);
                        output.accept(GHOST_PAINTING);
                        output.accept(NIGHT_WANDERER_SPAWN_EGG);
                        output.accept(INVISIBLE_GHOST_SPAWN_EGG);
                        output.accept(KNOCKING_GHOST_SPAWN_EGG);
                        output.accept(OPENING_GHOST_SPAWN_EGG);
                        output.accept(CLOSING_GHOST_SPAWN_EGG);
                        output.accept(CALLING_GHOST_SPAWN_EGG);
                    })
                    .build()
            );

    // 创造物品栏（齐计划2：灵异材料）
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> QIS_PLAN_GHOST_ITEM_TAB =
            CREATIVE_MODE_TABS.register("qis_plan_ghost_items", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.qisplan2.qis_plan_ghost_items"))
                    .icon(() -> GHOST_COIN.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(GHOST_COIN);
                        output.accept(GHOST_STONE_FINGER);
                        output.accept(INCENSE_ASH);
                    })
                    .build()
            );
}
