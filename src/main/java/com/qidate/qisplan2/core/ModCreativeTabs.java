package com.qidate.qisplan2.core;

import com.qidate.qisplan2.entity.painting.GhostPaintingVariants;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostModuleDataModifier;
import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemData;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemModuleHost;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

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
                        output.accept(GHOST_BLANK_SWORD);
                        output.accept(DEATH_CURSE_SWORD);
                        output.accept(GHOST_CARPET_ITEM);
                        output.accept(GHOST_STONE_BRICKS_ITEM);
                        output.accept(GHOST_GRAVE_SOIL_ITEM);
                        output.accept(GHOST_STOVE_ITEM);
                        output.accept(GHOST_DOOR_ITEM);
                        output.accept(GHOST_DOOR_PLATE_ITEM);
                        output.accept(GHOST_GRASS_ITEM);
                        output.accept(GHOST_SHROUD);
                        output.accept(GHOST_BOOK);
                        output.accept(GHOST_PIANO);
                        output.accept(GHOST_WHITE_PORRIDGE);
                        output.accept(GHOST_UMBRELLA);
                        output.accept(GHOST_LEATHER_BOX_ITEM);
                        output.accept(GHOST_LEATHER_WALL_ITEM);
                        output.accept(COFFIN_NAIL);
                        output.accept(COFFIN_NAIL_PICKAXE);
                        output.accept(GHOST_DIVINATION_SLIP);
                        output.accept(WHITE_GHOST_CANDLE);
                        output.accept(RED_GHOST_CANDLE);
                        output.accept(GHOST_TOMBSTONE_ITEM);

                        // 鬼画
                        output.accept(GHOST_PAINTING);

                        ItemStack orokin =
                                new ItemStack(GHOST_PAINTING.get());

                        CompoundTag tag =
                                new CompoundTag();

                        tag.putString(
                                "PaintingVariant",
                                GhostPaintingVariants.OROKIN.toString()
                        );

                        orokin.set(
                                DataComponents.CUSTOM_DATA,
                                CustomData.of(tag)
                        );

                        output.accept(orokin);
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
                        output.accept(GOLDEN_ROD);
                    })
                    .build()
            );

    // 创造物品栏（齐计划2：实体厉鬼）
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> QIS_PLAN_GHOST_ENTITIES_TAB =
            CREATIVE_MODE_TABS.register("qis_plan_ghost_entities", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.qisplan2.qis_plan_ghost_entities"))
                    .icon(() -> MODULAR_GHOST_SPAWN_EGG.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        // 通用模块化厉鬼刷怪蛋：不预设模块。
                        output.accept(MODULAR_GHOST_SPAWN_EGG);

                        // 为每个已注册模块生成一个带名称和描述的刷怪蛋。
                        for (GhostModule module : GhostModuleRegistry.getAll()) {
                            ItemStack stack = MODULAR_GHOST_SPAWN_EGG.get()
                                    .getDefaultInstance();

                            // 保存预设模块 ID，保留现有刷怪蛋的识别逻辑。
                            CompoundTag tag = new CompoundTag();
                            tag.putString("GhostModule", module.getId().toString());

                            stack.set(
                                    DataComponents.CUSTOM_DATA,
                                    CustomData.of(tag)
                            );

                            // 同时保存真实的模块实例数据。
                            // 初始强度、复苏值和死机值均来自模块定义。
                            GhostModuleDataModifier host = new GhostItemModuleHost(stack);

                            boolean added = host.addModule(module);

                            if (!added) {
                                com.qidate.qisplan2.QisPlan2.LOGGER.warn(
                                        "无法为模块化厉鬼刷怪蛋写入模块实例数据：{}",
                                        module.getId()
                                );
                            } else {
                                com.qidate.qisplan2.QisPlan2.LOGGER.info(
                                        "已为模块化厉鬼刷怪蛋写入模块实例数据：{}，当前数据：{}",
                                        module.getId(),
                                        GhostItemData.getModules(stack)
                                );
                            }

                            // 根据模块 ID 生成对应的翻译键。
                            ResourceLocation moduleId = module.getId();
                            String moduleNameKey = "module."
                                    + moduleId.getNamespace()
                                    + "."
                                    + moduleId.getPath();

                            Component moduleName = Component.translatable(moduleNameKey);

                            // 动态物品名称
                            stack.set(
                                    DataComponents.ITEM_NAME,
                                    Component.translatable(
                                            "item.qisplan2.modular_ghost_spawn_egg.module",
                                            moduleName
                                    ).withStyle(style -> style.withColor(0xAA0000))
                            );

                            // 名称下方的描述
                            stack.set(
                                    DataComponents.LORE,
                                    new ItemLore(List.of(
                                            Component.translatable(
                                                    "item.qisplan2.modular_ghost_spawn_egg.loaded_module",
                                                    moduleName
                                            )
                                    ))
                            );

                            output.accept(stack);
                        }

                        // 其他刷怪蛋暂时保持原样。
                        output.accept(NIGHT_WANDERER_SPAWN_EGG);
                        output.accept(KNOCKING_GHOST_SPAWN_EGG);
                        output.accept(OPENING_GHOST_SPAWN_EGG);
                        output.accept(CLOSING_GHOST_SPAWN_EGG);
                        output.accept(CALLING_GHOST_SPAWN_EGG);
                        output.accept(GHOST_MIST_SPAWN_EGG);
                    })
                    .build()
            );
}
