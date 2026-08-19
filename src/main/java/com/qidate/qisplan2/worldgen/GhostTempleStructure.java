package com.qidate.qisplan2.worldgen;

import com.mojang.serialization.MapCodec;
import com.qidate.qisplan2.QisPlan2;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Optional;

public class GhostTempleStructure extends Structure {

    public static final MapCodec<GhostTempleStructure> CODEC =
            Structure.simpleCodec(
                    GhostTempleStructure::new
            );

    private static final ResourceLocation TEMPLATE =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "ghost_temple"
            );

    public GhostTempleStructure(
            StructureSettings settings
    ) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(
            GenerationContext context
    ) {

        System.out.println(
                "[QisPlan2][GhostTemple] findGenerationPoint called! "
                        + "chunk="
                        + context.chunkPos()
        );

        return onTopOfChunkCenter(
                context,
                Heightmap.Types.WORLD_SURFACE_WG,
                builder -> {

                    System.out.println(
                            "[QisPlan2][GhostTemple] GenerationStub created!"
                    );

                    generatePieces(
                            builder,
                            context
                    );
                }
        );
    }

    private static void generatePieces(
            StructurePiecesBuilder builder,
            GenerationContext context
    ) {

        System.out.println(
                "[QisPlan2][GhostTemple] generatePieces() called!"
        );

        StructureTemplateManager manager =
                context.structureTemplateManager();

        BlockPos center =
                context.chunkPos()
                        .getMiddleBlockPosition(0);

        System.out.println(
                "[QisPlan2][GhostTemple] center = "
                        + center
        );

        int y =
                context.chunkGenerator()
                        .getFirstFreeHeight(
                                center.getX(),
                                center.getZ(),
                                Heightmap.Types.WORLD_SURFACE_WG,
                                context.heightAccessor(),
                                context.randomState()
                        );

        BlockPos position =
                new BlockPos(
                        center.getX(),
                        y,
                        center.getZ()
                );

        System.out.println(
                "[QisPlan2][GhostTemple] position = "
                        + position
        );

        Rotation rotation =
                Rotation.getRandom(
                        context.random()
                );

        StructurePlaceSettings settings =
                new StructurePlaceSettings()
                        .setRotation(rotation);

        GhostTemplePiece piece =
                new GhostTemplePiece(
                        manager,
                        position,
                        settings
                );

        System.out.println(
                "[QisPlan2][GhostTemple] Piece created!"
        );

        System.out.println(
                "[QisPlan2][GhostTemple] Piece boundingBox = "
                        + piece.getBoundingBox()
        );

        builder.addPiece(piece);

        System.out.println(
                "[QisPlan2][GhostTemple] Piece added!"
        );
    }


    public static class GhostTemplePiece
            extends TemplateStructurePiece {

        public GhostTemplePiece(
                StructureTemplateManager manager,
                BlockPos position,
                StructurePlaceSettings settings
        ) {

            super(
                    QisPlan2.GHOST_TEMPLE_PIECE.get(),
                    0,
                    manager,
                    TEMPLATE,
                    TEMPLATE.toString(),
                    settings,
                    position
            );

            System.out.println(
                    "[QisPlan2][GhostTemple] "
                            + "Template Piece constructed: "
                            + TEMPLATE
            );

            System.out.println(
                    "[QisPlan2][GhostTemple] "
                            + "BoundingBox: "
                            + this.getBoundingBox()
            );
        }


        public GhostTemplePiece(
                net.minecraft.nbt.CompoundTag tag,
                StructureTemplateManager manager
        ) {

            super(
                    QisPlan2.GHOST_TEMPLE_PIECE.get(),
                    tag,
                    manager,
                    location ->
                            new StructurePlaceSettings()
            );
        }


        @Override
        protected void handleDataMarker(
                String name,
                BlockPos pos,
                net.minecraft.world.level.ServerLevelAccessor level,
                RandomSource random,
                net.minecraft.world.level.levelgen.structure.BoundingBox box
        ) {
        }
    }


    @Override
    public StructureType<?> type() {
        return QisPlan2.GHOST_TEMPLE_STRUCTURE_TYPE.get();
    }
}