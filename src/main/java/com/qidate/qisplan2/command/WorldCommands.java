package com.qidate.qisplan2.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.death.SupernaturalEntity;
import com.qidate.qisplan2.ghost.domain.type.debug.DebugDomainController;
import com.qidate.qisplan2.ghost.isolation.GhostIsolationDetector;
import com.qidate.qisplan2.ghost.isolation.GhostIsolationSystem;
import com.qidate.qisplan2.structure.GhostLakeGenerationManager;
import com.qidate.qisplan2.structure.GhostManorGenerationManager;
import com.qidate.qisplan2.structure.StructureSplitter;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
         * /qisplan2 debug_domain
         * ========================================================
         */

        root.then(
                Commands.literal("debug_domain")
                        .then(
                                Commands.literal("create")
                                        .then(
                                                Commands.argument(
                                                                "radius",
                                                                DoubleArgumentType.doubleArg(
                                                                        1.0D,
                                                                        512.0D
                                                                )
                                                        )
                                                        .executes(
                                                                WorldCommands::createDebugDomain
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "layer",
                                                                                IntegerArgumentType.integer(
                                                                                        1,
                                                                                        10
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                WorldCommands::createDebugDomain
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "strength",
                                                                                                DoubleArgumentType.doubleArg(
                                                                                                        0.0D
                                                                                                )
                                                                                        )
                                                                                        .executes(
                                                                                                WorldCommands::createDebugDomain
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("remove")
                                        .executes(
                                                WorldCommands::removeDebugDomain
                                        )
                        )
        );

        /*
         * ========================================================
         * /qisplan2 debug_isolation
         *
         * 检测玩家当前位置是否被灵异隔绝
         * ========================================================
         */

        root.then(
                Commands.literal("debug_isolation")
                        .executes(
                                WorldCommands::debugIsolation
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


        /*
         * ========================================================
         * /qisplan2 generate_ghost_lake
         * ========================================================
         */

        root.then(
                Commands.literal("generate_ghost_lake")
                        .executes(
                                WorldCommands::generateGhostLake
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
                    Component.translatable(
                            "command.qisplan2.entity_only"
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
                () -> Component.translatable(
                        radius == 32.0D
                                ? "command.qisplan2.kill.default"
                                : "command.qisplan2.kill.radius",
                        finalCount,
                        radius
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
                    () -> Component.translatable(
                            "command.qisplan2.split_structure.success",
                            sourceId.toString(),
                            count
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
                    Component.translatable(
                            "command.qisplan2.split_structure.failure",
                            sourceId.toString(),
                            e.getMessage()
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
                instanceof ServerPlayer player)) {

            source.sendFailure(
                    Component.translatable(
                            "command.qisplan2.player_only"
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
                    Component.translatable(
                            "command.qisplan2.generate_ghost_manor.busy"
                    )
            );

            return 0;
        }

        source.sendSuccess(
                () -> Component.translatable(
                        "command.qisplan2.generate_ghost_manor.success"
                ),
                true
        );

        return 1;
    }


    /*
     * ============================================================
     * 生成鬼湖
     * ============================================================
     */

    private static int generateGhostLake(
            CommandContext<CommandSourceStack> context
    ) {

        CommandSourceStack source =
                context.getSource();

        if (!(source.getEntity()
                instanceof ServerPlayer player)) {

            source.sendFailure(
                    Component.translatable(
                            "command.qisplan2.player_only"
                    )
            );

            return 0;
        }

        boolean success =
                GhostLakeGenerationManager.start(
                        player.serverLevel(),
                        player.blockPosition()
                );

        if (!success) {

            source.sendFailure(
                    Component.translatable(
                            "command.qisplan2.generate_ghost_lake.busy"
                    )
            );

            return 0;
        }

        source.sendSuccess(
                () -> Component.translatable(
                        "command.qisplan2.generate_ghost_lake.success"
                ),
                true
        );

        return 1;
    }


    /*
     * ============================================================
     * 创建 Debug 鬼域
     * ============================================================
     */

    private static int createDebugDomain(
            CommandContext<CommandSourceStack> context
    ) {

        CommandSourceStack source =
                context.getSource();

        if (!(source.getEntity()
                instanceof ServerPlayer player)) {

            source.sendFailure(
                    Component.translatable(
                            "command.qisplan2.player_only"
                    )
            );

            return 0;
        }

        double radius =
                DoubleArgumentType.getDouble(
                        context,
                        "radius"
                );

        int layer = 1;

        try {

            layer =
                    IntegerArgumentType.getInteger(
                            context,
                            "layer"
                    );

        } catch (IllegalArgumentException ignored) {
            // 未提供层数，使用默认值 1
        }

        double strength = 1.0D;

        try {

            strength =
                    DoubleArgumentType.getDouble(
                            context,
                            "strength"
                    );

        } catch (IllegalArgumentException ignored) {
            // 未提供强度，使用默认值 1
        }

        DebugDomainController.create(
                player,
                radius,
                layer,
                strength
        );

        final int finalLayer = layer;
        final double finalStrength = strength;

        source.sendSuccess(
                () -> Component.translatable(
                        "command.qisplan2.debug_domain.create.success",
                        radius,
                        finalLayer,
                        finalStrength
                ),
                true
        );

        return 1;
    }


    /*
     * ============================================================
     * 删除 Debug 鬼域
     * ============================================================
     */

    private static int removeDebugDomain(
            CommandContext<CommandSourceStack> context
    ) {

        CommandSourceStack source =
                context.getSource();

        if (!(source.getEntity()
                instanceof ServerPlayer player)) {

            source.sendFailure(
                    Component.translatable(
                            "command.qisplan2.player_only"
                    )
            );

            return 0;
        }

        DebugDomainController.remove(
                player
        );

        source.sendSuccess(
                () -> Component.translatable(
                        "command.qisplan2.debug_domain.remove.success"
                ),
                true
        );

        return 1;
    }

    /*
     * ============================================================
     * 检测当前位置是否被灵异隔绝
     * ============================================================
     */
    private static int debugIsolation(
            CommandContext<CommandSourceStack> context
    ) {

        CommandSourceStack source =
                context.getSource();

        if (!(source.getEntity()
                instanceof ServerPlayer player)) {

            source.sendFailure(
                    Component.translatable(
                            "command.qisplan2.player_only"
                    )
            );

            return 0;
        }

        GhostIsolationSystem.QueryResult result =
                GhostIsolationSystem.query(
                        player.serverLevel(),
                        player.blockPosition()
                );

        switch (result.state()) {

            case ISOLATED -> {

                source.sendSuccess(
                        () -> Component.translatable(
                                "command.qisplan2.debug_isolation.isolated"
                        ),
                        false
                );

                QisPlan2.LOGGER.info(
                        "[灵异隔绝测试] 玩家 {} 当前位置 {} 被确认处于灵异隔绝空间，查询方式：{}",
                        player.getGameProfile().getName(),
                        player.blockPosition(),
                        result.cacheHit()
                                ? "缓存命中"
                                : result.rechecked()
                                ? "缓存失效后重新检测"
                                : result.regionCreated()
                                ? "首次检测并建立缓存"
                                : "检测"
                );
            }

            case UNKNOWN -> {

                source.sendSuccess(
                        () -> Component.translatable(
                                "command.qisplan2.debug_isolation.unknown"
                        ),
                        false
                );

                QisPlan2.LOGGER.info(
                        "[灵异隔绝测试] 玩家 {} 当前位置 {} 无法确定灵异隔绝状态，查询方式：{}",
                        player.getGameProfile().getName(),
                        player.blockPosition(),
                        result.rechecked()
                                ? "缓存失效后重新检测"
                                : "首次检测"
                );
            }

            case OPEN -> {

                /*
                 * 当前 Detector 暂时不会主动返回 OPEN。
                 */
                source.sendSuccess(
                        () -> Component.translatable(
                                "command.qisplan2.debug_isolation.open"
                        ),
                        false
                );

                QisPlan2.LOGGER.info(
                        "[灵异隔绝测试] 玩家 {} 当前位置 {} 未被灵异隔绝，查询命中缓存：{}",
                        player.getGameProfile().getName(),
                        player.blockPosition(),
                        result.cacheHit()
                );
            }

            case DIRTY -> {

                /*
                 * DIRTY 是缓存内部状态。
                 *
                 * 正常情况下 query() 会自动重新检测，
                 * 因此这里理论上不会直接返回 DIRTY
                 */
                source.sendSuccess(
                        () -> Component.translatable(
                                "command.qisplan2.debug_isolation.unknown"
                        ),
                        false
                );

                QisPlan2.LOGGER.info(
                        "[灵异隔绝测试] 玩家 {} 当前位置 {} 状态为 DIRTY",
                        player.getGameProfile().getName(),
                        player.blockPosition()
                );
            }
        }

        return 1;
    }
}