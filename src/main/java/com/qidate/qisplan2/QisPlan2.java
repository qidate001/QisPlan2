package com.qidate.qisplan2;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.Codec;
import com.qidate.qisplan2.block.GhostCarpetBlock;
import com.qidate.qisplan2.block.GhostDoorBlock;
import com.qidate.qisplan2.block.GhostStoveBlock;
import com.qidate.qisplan2.core.QisConfig;
import com.qidate.qisplan2.item.DeathCurseSword;
import com.qidate.qisplan2.item.GhostCoin;
import com.qidate.qisplan2.util.StructureDebug;
import com.qidate.qisplan2.util.StructureUtil;
import com.qidate.qisplan2.worldgen.GhostTempleGeneration;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(QisPlan2.MODID)
public class QisPlan2 {
    public static final String MODID = "qisplan2";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

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

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so attachment types get registered
        ATTACHMENT_TYPES.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

//        STRUCTURE_TYPES.register(modEventBus);

//        STRUCTURE_PIECE_TYPES.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

//        NeoForge.EVENT_BUS.register(
//                GhostTempleGeneration.class
//        );


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

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
        LOGGER.info("Isay game rule registered: {}", ISAY_ENABLED.getId());
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {

        event.getDispatcher().register(
                Commands.literal("qisplan2_test")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {

                            ServerPlayer player =
                                    context.getSource()
                                            .getPlayerOrException();

                            boolean success =
                                    StructureUtil.placeStructure(
                                            player.serverLevel(),
                                            player.blockPosition().above(),
                                            "qisplan2:ghost_temple"
                                    );

                            if (success) {

                                context.getSource().sendSuccess(
                                        () -> Component.literal(
                                                "鬼庙生成成功！"
                                        ),
                                        true
                                );

                            } else {

                                context.getSource().sendFailure(
                                        Component.literal(
                                                "鬼庙生成失败，请查看控制台。"
                                        )
                                );
                            }

                            return success ? 1 : 0;
                        })
        );
    }

    @SubscribeEvent
    public void onRegisterCommands(
            net.neoforged.neoforge.event.RegisterCommandsEvent event
    ) {
        event.getDispatcher().register(
                net.minecraft.commands.Commands.literal("qis_debug_structure")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {

                            var player =
                                    context.getSource().getPlayerOrException();

                            StructureDebug.test(
                                    player.serverLevel(),
                                    player.blockPosition()
                            );

                            context.getSource().sendSuccess(
                                    () -> Component.literal(
                                            "结构调试信息已输出到控制台"
                                    ),
                                    false
                            );

                            return 1;
                        })
        );
    }
}
