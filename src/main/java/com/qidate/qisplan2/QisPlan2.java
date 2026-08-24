package com.qidate.qisplan2;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;
import com.qidate.qisplan2.block.*;
import com.qidate.qisplan2.block.entity.GhostManorMarkerBlockEntity;
import com.qidate.qisplan2.block.entity.GhostPianoBlockEntity;
import com.qidate.qisplan2.block.entity.GhostStoveBlockEntity;
import com.qidate.qisplan2.client.*;
import com.qidate.qisplan2.core.QisConfig;
import com.qidate.qisplan2.death.SupernaturalEntity;
import com.qidate.qisplan2.entity.InvisibleGhost;
import com.qidate.qisplan2.entity.NightWanderer;
import com.qidate.qisplan2.event.GhostShroudHandler;
import com.qidate.qisplan2.event.GhostUmbrellaAttackHandler;
import com.qidate.qisplan2.event.PossessionHudOverlay;
import com.qidate.qisplan2.ghost.PossessedGhostState;
import com.qidate.qisplan2.ghost.PossessionHandler;
import com.qidate.qisplan2.item.*;
import com.qidate.qisplan2.menu.GhostStoveMenu;
import com.qidate.qisplan2.recipe.GhostStoveRecipe;
import com.qidate.qisplan2.recipe.GhostStoveRecipeSerializer;
import com.qidate.qisplan2.structure.GhostManorGenerationManager;
import com.qidate.qisplan2.structure.StructureSplitter;
//import com.qidate.qisplan2.util.StructureUtil;
import net.minecraft.Util;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import com.mojang.brigadier.arguments.DoubleArgumentType;
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
import net.minecraft.core.registries.Registries;

import java.util.*;
import java.util.function.Supplier;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(QisPlan2.MODID)
public class QisPlan2 {
    public static final String MODID = "qisplan2";
    public static final Logger LOGGER = LogUtils.getLogger();

