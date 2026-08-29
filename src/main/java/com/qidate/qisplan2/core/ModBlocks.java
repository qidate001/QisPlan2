package com.qidate.qisplan2.core;

import com.qidate.qisplan2.block.*;
import com.qidate.qisplan2.block.entity.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import static com.qidate.qisplan2.core.ModRegistries.*;

public class ModBlocks {

    private ModBlocks() {}

    public static void init() {
        /*
         * 故意留空。
         *
         * 调用这个方法本身，就会强制 JVM
         * 在正确的时机完成 ModBlocks 的静态初始化。
         */
    }

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

    // 鬼湖生成占位符方块
    public static final DeferredHolder<
            Block,
            GhostLakeMarkerBlock
            > GHOST_LAKE_MARKER =
            BLOCKS.register(
                    "ghost_lake_marker",
                    () -> new GhostLakeMarkerBlock(
                            BlockBehaviour.Properties.of()
                                    .noLootTable()
                                    .noOcclusion()
                                    .strength(
                                            -1.0F,
                                            3600000.0F
                                    )
                    )
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<GhostLakeMarkerBlockEntity>
            > GHOST_LAKE_MARKER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "ghost_lake_marker",
                    () -> BlockEntityType.Builder.of(
                            GhostLakeMarkerBlockEntity::new,
                            GHOST_LAKE_MARKER.get()
                    ).build(null)
            );

    // 分割维度离开方块
    public static final DeferredBlock<PartitionExitBlock>
            PARTITION_EXIT =
            BLOCKS.registerBlock(
                    "partition_exit",
                    PartitionExitBlock::new,
                    BlockBehaviour.Properties
                            .of()
                            .strength(2.0F)
                            .noOcclusion()
            );

    public static final DeferredItem<BlockItem>
            PARTITION_EXIT_ITEM =
            ITEMS.registerSimpleBlockItem(
                    PARTITION_EXIT,
                    new Item.Properties()
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

    // 鬼皮箱
    public static final DeferredBlock<GhostLeatherBoxBlock>
            GHOST_LEATHER_BOX =
            BLOCKS.registerBlock(
                    "ghost_leather_box",
                    GhostLeatherBoxBlock::new,
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .of()
                            .strength(2.0F)
                            .noOcclusion()
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<GhostLeatherBoxBlockEntity>
            > GHOST_LEATHER_BOX_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "ghost_leather_box",
                    () ->
                            BlockEntityType.Builder.of(
                                    GhostLeatherBoxBlockEntity::new,
                                    GHOST_LEATHER_BOX.get()
                            ).build(null)
            );

    public static final DeferredItem<BlockItem>
            GHOST_LEATHER_BOX_ITEM =
            ITEMS.registerSimpleBlockItem(
                    GHOST_LEATHER_BOX,
                    new Item.Properties()
            );

    // 鬼皮墙
    public static final DeferredBlock<GhostLeatherWallBlock>
            GHOST_LEATHER_WALL =
            BLOCKS.registerBlock(
                    "ghost_leather_wall",
                    GhostLeatherWallBlock::new,
                    BlockBehaviour.Properties
                            .of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .strength(-1.0F, 3600000.0F)
                            .noLootTable()
                            .pushReaction(
                                    PushReaction.BLOCK
                            )
                            .sound(
                                    SoundType.STONE
                            )
            );

    public static final DeferredItem<BlockItem>
            GHOST_LEATHER_WALL_ITEM =
            ITEMS.registerSimpleBlockItem(
                    GHOST_LEATHER_WALL,
                    new Item.Properties()
            );

    // 鬼门牌
    public static final DeferredBlock<GhostDoorPlateBlock>
            GHOST_DOOR_PLATE =
            BLOCKS.registerBlock(
                    "ghost_door_plate",
                    GhostDoorPlateBlock::new,
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .of()
                            .strength(2.0F)
                            .noOcclusion()
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<GhostDoorPlateBlockEntity>
            > GHOST_DOOR_PLATE_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "ghost_door_plate",
                    () ->
                            BlockEntityType.Builder.of(
                                    GhostDoorPlateBlockEntity::new,
                                    GHOST_DOOR_PLATE.get()
                            ).build(null)
            );

    public static final DeferredItem<BlockItem>
            GHOST_DOOR_PLATE_ITEM =
            ITEMS.registerSimpleBlockItem(
                    GHOST_DOOR_PLATE,
                    new Item.Properties()
            );
}
