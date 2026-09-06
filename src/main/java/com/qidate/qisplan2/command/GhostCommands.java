package com.qidate.qisplan2.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.core.ModAttachments;
import com.qidate.qisplan2.ghost.PossessedGhostState;
import com.qidate.qisplan2.ghost.PossessionHandler;
import com.qidate.qisplan2.ghost.ability.GhostAbilityRegistry;

import com.qidate.qisplan2.ghost.corrosion.CorrosionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumMap;
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
         * ========================================================
         */

        root.then(
                Commands.literal("corrosion")
                        .executes(
                                GhostCommands::corrosion
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
                            Component.literal(
                                    "这个命令必须由玩家执行。"
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
                            Component.literal(
                                    "不存在可驾驭的鬼："
                                            + ghost
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
                            Component.literal(
                                    "你已经驾驭了："
                                            + ghost
                            )
                    );

            return 0;
        }

        context.getSource()
                .sendSuccess(
                        () -> Component.literal(
                                "成功驾驭："
                                        + ghost
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
                            Component.literal(
                                    "这个命令必须由玩家执行。"
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
                            Component.literal(
                                    "你没有驾驭："
                                            + ghost
                            )
                    );

            return 0;
        }

        context.getSource()
                .sendSuccess(
                        () -> Component.literal(
                                "已解除驾驭："
                                        + ghost
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
                            Component.literal(
                                    "这个命令必须由玩家执行。"
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
                            () -> Component.literal(
                                    "当前没有驾驭任何鬼。"
                            ),
                            false
                    );

            return 0;
        }

        StringBuilder message =
                new StringBuilder(
                        "当前驾驭："
                );

        for (var entry :
                ghosts.entrySet()) {

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
                                    state.revival()
                                            * 100.0D
                            )
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
                            Component.literal(
                                    "这个命令必须由玩家执行。"
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
                            Component.literal(
                                    "你没有驾驭："
                                            + ghost
                            )
                    );

            return 0;
        }

        context.getSource()
                .sendSuccess(
                        () -> Component.literal(
                                ghost
                                        + " 已进入普通死机 "
                                        + seconds
                                        + " 秒。"
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
                            Component.literal(
                                    "这个命令必须由玩家执行。"
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
                            Component.literal(
                                    "你没有驾驭："
                                            + ghost
                            )
                    );

            return 0;
        }

        context.getSource()
                .sendSuccess(
                        () -> Component.literal(
                                ghost
                                        + " 已进入永久死机。"
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
                            Component.literal(
                                    "这个命令必须由玩家执行。"
                            )
                    );

            return 0;
        }

        EnumMap<CorrosionType, Integer> values =
                PossessionHandler.getAllCorrosion(
                        player
                );

        int global =
                values.getOrDefault(
                        CorrosionType.GLOBAL,
                        0
                );

        StringBuilder message =
                new StringBuilder();

        message.append("§6========== 侵蚀值 ==========")
                .append("\n")
                .append("§e全方位侵蚀：§f")
                .append(global);

        for (CorrosionType type :
                CorrosionType.values()) {

            if (type == CorrosionType.GLOBAL) {
                continue;
            }

            int value =
                    values.getOrDefault(
                            type,
                            0
                    );

            message.append("\n")
                    .append("§7")
                    .append(getCorrosionName(type))
                    .append("：§f")
                    .append(value);
        }

        context.getSource()
                .sendSuccess(
                        () -> Component.literal(
                                message.toString()
                        ),
                        false
                );

        return 1;
    }

    /*
     * ============================================================
     * 中文名称
     * ============================================================
     */

    private static String getCorrosionName(
            CorrosionType type
    ) {

        return switch (type) {

            case GLOBAL -> "全方位";

            case BRAIN -> "大脑";
            case HEART -> "心脏";
            case LUNG -> "肺";
            case STOMACH -> "胃";
            case LIVER -> "肝";
            case KIDNEY -> "肾";
            case PANCREAS -> "胰";
            case GALLBLADDER -> "胆";
            case SPLEEN -> "脾";
            case INTESTINE -> "肠";

            case SKIN -> "皮肤";
            case BLOOD -> "血液";
            case BONE -> "骨骼";
            case FLESH -> "血肉";

            case EYE -> "眼";
            case EAR -> "耳";
            case NOSE -> "鼻";
            case MOUTH -> "口";

            case HAND -> "手";
            case FOOT -> "脚";

            case HAIR -> "发";
        };
    }
}