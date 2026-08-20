package com.qidate.qisplan2;

import com.mojang.serialization.Codec;
import com.qidate.qisplan2.block.GhostCarpetBlock;
import com.qidate.qisplan2.block.GhostDoorBlock;
import com.qidate.qisplan2.block.GhostGrassBlock;
import com.qidate.qisplan2.block.GhostStoveBlock;
import com.qidate.qisplan2.core.QisConfig;
import com.qidate.qisplan2.death.SupernaturalEntity;
import com.qidate.qisplan2.entity.NightWanderer;
import com.qidate.qisplan2.event.PossessionHudOverlay;
import com.qidate.qisplan2.ghost.PossessedGhostState;
import com.qidate.qisplan2.ghost.PossessionHandler;
import com.qidate.qisplan2.item.DeathCurseSword;
import com.qidate.qisplan2.item.GhostCoin;
import com.qidate.qisplan2.util.StructureUtil;
import net.minecraft.commands.Commands;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(QisPlan2.MODID)
public class QisPlan2 {
    public static final String MODID = "qisplan2";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(
                    Registries.ENTITY_TYPE,
                    MODID
            );

    // 附件类型注册表
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);

    /**
     * 鬼庙生成检查标记
     *
     * true  = 这个候选区块已经处理过
     * false = 尚未处理
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>>
            GHOST_TEMPLE_GENERATED =
            ATTACHMENT_TYPES.register(
                    "ghost_temple_generated",
                    () -> AttachmentType.builder(() -> false)
                            .serialize(Codec.BOOL)
                            .build()
            );

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

    public static final DeferredItem<BlockItem> GHOST_STOVE_ITEM =
            ITEMS.registerSimpleBlockItem(GHOST_STOVE);

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
                        output.accept(NIGHT_WANDERER_SPAWN_EGG);
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

        // 实体属性注册
        modEventBus.addListener(this::onEntityAttributeCreation);

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
        event.put(
                NIGHT_WANDERER.get(),
                NightWanderer.createAttributes().build()
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



    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
        LOGGER.info("Isay game rule registered: {}", ISAY_ENABLED.getId());
    }
}
