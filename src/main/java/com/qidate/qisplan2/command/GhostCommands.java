package com.qidate.qisplan2.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import com.qidate.qisplan2.core.ModAttachments;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostState;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import com.qidate.qisplan2.ghost.possession.ability.GhostAbilityRegistry;

import com.qidate.qisplan2.ghost.corrosion.CorrosionMatrix;
import com.qidate.qisplan2.ghost.corrosion.CorrosionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Map;

public final class GhostCommands {

    private GhostCommands() {
    }

    public static void register(
            LiteralArgumentBuilder<CommandSourceStack> root
    ) {

        /*
         * ========================================================
         * /qisplan2 possess <ghost>
         * ========================================================
         */

        root.then(
                Commands.literal("possess")
                        .then(
                                Commands.argument(
                                                "ghost",
                                                ResourceLocationArgument.id()
                                        )
                                        .suggests(
                                                (context, builder) -> {

                                                    for (ResourceLocation id :
                                                            GhostAbilityRegistry.ids()) {

                                                        builder.suggest(
                                                                id.toString()
                                                        );
                                                    }

                                                    return builder.buildFuture();
                                                }
                                        )
                                        .executes(
                                                GhostCommands::possess
                                        )
                        )
        );


        /*
         * ========================================================
         * /qisplan2 release <ghost>
         * ========================================================
         */

        root.then(
                Commands.literal("release")
                        .then(
                                Commands.argument(
                                                "ghost",
                                                ResourceLocationArgument.id()
                                        )
                                        .suggests(
                                                (context, builder) -> {

                                                    for (ResourceLocation id :
                                                            GhostAbilityRegistry.ids()) {

                                                        builder.suggest(
                                                                id.toString()
                                                        );
                                                    }

                                                    return builder.buildFuture();
                                                }
                                        )
                                        .executes(
                                                GhostCommands::release
                                        )
                        )
        );


        /*
         * ========================================================
         * /qisplan2 possessed
         * ========================================================
         */

        root.then(
                Commands.literal("possessed")
                        .executes(
                                GhostCommands::possessed
                        )
        );

        /*
         * ========================================================
         * /qisplan2 corrosion
         * /qisplan2 corrosion <baseGrowth>
         * ========================================================
         */

        root.then(
                Commands.literal("corrosion")
                        .executes(
                                GhostCommands::corrosion
                        )
                        .then(
                                Commands.argument(
                                                "baseGrowth",
                                                DoubleArgumentType.doubleArg(
                                                        0.0D
                                                )
                                        )
                                        .executes(
                                                GhostCommands::corrosion
                                        )
                        )
        );

        /*
         * ========================================================
         * /qisplan2 defense
         * ========================================================
         */

        root.then(
                Commands.literal("defense")
                        .executes(
                                GhostCommands::defense
                        )
        );


        /*
         * ========================================================
         * /qisplan2 stun <ghost> <seconds>
         * ========================================================
         */

        root.then(
                Commands.literal("stun")
                        .then(
                                Commands.argument(
                                                "ghost",
                                                ResourceLocationArgument.id()
                                        )
                                        .then(
                                                Commands.argument(
                                                                "seconds",
                                                                IntegerArgumentType.integer(
                                                                        1,
                                                                        3600
                                                                )
                                                        )
                                                        .suggests(
                                                                (context, builder) -> {

                                                                    for (ResourceLocation id :
                                                                            GhostAbilityRegistry.ids()) {

                                                                        builder.suggest(
                                                                                id.toString()
                                                                        );
                                                                    }

                                                                    return builder.buildFuture();
                                                                }
                                                        )
                                                        .executes(
                                                                GhostCommands::stun
                                                        )
                                        )
                        )
        );


        /*
         * ========================================================
         * /qisplan2 permanent_stun <ghost>
         * ========================================================
         */

        root.then(
                Commands.literal("permanent_stun")
                        .then(
                                Commands.argument(
                                                "ghost",
                                                ResourceLocationArgument.id()
                                        )
                                        .suggests(
                                                (context, builder) -> {

                                                    for (ResourceLocation id :
                                                            GhostAbilityRegistry.ids()) {

                                                        builder.suggest(
                                                                id.toString()
                                                        );
                                                    }

                                                    return builder.buildFuture();
                                                }
                                        )
                                        .executes(
                                                GhostCommands::permanentStun
                                        )
                        )
        );
    }


    /*
     * ============================================================
     * 驾驭
     * ============================================================
     */

