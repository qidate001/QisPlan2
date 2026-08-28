package com.qidate.qisplan2;

import com.mojang.serialization.Codec;
import com.qidate.qisplan2.block.*;
import com.qidate.qisplan2.block.entity.*;
import com.qidate.qisplan2.client.*;
import com.qidate.qisplan2.core.*;
import com.qidate.qisplan2.entity.*;
import com.qidate.qisplan2.event.GhostUmbrellaAttackHandler;
import com.qidate.qisplan2.event.PossessionHudOverlay;
import com.qidate.qisplan2.ghost.GhostAbilityInteractionHandler;
import com.qidate.qisplan2.ghost.PossessedGhostState;
import com.qidate.qisplan2.ghost.ability.GhostAbilityRegistry;
import com.qidate.qisplan2.ghost.partition.PartitionReturnData;
import com.qidate.qisplan2.item.*;
import com.qidate.qisplan2.menu.GhostStoveMenu;
import com.qidate.qisplan2.recipe.GhostStoveRecipe;
import com.qidate.qisplan2.recipe.GhostStoveRecipeSerializer;
import net.minecraft.Util;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.*;
import java.util.function.Supplier;

import static com.qidate.qisplan2.core.ModEntities.*;
import static com.qidate.qisplan2.core.ModRegistries.*;
import static com.qidate.qisplan2.core.ModItems.*;
import static com.qidate.qisplan2.core.ModBlocks.*;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(QisPlan2.MODID)
public class QisPlan2 {
    public static final String MODID = "qisplan2";
    public static final Logger LOGGER = LogUtils.getLogger();




















    // 创造物品栏（齐计划2：其他）
//    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> QIS_PLAN_ELSE_TAB =
//            CREATIVE_MODE_TABS.register("qis_plan_else", () -> CreativeModeTab.builder()
//                    .title(Component.translatable("itemGroup.qisplan2.qis_plan_else"))
//                    .icon(() -> GHOST_LEATHER_BOX_ITEM.get().getDefaultInstance())
//                    .displayItems((parameters, output) -> {
//                        output.accept(PARTITION_EXIT_ITEM);
//                    })
//                    .build()
//            );












    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public QisPlan2(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, QisConfig.CLIENT_SPEC);

        ModRegistries.registerAll(modEventBus);
        ModRegistries.initAll();

        // 实体属性注册
        modEventBus.addListener(
                this::onEntityAttributeCreation
        );

        modEventBus.addListener(
                InvisibleGhostClient::registerRenderers
        );

        modEventBus.addListener(
                QisPlan2::registerMenuScreens
        );

        modEventBus.register(
                GhostUmbrellaClient.class
        );

        NeoForge.EVENT_BUS.register(
                GhostUmbrellaAttackHandler.class
        );

        // 驭鬼注册表注册
        GhostAbilityRegistry.bootstrap();

        // 驭鬼事件注册
        GhostAbilityInteractionHandler.register();

        // 驭鬼 HUD 注册
        NeoForge.EVENT_BUS.addListener(PossessionHudOverlay::render);

        // Mod Event Bus 事件
        NeoForge.EVENT_BUS.register(this);

        // 普通 NeoForge 游戏事件
        NeoForge.EVENT_BUS.register(this);
    }

    private void onEntityAttributeCreation(
            EntityAttributeCreationEvent event
    ) {
        // 夜游鬼
        event.put(
                NIGHT_WANDERER.get(),
                NightWanderer.createAttributes()
                        .build()
        );

        // 不可视之鬼
        event.put(
                INVISIBLE_GHOST.get(),
                InvisibleGhost.createAttributes()
                        .build()
        );

        // 敲门鬼
        event.put(
                KNOCKING_GHOST.get(),
                KnockingGhost.createAttributes()
                        .build()
        );

        // 开门鬼
        event.put(
                OPENING_GHOST.get(),
                OpeningGhost.createAttributes()
                        .build()
        );

        // 关门鬼
        event.put(
                CLOSING_GHOST.get(),
                ClosingGhost.createAttributes()
                        .build()
        );
    }

    public static void registerMenuScreens(
            RegisterMenuScreensEvent event
    ) {
        event.register(
                ModMenus.GHOST_STOVE_MENU.get(),
                GhostStoveScreen::new
        );
    }

    public static void registerClientItemExtensions(
            RegisterClientExtensionsEvent event
    ) {
        event.registerItem(
                new IClientItemExtensions() {

                    private final GhostUmbrellaRenderer renderer =
                            new GhostUmbrellaRenderer();

                    @Override
                    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                        return renderer;
                    }
                },
                GHOST_UMBRELLA.get()
        );
    }


    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // 什么都不干
    }
}
