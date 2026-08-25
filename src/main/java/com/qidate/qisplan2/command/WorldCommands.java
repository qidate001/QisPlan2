package com.qidate.qisplan2.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.death.SupernaturalEntity;
import com.qidate.qisplan2.structure.GhostManorGenerationManager;
import com.qidate.qisplan2.structure.StructureSplitter;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public final class WorldCommands {

    private WorldCommands() {
    }

    /**
     * 注册：
     *
     * /qisplan2 kill
     * /qisplan2 split_structure
     * /qisplan2 generate_ghost_manor
     */
    public static void register(
            LiteralArgumentBuilder<CommandSourceStack> root
    ) {

        /*
         * ========================================================
         * /qisplan2 kill
         *
         * 默认 32 格
         * ========================================================
         */

        root.then(
                Commands.literal("kill")
                        .executes(
                                WorldCommands::killDefault
                        )
                        .then(
                                Commands.argument(
                                                "radius",
                                                DoubleArgumentType.doubleArg(
                                                        1.0D,
                                                        256.0D
                                                )
                                        )
                                        .executes(
                                                WorldCommands::killWithRadius
                                        )
                        )
        );


        /*
         * ========================================================
         * /qisplan2 split_structure <structure>
         * ========================================================
         */

        root.then(
                Commands.literal("split_structure")
                        .then(
                                Commands.argument(
                                                "structure",
                                                ResourceLocationArgument.id()
                                        )
                                        .executes(
                                                WorldCommands::splitStructure
                                        )
                        )
        );


        /*
         * ========================================================
         * /qisplan2 generate_ghost_manor
         * ========================================================
         */

        root.then(
                Commands.literal("generate_ghost_manor")
                        .executes(
                                WorldCommands::generateGhostManor
                        )
        );
    }

    /*
     * ============================================================
     * 清除灵异实体
     * ============================================================
     */

    private static int killDefault(
            CommandContext<CommandSourceStack> context
    ) {
        return killGhosts(
                context,
                32.0D
        );
    }

    private static int killWithRadius(
            CommandContext<CommandSourceStack> context
    ) {

        double radius =
                DoubleArgumentType.getDouble(
                        context,
                        "radius"
                );

        return killGhosts(
                context,
                radius
        );
    }

    private static int killGhosts(
            CommandContext<CommandSourceStack> context,
            double radius
    ) {

        CommandSourceStack source =
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
                        radius == 32.0D
                                ? "已清除附近 "
                                + finalCount
                                + " 个灵异实体。"
                                : "已清除 "
                                + radius
                                + " 格内的 "
                                + finalCount
                                + " 个灵异实体。"
                ),
                true
        );

        return count;
    }


    /*
     * ============================================================
     * 结构拆分
     * ============================================================
     */

    private static int splitStructure(
            CommandContext<CommandSourceStack> context
    ) {

        CommandSourceStack source =
                context.getSource();

        ResourceLocation sourceId =
                ResourceLocationArgument.getId(
                        context,
                        "structure"
                );

        ResourceLocation outputId =
                ResourceLocation.fromNamespaceAndPath(
                        sourceId.getNamespace(),
                        sourceId.getPath()
                                + "_parts"
                );

        try {

            int count =
                    StructureSplitter.split(
                            source.getServer(),
                            sourceId,
                            outputId
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
    }


    /*
     * ============================================================
     * 生成鬼庄园
     * ============================================================
     */

    private static int generateGhostManor(
            CommandContext<CommandSourceStack> context
    ) {

        CommandSourceStack source =
                context.getSource();

        if (!(source.getEntity()
                instanceof net.minecraft.server.level.ServerPlayer player)) {

            source.sendFailure(
                    Component.literal(
                            "这个命令必须由玩家执行。"
                    )
            );

            return 0;
        }

        boolean success =
                GhostManorGenerationManager.start(
                        player.serverLevel(),
                        player.blockPosition()
                );

        if (!success) {

            source.sendFailure(
                    Component.literal(
                            "现在已经有一个鬼庄园正在生成。"
                    )
            );

            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "已开始生成鬼庄园。"
                ),
                true
        );

        return 1;
    }
}