    private static int possess(
            CommandContext<CommandSourceStack> context
    ) {

        ServerPlayer player =
                context.getSource().getPlayer();

        if (player == null) {
            context.getSource()
                    .sendFailure(
                            Component.translatable(
                                    "command.qisplan2.player_only"
                            )
                    );

            return 0;
        }

        ResourceLocation ghost =
                ResourceLocationArgument.getId(
                        context,
                        "ghost"
                );

        if (!GhostAbilityRegistry.contains(
                ghost
        )) {

            context.getSource()
                    .sendFailure(
                            Component.translatable(
                                    "command.qisplan2.ghost_not_found",
                                    ghost
                            )
                    );

            return 0;
        }

        if (!PossessionHandler.possess(
                player,
                ghost
        )) {

            context.getSource()
                    .sendFailure(
                            Component.translatable(
                                    "command.qisplan2.already_possessed",
                                    ghost
                            )
                    );

            return 0;
        }

        context.getSource()
                .sendSuccess(
                        () -> Component.translatable(
                                "command.qisplan2.possess.success",
                                ghost
                        ),
                        true
                );

        return 1;
    }


    /*
     * ============================================================
     * 解除驾驭
     * ============================================================
     */

    private static int release(
            CommandContext<CommandSourceStack> context
    ) {

        ServerPlayer player =
                context.getSource().getPlayer();

        if (player == null) {
            context.getSource()
                    .sendFailure(
                            Component.translatable(
                                    "command.qisplan2.player_only"
                            )
                    );

            return 0;
        }

        ResourceLocation ghost =
                ResourceLocationArgument.getId(
                        context,
                        "ghost"
                );

        if (!PossessionHandler.release(
                player,
                ghost
        )) {

            context.getSource()
                    .sendFailure(
                            Component.translatable(
                                    "command.qisplan2.not_possessed",
                                    ghost
                            )
                    );

            return 0;
        }

        context.getSource()
                .sendSuccess(
                        () -> Component.translatable(
                                "command.qisplan2.release.success",
                                ghost
                        ),
                        true
                );

        return 1;
    }


    /*
     * ============================================================
     * 查看所有驾驭
     * ============================================================
     */

    private static int possessed(
            CommandContext<CommandSourceStack> context
    ) {

        ServerPlayer player =
                context.getSource().getPlayer();

        if (player == null) {
            context.getSource()
                    .sendFailure(
                            Component.translatable(
                                    "command.qisplan2.player_only"
                            )
                    );

            return 0;
        }

        Map<ResourceLocation, PossessedGhostState> ghosts =
                player.getData(
                        ModAttachments.POSSESSED_GHOSTS
                );

        if (ghosts.isEmpty()) {

            context.getSource()
                    .sendSuccess(
                            () -> Component.translatable(
                                    "command.qisplan2.possessed.empty"
                            ),
                            false
                    );

            return 0;
        }

        MutableComponent message =
                Component.translatable(
                        "command.qisplan2.possessed.header"
                );

        for (var entry :
                ghosts.entrySet()) {

            ResourceLocation ghost =
                    entry.getKey();

            PossessedGhostState state =
                    entry.getValue();

            String ghostName =
                    Component.translatable(
                            "ghost."
                                    + ghost.getNamespace()
                                    + "."
                                    + ghost.getPath()
                    ).getString();

            message
                    .append("\n")
                    .append(
                            Component.literal(
                                            ghostName
                                    )
                                    .withStyle(ChatFormatting.YELLOW)
                    )
                    .append(
                            Component.translatable(
                                            "command.qisplan2.possessed.entry",
                                            String.format(
                                                    "%.1f%%",
                                                    state.revival() * 100.0D
                                            )
                                    )
                                    .withStyle(ChatFormatting.WHITE)
                    );
        }

        context.getSource()
                .sendSuccess(
                        () -> Component.literal(
                                message.toString()
                        ),
                        false
                );

        return ghosts.size();
    }


    /*
     * ============================================================
     * 普通死机
     * ============================================================
     */

    private static int stun(
            CommandContext<CommandSourceStack> context
    ) {

        ServerPlayer player =
                context.getSource().getPlayer();

        if (player == null) {
            context.getSource()
                    .sendFailure(
                            Component.translatable(
                                    "command.qisplan2.player_only"
                            )
                    );

            return 0;
        }

        ResourceLocation ghost =
                ResourceLocationArgument.getId(
                        context,
                        "ghost"
                );

        int seconds =
                IntegerArgumentType.getInteger(
                        context,
                        "seconds"
                );

        boolean success =
                PossessionHandler.testStun(
                        player,
                        ghost,
                        seconds * 20L
                );

        if (!success) {

            context.getSource()
                    .sendFailure(
                            Component.translatable(
                                    "command.qisplan2.not_possessed",
                                    ghost
                            )
                    );

            return 0;
        }

        context.getSource()
                .sendSuccess(
                        () -> Component.translatable(
                                "command.qisplan2.stun.success",
                                ghost,
                                seconds
                        ),
                        true
                );

        return 1;
    }