    // 附件类型注册表
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(
                    Registries.ENTITY_TYPE,
                    MODID
            );
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(
                    Registries.BLOCK_ENTITY_TYPE,
                    MODID
            );
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(
                    BuiltInRegistries.ARMOR_MATERIAL,
                    MODID
            );
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(
                    BuiltInRegistries.SOUND_EVENT,
                    MODID
            );
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(
                    BuiltInRegistries.MENU,
                    MODID
            );
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(
                    Registries.RECIPE_TYPE,
                    MODID
            );
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(
                    Registries.RECIPE_SERIALIZER,
                    MODID
            );
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    MODID
            );
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(
                    BuiltInRegistries.PARTICLE_TYPE,
                    MODID
            );
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);

    /**
     * 玩家当前驾驭的鬼及其状态。
     *
     * Key：
     *     鬼的 ResourceLocation
     *
     * Value：
     *     该鬼的复苏值、上次使用时间等状态
     *
     * 因此玩家可以同时驾驭多只鬼。
     */
    public static final DeferredHolder<
            AttachmentType<?>,
            AttachmentType<Map<ResourceLocation, PossessedGhostState>>
            > POSSESSED_GHOSTS =
            ATTACHMENT_TYPES.register(
                    "possessed_ghosts",
                    () -> AttachmentType
                            .<Map<ResourceLocation, PossessedGhostState>>builder(
                                    (java.util.function.Supplier<
                                            Map<ResourceLocation, PossessedGhostState>
                                            >)
                                            HashMap::new
                            )
                            .serialize(
                                    Codec.unboundedMap(
                                            ResourceLocation.CODEC,
                                            PossessedGhostState.CODEC
                                    )
                            )
                            .sync(
                                    ByteBufCodecs.map(
                                            HashMap::new,
                                            ResourceLocation.STREAM_CODEC,
                                            PossessedGhostState.STREAM_CODEC,
                                            32
                                    )
                            )
                            .build()
            );

    // 鬼庄园生成占位符方块
    public static final DeferredHolder<Block, GhostManorMarkerBlock>
            GHOST_MANOR_MARKER =
            BLOCKS.register(
                    "ghost_manor_marker",
                    () -> new GhostManorMarkerBlock(
                            BlockBehaviour.Properties.of()
                                    .noLootTable()
                                    .noOcclusion()
                                    .strength(-1.0F, 3600000.0F)
                    )
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<GhostManorMarkerBlockEntity>
            > GHOST_MANOR_MARKER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "ghost_manor_marker",
                    () -> BlockEntityType.Builder.of(
                            GhostManorMarkerBlockEntity::new,
                            GHOST_MANOR_MARKER.get()
                    ).build(null)
            );

    // 必死诅咒层数（0~10，同步到客户端供骷髅条显示）
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> DEATH_CURSE_COUNT =
            ATTACHMENT_TYPES.register(
                    "death_curse_count",
                    () -> AttachmentType.builder(() -> 0)
                            .serialize(Codec.INT)        // 存档用
                            .sync(ByteBufCodecs.VAR_INT) // 同步到客户端用
                            .build()
            );

    public static final DeferredItem<DeathCurseSword> DEATH_CURSE_SWORD =
            ITEMS.register(
                    "death_curse_sword",
                    () -> new DeathCurseSword(new Item.Properties())
            );

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

    // 鬼地毯
    public static final DeferredBlock<GhostCarpetBlock> GHOST_CARPET =
            BLOCKS.registerBlock(
                    "ghost_carpet",
                    GhostCarpetBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .destroyTime(0.1F)
                            .explosionResistance(0.1F)
                            .sound(SoundType.WOOL)
                            .noCollission()
                            .instabreak()
                            .pushReaction(PushReaction.BLOCK)
            );

    public static final DeferredItem<BlockItem> GHOST_CARPET_ITEM =
            ITEMS.registerSimpleBlockItem(GHOST_CARPET);

    // 鬼石砖
    public static final DeferredBlock<Block> GHOST_STONE_BRICKS =
            BLOCKS.register(
                    "ghost_stone_bricks",
                    () -> new Block(
                            BlockBehaviour.Properties.of()
                                    .strength(1.5F, 6.0F)
                                    .requiresCorrectToolForDrops()
                    )
            );

    public static final DeferredItem<BlockItem> GHOST_STONE_BRICKS_ITEM =
            ITEMS.registerSimpleBlockItem(GHOST_STONE_BRICKS);


    // 鬼灶台
    public static final DeferredBlock<Block> GHOST_STOVE =
            BLOCKS.register(
                    "ghost_stove",
                    () -> new GhostStoveBlock(
                            BlockBehaviour.Properties.of()
                                    .strength(1.5F, 6.0F)
                    )
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<GhostStoveBlockEntity>
            > GHOST_STOVE_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "ghost_stove",
                    () -> BlockEntityType.Builder.of(
                            GhostStoveBlockEntity::new,
                            GHOST_STOVE.get()
                    ).build(null)
            );

    public static final DeferredItem<BlockItem> GHOST_STOVE_ITEM =
            ITEMS.registerSimpleBlockItem(GHOST_STOVE);

    // 鬼灶台 GUI
    public static final DeferredHolder<
            MenuType<?>,
            MenuType<GhostStoveMenu>
            > GHOST_STOVE_MENU =
            MENUS.register(
                    "ghost_stove",
                    () -> new MenuType<>(
                            GhostStoveMenu::new,
                            FeatureFlags.DEFAULT_FLAGS
                    )
            );

    // 鬼门
    public static final DeferredBlock<Block> GHOST_DOOR =
            BLOCKS.register(
                    "ghost_door",
                    () -> new GhostDoorBlock(
                            BlockSetType.OAK,
                            BlockBehaviour.Properties.of()
                                    .strength(-1.0F, 3600000.0F)
                                    .noOcclusion()
                    )
            );

    public static final DeferredItem<BlockItem> GHOST_DOOR_ITEM =
            ITEMS.registerSimpleBlockItem(GHOST_DOOR);

    // 鬼草丛
    public static final DeferredBlock<GhostGrassBlock> GHOST_GRASS =
            BLOCKS.registerBlock(
                    "ghost_grass",
                    GhostGrassBlock::new,
                    BlockBehaviour.Properties.of()
                            .noCollission()
                            .noOcclusion()
                            .instabreak()
                            .sound(SoundType.GRASS)
            );

    public static final DeferredItem<BlockItem> GHOST_GRASS_ITEM =
            ITEMS.registerSimpleBlockItem(GHOST_GRASS);

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

    // 鬼钢琴
    public static final DeferredHolder<Block, GhostPianoBlock> GHOST_PIANO_BLOCK =
            BLOCKS.register(
                    "ghost_piano",
                    () -> new GhostPianoBlock(
                            BlockBehaviour.Properties.of()
                                    .strength(2.0F)
                                    .noOcclusion()
                    )
            );

    public static final DeferredItem<BlockItem> GHOST_PIANO =
            ITEMS.register(
                    "ghost_piano",
                    () -> new BlockItem(
                            GHOST_PIANO_BLOCK.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<GhostPianoBlockEntity>
            > GHOST_PIANO_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "ghost_piano",
                    () -> BlockEntityType.Builder.of(
                            GhostPianoBlockEntity::new,
                            GHOST_PIANO_BLOCK.get()
                    ).build(null)
            );

    // 鬼钢琴音乐
    public static final DeferredHolder<SoundEvent, SoundEvent> GHOST_PIANO_MUSIC =
            SOUND_EVENTS.register(
                    "ghost_piano_music",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(
                                    MODID,
                                    "ghost_piano_music"
                            )
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

    // 鬼黑雨
    public static final DeferredHolder<
            ParticleType<?>,
            SimpleParticleType
            > BLACK_RAIN =
            PARTICLE_TYPES.register(
                    "black_rain",
                    () -> new SimpleParticleType(false)
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

    public static final DeferredItem<SpawnEggItem> NINVISIBLE_GHOST_SPAWN_EGG =
            ITEMS.register(
                    "invisible_ghost_spawn_egg",
                    () -> new SpawnEggItem(
                            INVISIBLE_GHOST.get(),
                            0x191919, // 基础颜色
                            0x6B6B6B, // 斑点颜色
                            new Item.Properties()
                    )
            );


    // 创造物品栏
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

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
                        output.accept(NIGHT_WANDERER_SPAWN_EGG);
                        output.accept(NINVISIBLE_GHOST_SPAWN_EGG);
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

    // ========================================
    // 鬼灶台 Recipe 注册
    // ========================================
    public static final Supplier<RecipeType<GhostStoveRecipe>>
            GHOST_STOVE_RECIPE_TYPE =
            RECIPE_TYPES.register(
                    "ghost_stove",
                    () -> RecipeType.simple(
                            ResourceLocation.fromNamespaceAndPath(
                                    MODID,
                                    "ghost_stove"
                            )
                    )
            );


    public static final Supplier<RecipeSerializer<GhostStoveRecipe>>
            GHOST_STOVE_RECIPE_SERIALIZER =
            RECIPE_SERIALIZERS.register(
                    "ghost_stove",
                    GhostStoveRecipeSerializer::new
            );



    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public QisPlan2(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, QisConfig.CLIENT_SPEC);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ATTACHMENT_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        ARMOR_MATERIALS.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);
        MENUS.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        DATA_COMPONENTS.register(modEventBus);
        PARTICLE_TYPES.register(modEventBus);

        // 实体属性注册
        modEventBus.addListener(this::onEntityAttributeCreation);

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
    }

    public static void registerMenuScreens(
            RegisterMenuScreensEvent event
    ) {
        event.register(
                GHOST_STOVE_MENU.get(),
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

    private void commonSetup(FMLCommonSetupEvent event) {

    }

    // 声明游戏规则 Key

    /**
     * 灵异攻击是否强制抹杀玩家
     */
    public static final GameRules.Key<GameRules.BooleanValue> GHOST_DAMAGE_INSTANTLY_KILL =
            GameRules.register("ghostDamageInstantlyKill", GameRules.Category.MISC, GameRules.BooleanValue.create(true));

    /**
     * 启用/禁用 许愿鬼
     */
    public static final GameRules.Key<GameRules.BooleanValue> ISAY_ENABLED =
            GameRules.register("isayEnabled", GameRules.Category.MISC, GameRules.BooleanValue.create(true));

    /**
     * 鬼地毯灵异叠加花费时间
     */
    public static final GameRules.Key<GameRules.IntegerValue> GHOST_CARPET_KILL_TIME =
            GameRules.register("ghostCarpetKillTime", GameRules.Category.MISC, GameRules.IntegerValue.create(300));

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {

        event.getDispatcher().register(
                Commands.literal("qis_kill_ghosts")
                        .requires(source -> source.hasPermission(2))

                        .executes(context -> {

                            var source = context.getSource();

                            if (source.getEntity() == null) {
                                source.sendFailure(
                                        Component.literal(
                                                "这个命令必须由实体执行。"
                                        )
                                );

                                return 0;
                            }

                            var entity = source.getEntity();

                            AABB area =
                                    entity.getBoundingBox()
                                            .inflate(32.0D);

                            var ghosts =
                                    entity.level()
                                            .getEntitiesOfClass(
                                                    LivingEntity.class,
                                                    area,
                                                    target ->
                                                            target instanceof SupernaturalEntity
                                            );

                            int count = 0;

                            for (LivingEntity ghost : ghosts) {
                                ghost.discard();
                                count++;
                            }

                            final int finalCount = count;

                            source.sendSuccess(
                                    () -> Component.literal(
                                            "已清除附近 "
                                                    + finalCount
                                                    + " 个灵异实体。"
                                    ),
                                    true
                            );

                            return count;
                        })

                        .then(
                                Commands.argument(
                                                "radius",
                                                DoubleArgumentType.doubleArg(
                                                        1.0D,
                                                        256.0D
                                                )
                                        )
                                        .executes(context -> {

                                            var source =
                                                    context.getSource();

                                            if (source.getEntity() == null) {
                                                source.sendFailure(
                                                        Component.literal(
                                                                "这个命令必须由实体执行。"
                                                        )
                                                );

                                                return 0;
                                            }

                                            var entity =
                                                    source.getEntity();

                                            double radius =
                                                    DoubleArgumentType.getDouble(
                                                            context,
                                                            "radius"
                                                    );

                                            AABB area =
                                                    entity.getBoundingBox()
                                                            .inflate(radius);

                                            var ghosts =
                                                    entity.level()
                                                            .getEntitiesOfClass(
                                                                    LivingEntity.class,
                                                                    area,
                                                                    target ->
                                                                            target instanceof SupernaturalEntity
                                                            );

                                            int count = 0;

                                            for (LivingEntity ghost : ghosts) {
                                                ghost.discard();
                                                count++;
                                            }

                                            final int finalCount = count;

                                            source.sendSuccess(
                                                    () -> Component.literal(
                                                            "已清除 "
                                                                    + radius
                                                                    + " 格内的 "
                                                                    + finalCount
                                                                    + " 个灵异实体。"
                                                    ),
                                                    true
                                            );

                                            return count;
                                        })
                        )
        );

        event.getDispatcher().register(
                Commands.literal("qis_possess")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.literal("night_wanderer")
                                        .executes(context -> {

                                            ServerPlayer player =
                                                    context.getSource()
                                                            .getPlayerOrException();

                                            boolean success =
                                                    PossessionHandler.possess(
                                                            player,
                                                            PossessionHandler.NIGHT_WANDERER
                                                    );

                                            if (success) {

                                                context.getSource()
                                                        .sendSuccess(
                                                                () -> Component.literal(
                                                                        "成功驾驭夜游鬼。"
                                                                ),
                                                                true
                                                        );

                                                return 1;
                                            }

                                            context.getSource()
                                                    .sendFailure(
                                                            Component.literal(
                                                                    "你已经驾驭了夜游鬼。"
                                                            )
                                                    );

                                            return 0;
                                        })
                        )
        );

        event.getDispatcher().register(
                Commands.literal("qis_release")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.literal("night_wanderer")
                                        .executes(context -> {

                                            ServerPlayer player =
                                                    context.getSource()
                                                            .getPlayerOrException();

                                            boolean success =
                                                    PossessionHandler.release(
                                                            player,
                                                            PossessionHandler.NIGHT_WANDERER
                                                    );

                                            if (success) {

                                                context.getSource()
                                                        .sendSuccess(
                                                                () -> Component.literal(
                                                                        "已解除夜游鬼驾驭。"
                                                                ),
                                                                true
                                                        );

                                                return 1;
                                            }

                                            context.getSource()
                                                    .sendFailure(
                                                            Component.literal(
                                                                    "你没有驾驭夜游鬼。"
                                                            )
                                                    );

                                            return 0;
                                        })
                        )
        );

        event.getDispatcher().register(
                Commands.literal("qis_possessed")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {

                            ServerPlayer player =
                                    context.getSource()
                                            .getPlayerOrException();

                            Map<ResourceLocation, PossessedGhostState> ghosts =
                                    player.getData(
                                            QisPlan2.POSSESSED_GHOSTS
                                    );

                            if (ghosts.isEmpty()) {

                                context.getSource()
                                        .sendSuccess(
                                                () -> Component.literal(
                                                        "当前没有驾驭任何鬼。"
                                                ),
                                                false
                                        );

                                return 0;
                            }

                            StringBuilder message =
                                    new StringBuilder("当前驾驭：");

                            for (var entry : ghosts.entrySet()) {

                                ResourceLocation ghost =
                                        entry.getKey();

                                PossessedGhostState state =
                                        entry.getValue();

                                message.append("\n")
                                        .append("§e")
                                        .append(ghost)
                                        .append(" §f- 复苏值：")
                                        .append(
                                                String.format(
                                                        "%.1f%%",
                                                        state.revival() * 100.0D
                                                )
                                        );
                            }

                            String result =
                                    message.toString();

                            context.getSource()
                                    .sendSuccess(
                                            () -> Component.literal(result),
                                            false
                                    );

                            return ghosts.size();
                        })
        );


        event.getDispatcher().register(
                Commands.literal("qis_ghost_stun")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.literal("night_wanderer")
                                        .then(
                                                Commands.argument(
                                                                "seconds",
                                                                IntegerArgumentType.integer(
                                                                        1,
                                                                        3600
                                                                )
                                                        )
                                                        .executes(context -> {

                                                            ServerPlayer player =
                                                                    context.getSource()
                                                                            .getPlayerOrException();

                                                            int seconds =
                                                                    IntegerArgumentType.getInteger(
                                                                            context,
                                                                            "seconds"
                                                                    );

                                                            boolean success =
                                                                    PossessionHandler.testStun(
                                                                            player,
                                                                            PossessionHandler.NIGHT_WANDERER,
                                                                            seconds * 20L
                                                                    );

                                                            if (!success) {
                                                                context.getSource()
                                                                        .sendFailure(
                                                                                Component.literal(
                                                                                        "你没有驾驭夜游鬼。"
                                                                                )
                                                                        );

                                                                return 0;
                                                            }

                                                            context.getSource()
                                                                    .sendSuccess(
                                                                            () -> Component.literal(
                                                                                    "夜游鬼已进入普通死机 "
                                                                                            + seconds
                                                                                            + " 秒。"
                                                                            ),
                                                                            true
                                                                    );

                                                            return 1;
                                                        })
                                        )
                        )
        );

        event.getDispatcher().register(
                Commands.literal("qis_ghost_permanent_stun")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.literal("night_wanderer")
                                        .executes(context -> {

                                            ServerPlayer player =
                                                    context.getSource()
                                                            .getPlayerOrException();

                                            boolean success =
                                                    PossessionHandler.testPermanentStun(
                                                            player,
                                                            PossessionHandler.NIGHT_WANDERER
                                                    );

                                            if (!success) {
                                                context.getSource()
                                                        .sendFailure(
                                                                Component.literal(
                                                                        "你没有驾驭夜游鬼。"
                                                                )
                                                        );

                                                return 0;
                                            }

                                            context.getSource()
                                                    .sendSuccess(
                                                            () -> Component.literal(
                                                                    "夜游鬼已进入永久死机。"
                                                            ),
                                                            true
                                                    );

                                            return 1;
                                        })
                        )
        );

        event.getDispatcher().register(
                Commands.literal("qis_split_structure")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.argument(
                                                "structure",
                                                ResourceLocationArgument.id()
                                        )
                                        .executes(context -> {

                                            var source =
                                                    context.getSource();

                                            ResourceLocation sourceId =
                                                    ResourceLocationArgument.getId(
                                                            context,
                                                            "structure"
                                                    );

                                            try {

                                                int count =
                                                        StructureSplitter.split(
                                                                source.getServer(),
                                                                sourceId,
                                                                ResourceLocation.fromNamespaceAndPath(
                                                                        sourceId.getNamespace(),
                                                                        sourceId.getPath()
                                                                                + "_parts"
                                                                )
                                                        );

                                                source.sendSuccess(
                                                        () -> Component.literal(
                                                                "结构拆分完成："
                                                                        + sourceId
                                                                        + "\n"
                                                                        + "共生成 "
                                                                        + count
                                                                        + " 个区块结构。"
                                                        ),
                                                        true
                                                );

                                                return count;

                                            } catch (Exception e) {

                                                QisPlan2.LOGGER.error(
                                                        "拆分结构失败："
                                                                + sourceId,
                                                        e
                                                );

                                                source.sendFailure(
                                                        Component.literal(
                                                                "结构拆分失败："
                                                                        + e.getMessage()
                                                        )
                                                );

                                                return 0;
                                            }
                                        })
                        )
        );

        event.getDispatcher().register(
                Commands.literal("qis_generate_ghost_manor")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {

                            ServerPlayer player =
                                    context.getSource()
                                            .getPlayerOrException();

                            boolean success =
                                    GhostManorGenerationManager.start(
                                            player.serverLevel(),
                                            player.blockPosition()
                                    );

                            if (!success) {

                                context.getSource()
                                        .sendFailure(
                                                Component.literal(
                                                        "现在已经有一个鬼庄园正在生成。"
                                                )
                                        );

                                return 0;
                            }

                            context.getSource()
                                    .sendSuccess(
                                            () -> Component.literal(
                                                    "已开始生成鬼庄园。"
                                            ),
                                            true
                                    );

                            return 1;
                        })
        );
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
        LOGGER.info("Isay game rule registered: {}", ISAY_ENABLED.getId());
    }
}