    /*
     * ============================================================
     * 永久死机
     * ============================================================
     */

    private static int permanentStun(
            CommandContext<CommandSourceStack> context
    ) {

        ServerPlayer player =
                context.getSource().getPlayer();

        if (player == null) {
            context.getSource()
                    .sendFailure(
                            Component.translatable(
                                    "command.qisplan2.player_only"
                            )
                    );

            return 0;
        }

        ResourceLocation ghost =
                ResourceLocationArgument.getId(
                        context,
                        "ghost"
                );

        boolean success =
                PossessionHandler.testPermanentStun(
                        player,
                        ghost
                );

        if (!success) {

            context.getSource()
                    .sendFailure(
                            Component.translatable(
                                    "command.qisplan2.not_possessed",
                                    ghost
                            )
                    );

            return 0;
        }

        context.getSource()
                .sendSuccess(
                        () -> Component.translatable(
                                "command.qisplan2.permanent_stun.success",
                                ghost
                        ),
                        true
                );

        return 1;
    }

    /*
     * ============================================================
     * 查看侵蚀值
     * ============================================================
     */

    private static int corrosion(
            CommandContext<CommandSourceStack> context
    ) {

        ServerPlayer player =
                context.getSource().getPlayer();

        if (player == null) {

            context.getSource()
                    .sendFailure(
                            Component.translatable(
                                    "command.qisplan2.player_only"
                            )
                    );

            return 0;
        }

        CorrosionMatrix matrix =
                PossessionHandler.getCorrosionMatrix(
                        player
                );

        /*
         * 是否提供了基础增长值。
         *
         * 不能使用 context.getNodes().containsKey()
         * 因为 getNodes() 返回的是 List。
         */
        double baseGrowth = 0.0D;

        try {

            baseGrowth =
                    DoubleArgumentType.getDouble(
                            context,
                            "baseGrowth"
                    );

        } catch (IllegalArgumentException ignored) {
            /*
             * 没有提供参数时，
             * 只是查看侵蚀，不模拟增长。
             */
        }


        MutableComponent message =
                Component.translatable(
                                "command.qisplan2.corrosion.title"
                        )
                        .withStyle(ChatFormatting.GOLD);


        /*
         * ========================================================
         * 每一个侵蚀部位
         * ========================================================
         */

        for (CorrosionType type :
                CorrosionType.values()) {

            int total =
                    matrix.total(type);

            message
                    .append("\n\n")
                    .append(
                            Component.literal("【")
                                    .withStyle(ChatFormatting.YELLOW)
                    )
                    .append(
                            Component.translatable(
                                            "corrosion.qisplan2." +
                                                    type.name().toLowerCase(
                                                            Locale.ROOT
                                                    )
                                    )
                                    .withStyle(ChatFormatting.YELLOW)
                    )
                    .append(
                            Component.literal("】")
                                    .withStyle(ChatFormatting.YELLOW)
                    )
                    .append("\n")
                    .append(
                            Component.translatable(
                                            "command.qisplan2.corrosion.total"
                                    )
                                    .withStyle(ChatFormatting.WHITE)
                    )
                    .append(
                            Component.literal(
                                            String.valueOf(total)
                                    )
                                    .withStyle(ChatFormatting.WHITE)
                    );


            appendGhostContributions(
                    message,
                    matrix,
                    type,
                    baseGrowth
            );
        }


        message
                .append("\n\n")
                .append(
                        Component.translatable(
                                        "command.qisplan2.corrosion.footer"
                                )
                                .withStyle(ChatFormatting.GOLD)
                );


        if (baseGrowth > 0.0D) {

            message
                    .append("\n")
                    .append(
                            Component.translatable(
                                            "command.qisplan2.corrosion.simulated_growth"
                                    )
                                    .withStyle(ChatFormatting.GRAY)
                    )
                    .append(
                            Component.literal(
                                            String.format(
                                                    "%.2f",
                                                    baseGrowth
                                            )
                                    )
                                    .withStyle(ChatFormatting.WHITE)
                    );
        }


        context.getSource()
                .sendSuccess(
                        () -> message,
                        false
                );

        return 1;
    }

    /*
     * ============================================================
     * 输出某个部位的鬼贡献
     * ============================================================
     */

    private static void appendGhostContributions(
            MutableComponent message,
            CorrosionMatrix matrix,
            CorrosionType target,
            double baseGrowth
    ) {

        int total =
                matrix.total(target);

        if (total <= 0) {
            return;
        }

        Map<ResourceLocation, Integer> ghosts =
                matrix.contributions(target);

        message.append(
                Component.translatable(
                                "command.qisplan2.corrosion.contribution_separator"
                        )
                        .withStyle(ChatFormatting.WHITE)
        );

        boolean first = true;

        for (ResourceLocation ghost :
                ghosts.keySet()) {

            int contribution =
                    matrix.contribution(
                            target,
                            ghost
                    );

            double ratio =
                    contribution / (double) total;


            if (!first) {

                message.append(
                        Component.translatable(
                                        "command.qisplan2.corrosion.contribution_separator"
                                )
                                .withStyle(ChatFormatting.WHITE)
                );
            }

            first = false;


            String ghostName =
                    Component.translatable(
                            "ghost."
                                    + ghost.getNamespace()
                                    + "."
                                    + ghost.getPath()
                    ).getString();


            if (baseGrowth > 0.0D) {

                message.append(
                        Component.translatable(
                                        "command.qisplan2.corrosion.contribution_growth",
                                        ghostName,
                                        contribution,
                                        ratio * 100.0D,
                                        String.format(
                                                "%.2f",
                                                baseGrowth * ratio
                                        )
                                )
                                .withStyle(ChatFormatting.WHITE)
                );

            } else {

                message.append(
                        Component.translatable(
                                        "command.qisplan2.corrosion.contribution",
                                        ghostName,
                                        contribution,
                                        ratio * 100.0D
                                )
                                .withStyle(ChatFormatting.WHITE)
                );
            }
        }
    }

    /*
     * ============================================================
     * 查看肉身强化
     * ============================================================
     */

    private static int defense(
            CommandContext<CommandSourceStack> context
    ) {

        ServerPlayer player =
                context.getSource().getPlayer();

        if (player == null) {

            context.getSource()
                    .sendFailure(
                            Component.translatable(
                                    "command.qisplan2.player_only"
                            )
                    );

            return 0;
        }

        double bodyCorrosion =
                PossessionHandler.getEffectiveBodyCorrosion(
                        player
                );

        double reduction =
                PossessionHandler.getNonSupernaturalDamageReduction(
                        player
                );

        double healthBonus =
                PossessionHandler.getMaxHealthBonus(
                        player
                );

        Component message =
                Component.translatable(
                                "command.qisplan2.defense.title"
                        )
                        .withStyle(ChatFormatting.GOLD)
                        .append("\n")
                        .append(
                                Component.translatable(
                                                "command.qisplan2.defense.body_corrosion"
                                        )
                                        .withStyle(ChatFormatting.WHITE)
                        )
                        .append(
                                Component.literal(
                                                String.format(
                                                        "%.1f",
                                                        bodyCorrosion
                                                )
                                        )
                                        .withStyle(ChatFormatting.YELLOW)
                        )
                        .append("\n")
                        .append(
                                Component.translatable(
                                                "command.qisplan2.defense.damage_reduction"
                                        )
                                        .withStyle(ChatFormatting.WHITE)
                        )
                        .append(
                                Component.literal(
                                                String.format(
                                                        "%.1f%%",
                                                        reduction * 100.0D
                                                )
                                        )
                                        .withStyle(ChatFormatting.AQUA)
                        )
                        .append("\n")
                        .append(
                                Component.translatable(
                                                "command.qisplan2.defense.health_bonus"
                                        )
                                        .withStyle(ChatFormatting.WHITE)
                        )
                        .append(
                                Component.literal(
                                                String.format(
                                                        "+%.1f",
                                                        healthBonus
                                                )
                                        )
                                        .withStyle(ChatFormatting.RED)
                        )
                        .append("\n")
                        .append(
                                Component.translatable(
                                                "command.qisplan2.defense.max_health"
                                        )
                                        .withStyle(ChatFormatting.WHITE)
                        )
                        .append(
                                Component.literal(
                                                String.format(
                                                        "%.1f",
                                                        player.getMaxHealth()
                                                )
                                        )
                                        .withStyle(ChatFormatting.GREEN)
                        )
                        .append("\n")
                        .append(
                                Component.translatable(
                                                "command.qisplan2.defense.footer"
                                        )
                                        .withStyle(ChatFormatting.GOLD)
                        );

        context.getSource()
                .sendSuccess(
                        () -> message,
                        false
                );

        return 1;
    }

    /*
     * ============================================================
     * 器官名称
     * ============================================================
     */

    private static String getCorrosionName(
            CorrosionType type
    ) {
        return Component.translatable(
                "corrosion.qisplan2." +
                        type.name().toLowerCase(Locale.ROOT)
        ).getString();
    }
}