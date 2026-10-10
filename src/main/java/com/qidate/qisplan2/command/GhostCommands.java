package com.qidate.qisplan2.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.qidate.qisplan2.core.ModAttachments;
import com.qidate.qisplan2.core.ModTags;
import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.curse.Curse;
import com.qidate.qisplan2.ghost.curse.CurseManager;
import com.qidate.qisplan2.ghost.curse.CurseRegistry;
import com.qidate.qisplan2.ghost.module.GhostItemData;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostModuleData;
import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostData;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostState;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import com.qidate.qisplan2.ghost.possession.ability.GhostAbilityRegistry;

import com.qidate.qisplan2.ghost.corrosion.CorrosionMatrix;
import com.qidate.qisplan2.ghost.corrosion.CorrosionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

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

        /*
         * ========================================================
         * /qisplan2 curse
         * ========================================================
         */

        root.then(
                Commands.literal("curse")

                        /*
                         * /qisplan2 curse list
                         */
                        .then(
                                Commands.literal("list")
                                        .executes(
                                                GhostCommands::curseList
                                        )
                        )

                        /*
                         * /qisplan2 curse add <curse> <target>
                         */
                        .then(
                                Commands.literal("add")
                                        .then(
                                                Commands.argument(
                                                                "curse",
                                                                ResourceLocationArgument.id()
                                                        )
                                                        .suggests(
                                                                (context, builder) -> {

                                                                    for (ResourceLocation id :
                                                                            CurseRegistry.ids()) {

                                                                        builder.suggest(
                                                                                id.toString()
                                                                        );
                                                                    }

                                                                    return builder.buildFuture();
                                                                }
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "target",
                                                                                EntityArgument.player()
                                                                        )
                                                                        .executes(
                                                                                GhostCommands::curseAdd
                                                                        )
                                                        )
                                        )
                        )

                        /*
                         * /qisplan2 curse remove <curseId>
                         */
                        .then(
                                Commands.literal("remove")
                                        .then(
                                                Commands.argument(
                                                                "curseId",
                                                                UuidArgument.uuid()
                                                        )
                                                        .executes(
                                                                GhostCommands::curseRemove
                                                        )
                                        )
                        )
        );

        /*
         * ========================================================
         * /qisplan2 module list
         * /qisplan2 module add <module>
         * /qisplan2 module remove <module>
         * /qisplan2 module info
         *
         * /qisplan2 module entity add <targets> <module>
         * /qisplan2 module entity remove <targets> <module>
         * /qisplan2 module entity info <targets>
         * ========================================================
         */

        root.then(
                Commands.literal("module")
                        .then(
                                Commands.literal("list")
                                        .executes(GhostCommands::moduleList)
                        )
                        .then(
                                Commands.literal("add")
                                        .then(
                                                Commands.argument(
                                                                "module",
                                                                ResourceLocationArgument.id()
                                                        )
                                                        .suggests(
                                                                (context, builder) -> {
                                                                    for (GhostModule module :
                                                                            GhostModuleRegistry.getAll()) {
                                                                        builder.suggest(
                                                                                module.getId().toString()
                                                                        );
                                                                    }

                                                                    return builder.buildFuture();
                                                                }
                                                        )
                                                        .executes(GhostCommands::moduleAdd)
                                        )
                        )
                        .then(
                                Commands.literal("remove")
                                        .then(
                                                Commands.argument(
                                                                "module",
                                                                ResourceLocationArgument.id()
                                                        )
                                                        .suggests(
                                                                (context, builder) -> {
                                                                    for (GhostModule module :
                                                                            GhostModuleRegistry.getAll()) {
                                                                        builder.suggest(
                                                                                module.getId().toString()
                                                                        );
                                                                    }

                                                                    return builder.buildFuture();
                                                                }
                                                        )
                                                        .executes(GhostCommands::moduleRemove)
                                        )
                        )
                        .then(
                                Commands.literal("info")
                                        .executes(GhostCommands::moduleInfo)
                        )
                        .then(
                                Commands.literal("entity")
                                        .then(
                                                Commands.literal("add")
                                                        .then(
                                                                Commands.argument(
                                                                                "targets",
                                                                                EntityArgument.entities()
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "module",
                                                                                                ResourceLocationArgument.id()
                                                                                        )
                                                                                        .suggests(
                                                                                                (context, builder) -> {
                                                                                                    for (GhostModule module :
                                                                                                            GhostModuleRegistry.getAll()) {
                                                                                                        builder.suggest(
                                                                                                                module.getId().toString()
                                                                                                        );
                                                                                                    }

                                                                                                    return builder.buildFuture();
                                                                                                }
                                                                                        )
                                                                                        .executes(
                                                                                                GhostCommands::moduleEntityAdd
                                                                                        )
                                                                        )
                                                        )
                                        )
                                        .then(
                                                Commands.literal("remove")
                                                        .then(
                                                                Commands.argument(
                                                                                "targets",
                                                                                EntityArgument.entities()
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "module",
                                                                                                ResourceLocationArgument.id()
                                                                                        )
                                                                                        .suggests(
                                                                                                (context, builder) -> {
                                                                                                    for (GhostModule module :
                                                                                                            GhostModuleRegistry.getAll()) {
                                                                                                        builder.suggest(
                                                                                                                module.getId().toString()
                                                                                                        );
                                                                                                    }

                                                                                                    return builder.buildFuture();
                                                                                                }
                                                                                        )
                                                                                        .executes(
                                                                                                GhostCommands::moduleEntityRemove
                                                                                        )
                                                                        )
                                                        )
                                        )
                                        .then(
                                                Commands.literal("info")
                                                        .then(
                                                                Commands.argument(
                                                                                "targets",
                                                                                EntityArgument.entities()
                                                                        )
                                                                        .executes(
                                                                                GhostCommands::moduleEntityInfo
                                                                        )
                                                        )
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

        /*
         * 厉鬼名称
         */
        String ghostName =
                Component.translatable(
                        "ghost."
                                + ghost.getNamespace()
                                + "."
                                + ghost.getPath()
                ).getString();

        /*
         * 未知厉鬼
         */
        if (!GhostAbilityRegistry.contains(
                ghost
        )) {

            context.getSource()
                    .sendFailure(
                            Component.translatable(
                                    "command.qisplan2.ghost_not_found",
                                    ghostName
                            )
                    );

            return 0;
        }

        /*
         * 成功驾驭
         */
        if (!PossessionHandler.possess(
                player,
                ghost
        )) {

            context.getSource()
                    .sendFailure(
                            Component.translatable(
                                    "command.qisplan2.already_possessed",
                                    ghostName
                            )
                    );

            return 0;
        }

        context.getSource()
                .sendSuccess(
                        () -> Component.translatable(
                                "command.qisplan2.possess.success",
                                ghostName
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

        String ghostName =
                Component.translatable(
                        "ghost."
                                + ghost.getNamespace()
                                + "."
                                + ghost.getPath()
                ).getString();

        if (!PossessionHandler.release(
                player,
                ghost
        )) {

            context.getSource()
                    .sendFailure(
                            Component.translatable(
                                    "command.qisplan2.not_possessed",
                                    ghostName
                            )
                    );

            return 0;
        }

        context.getSource()
                .sendSuccess(
                        () -> Component.translatable(
                                "command.qisplan2.release.success",
                                ghostName
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

        Map<ResourceLocation, PossessedGhostData> ghosts =
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
                    entry.getValue().state();

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

        String ghostName =
                Component.translatable(
                        "ghost."
                                + ghost.getNamespace()
                                + "."
                                + ghost.getPath()
                ).getString();

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
                                    ghostName
                            )
                    );

            return 0;
        }

        context.getSource()
                .sendSuccess(
                        () -> Component.translatable(
                                "command.qisplan2.stun.success",
                                ghostName,
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

        String ghostName =
                Component.translatable(
                        "ghost."
                                + ghost.getNamespace()
                                + "."
                                + ghost.getPath()
                ).getString();

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
                                    ghostName
                            )
                    );

            return 0;
        }

        context.getSource()
                .sendSuccess(
                        () -> Component.translatable(
                                "command.qisplan2.permanent_stun.success",
                                ghostName
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
                );


        /*
         * ========================================================
         * 每一个侵蚀部位
         * ========================================================
         */

        for (CorrosionType type :
                CorrosionType.values()) {

            int total =
                    matrix.total(type);

            message.append(
                    Component.translatable(
                            "command.qisplan2.corrosion.part",
                            getCorrosionName(type),
                            total
                    )
            );

            appendGhostContributions(
                    message,
                    matrix,
                    type,
                    baseGrowth
            );
        }


        message.append(
                Component.translatable(
                        "command.qisplan2.corrosion.footer"
                )
        );


        if (baseGrowth > 0.0D) {

            message.append(
                    Component.translatable(
                            "command.qisplan2.corrosion.simulated_growth",
                            String.format(
                                    "%.2f",
                                    baseGrowth
                            )
                    )
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
                );

            } else {

                message.append(
                        Component.translatable(
                                "command.qisplan2.corrosion.contribution",
                                ghostName,
                                contribution,
                                ratio * 100.0D
                        )
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
                        .append(
                                Component.translatable(
                                        "command.qisplan2.defense.body_corrosion",
                                        String.format(
                                                "%.1f",
                                                bodyCorrosion
                                        )
                                )
                        )
                        .append(
                                Component.translatable(
                                        "command.qisplan2.defense.damage_reduction",
                                        String.format(
                                                "%.1f%%",
                                                reduction * 100.0D
                                        )
                                )
                        )
                        .append(
                                Component.translatable(
                                        "command.qisplan2.defense.health_bonus",
                                        String.format(
                                                "+%.1f",
                                                healthBonus
                                        )
                                )
                        )
                        .append(
                                Component.translatable(
                                        "command.qisplan2.defense.max_health",
                                        String.format(
                                                "%.1f",
                                                player.getMaxHealth()
                                        )
                                )
                        )
                        .append(
                                Component.translatable(
                                        "command.qisplan2.defense.footer"
                                )
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
     * 查看当前所有诅咒
     * ============================================================
     */

    private static int curseList(
            CommandContext<CommandSourceStack> context
    ) {

        var curses =
                com.qidate.qisplan2.ghost.curse.CurseManager.getAll();

        if (curses.isEmpty()) {

            context.getSource()
                    .sendSuccess(
                            () -> Component.translatable(
                                    "command.qisplan2.curse.empty"
                            ),
                            false
                    );

            return 0;
        }

        MutableComponent message =
                Component.translatable(
                        "command.qisplan2.curse.header"
                );

        for (var curse : curses) {

            message.append(
                    "\n"
            );

            message.append(
                    Component.translatable(
                            "command.qisplan2.curse.entry",
                            curse.getId().toString(),
                            curse.getTarget().toString()
                    )
            );
        }

        context.getSource()
                .sendSuccess(
                        () -> message,
                        false
                );

        return curses.size();
    }

    /*
     * ============================================================
     * 添加诅咒
     * ============================================================
     */

    private static int curseAdd(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {

//        ResourceLocation curseType =
//                ResourceLocationArgument.getId(
//                        context,
//                        "curse"
//                );
//
//        ServerPlayer target =
//                EntityArgument.getPlayer(
//                        context,
//                        "target"
//                );
//
//        if (!CurseRegistry.contains(curseType)) {
//
//            context.getSource()
//                    .sendFailure(
//                            Component.literal(
//                                    "未知诅咒类型：" + curseType
//                            )
//                    );
//
//            return 0;
//        }
//
//        Curse curse =
//                CurseRegistry.create(
//                        curseType,
//                        target.getUUID()
//                );
//
//        if (curse == null) {
//
//            context.getSource()
//                    .sendFailure(
//                            Component.literal(
//                                    "创建诅咒失败：" + curseType
//                            )
//                    );
//
//            return 0;
//        }
//
//        CurseManager.add(curse);
//
//        context.getSource()
//                .sendSuccess(
//                        () -> Component.literal(
//                                "已添加诅咒 "
//                                        + curseType
//                                        + " -> "
//                                        + target.getGameProfile().getName()
//                                        + "（"
//                                        + curse.getId()
//                                        + "）"
//                        ),
//                        true
//                );

        return 1;
    }

    /*
     * ============================================================
     * 移除诅咒
     * ============================================================
     */

    private static int curseRemove(
            CommandContext<CommandSourceStack> context
    ) {

        UUID curseId =
                UuidArgument.getUuid(
                        context,
                        "curseId"
                );

        Curse curse =
                CurseManager.get(curseId);

        if (curse == null) {

            context.getSource()
                    .sendFailure(
                            Component.literal(
                                    "诅咒不存在：" + curseId
                            )
                    );

            return 0;
        }

//        CurseManager.remove(curseId);

        context.getSource()
                .sendSuccess(
                        () -> Component.literal(
                                "已移除诅咒：" + curseId
                        ),
                        true
                );

        return 1;
    }

    /**
     * /qisplan2 module list
     *
     * 列出所有已注册的厉鬼模块。
     */
    private static int moduleList(
            CommandContext<CommandSourceStack> context
    ) {
        var source = context.getSource();

        var modules = GhostModuleRegistry.getAll();

        if (modules.isEmpty()) {
            source.sendSuccess(
                    () -> Component.literal("当前没有注册任何厉鬼模块。"),
                    false
            );
            return 1;
        }

        source.sendSuccess(
                () -> Component.literal("已注册的厉鬼模块："),
                false
        );

        for (GhostModule module : modules) {
            source.sendSuccess(
                    () -> Component.literal(
                            "- " + module.getId()
                    ),
                    false
            );
        }

        return modules.size();
    }


    /**
     * /qisplan2 module add <module>
     *
     * 将指定模块装入主手灵异物品。
     */
    private static int moduleAdd(
            CommandContext<CommandSourceStack> context
    ) {
        var source = context.getSource();

        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendFailure(
                    Component.literal("该命令只能由玩家执行。")
            );
            return 0;
        }

        ResourceLocation moduleId =
                ResourceLocationArgument.getId(context, "module");

        GhostModule module = GhostModuleRegistry.get(moduleId);

        if (module == null) {
            source.sendFailure(
                    Component.literal("未注册的厉鬼模块：" + moduleId)
            );
            return 0;
        }

        ItemStack stack = player.getMainHandItem();

        if (stack.isEmpty()
                || !stack.is(ModTags.Items.SUPERNATURAL_ITEMS)) {
            source.sendFailure(
                    Component.literal(
                            "请在主手持有带有 supernatural_items Tag 的灵异物品。"
                    )
            );
            return 0;
        }

        GhostItemData.addModule(
                stack,
                moduleId,
                1.0D
        );

        source.sendSuccess(
                () -> Component.literal(
                        "已将厉鬼模块 " + moduleId
                                + " 装入物品，灵异强度：1.0"
                ),
                true
        );

        return 1;
    }


    /**
     * /qisplan2 module remove <module>
     *
     * 从主手物品移除指定模块。
     */
    private static int moduleRemove(
            CommandContext<CommandSourceStack> context
    ) {
        var source = context.getSource();

        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendFailure(
                    Component.literal("该命令只能由玩家执行。")
            );
            return 0;
        }

        ResourceLocation moduleId =
                ResourceLocationArgument.getId(context, "module");

        ItemStack stack = player.getMainHandItem();

        if (stack.isEmpty()
                || !stack.is(ModTags.Items.SUPERNATURAL_ITEMS)) {
            source.sendFailure(
                    Component.literal("请先手持灵异物品。")
            );
            return 0;
        }

        if (!GhostItemData.hasModule(stack, moduleId)) {
            source.sendFailure(
                    Component.literal(
                            "该物品没有携带模块：" + moduleId
                    )
            );
            return 0;
        }

        GhostItemData.removeModule(stack, moduleId);

        source.sendSuccess(
                () -> Component.literal(
                        "已从物品中移除厉鬼模块：" + moduleId
                ),
                true
        );

        return 1;
    }


    /**
     * /qisplan2 module info
     *
     * 查看主手物品携带的模块。
     */
    private static int moduleInfo(
            CommandContext<CommandSourceStack> context
    ) {
        var source = context.getSource();

        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendFailure(
                    Component.literal("该命令只能由玩家执行。")
            );
            return 0;
        }

        ItemStack stack = player.getMainHandItem();

        if (stack.isEmpty()
                || !stack.is(ModTags.Items.SUPERNATURAL_ITEMS)) {
            source.sendFailure(
                    Component.literal("请先手持灵异物品。")
            );
            return 0;
        }

        var modules = GhostItemData.getModules(stack);

        if (modules.isEmpty()) {
            source.sendSuccess(
                    () -> Component.literal("该物品尚未装载任何厉鬼模块。"),
                    false
            );
            return 1;
        }

        source.sendSuccess(
                () -> Component.literal("物品携带的厉鬼模块："),
                false
        );

        for (GhostModuleData.Entry entry : modules) {
            source.sendSuccess(
                    () -> Component.literal(
                            "- " + entry.id()
                                    + "（灵异强度："
                                    + entry.intensity()
                                    + "）"
                    ),
                    false
            );
        }

        return modules.size();
    }

    /**
     * /qisplan2 module entity add <targets> <module>
     *
     * 为选中的模块化厉鬼实体安装模块。
     */
    private static int moduleEntityAdd(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        var source = context.getSource();

        var targets = EntityArgument.getEntities(context, "targets");

        ResourceLocation moduleId =
                ResourceLocationArgument.getId(context, "module");

        GhostModule module = GhostModuleRegistry.get(moduleId);

        if (module == null) {
            source.sendFailure(
                    Component.literal("未注册的厉鬼模块：" + moduleId)
            );
            return 0;
        }

        int successCount = 0;
        int skippedCount = 0;

        for (Entity target : targets) {
            if (!(target instanceof ModularGhostEntity ghost)) {
                skippedCount++;
                continue;
            }

            ghost.addGhostModule(moduleId, 1.0D);
            successCount++;
        }

        if (successCount == 0) {
            source.sendFailure(
                    Component.literal(
                            "选择的实体中没有可操作的模块化厉鬼实体。"
                    )
            );
            return 0;
        }

        final int installedCount = successCount;
        final int skipped = skippedCount;

        source.sendSuccess(
                () -> Component.literal(
                        "已为 " + installedCount
                                + " 个模块化厉鬼实体安装模块 "
                                + moduleId
                                + "，灵异强度：1.0"
                                + (skipped > 0
                                ? "；跳过了 " + skipped + " 个非模块化厉鬼实体。"
                                : "")
                ),
                true
        );

        return successCount;
    }

    /**
     * /qisplan2 module entity remove <targets> <module>
     *
     * 从选中的模块化厉鬼实体移除模块。
     */
    private static int moduleEntityRemove(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        var source = context.getSource();

        var targets = EntityArgument.getEntities(context, "targets");

        ResourceLocation moduleId =
                ResourceLocationArgument.getId(context, "module");

        int removedCount = 0;
        int skippedCount = 0;

        for (Entity target : targets) {
            if (!(target instanceof ModularGhostEntity ghost)) {
                skippedCount++;
                continue;
            }

            if (!ghost.hasGhostModule(moduleId)) {
                continue;
            }

            ghost.removeGhostModule(moduleId);
            removedCount++;
        }

        if (removedCount == 0) {
            source.sendFailure(
                    Component.literal(
                            "没有实体移除模块 " + moduleId
                                    + "。请检查目标类型以及实体是否安装该模块。"
                    )
            );
            return 0;
        }

        final int count = removedCount;
        final int skipped = skippedCount;

        source.sendSuccess(
                () -> Component.literal(
                        "已从 " + count
                                + " 个模块化厉鬼实体移除模块："
                                + moduleId
                                + (skipped > 0
                                ? "；跳过了 " + skipped + " 个非模块化厉鬼实体。"
                                : "")
                ),
                true
        );

        return removedCount;
    }

    /**
     * /qisplan2 module entity info <targets>
     *
     * 查看选中的模块化厉鬼实体携带的模块。
     */
    private static int moduleEntityInfo(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        var source = context.getSource();

        var targets = EntityArgument.getEntities(context, "targets");

        int entityCount = 0;

        for (Entity target : targets) {
            if (!(target instanceof ModularGhostEntity ghost)) {
                continue;
            }

            entityCount++;

            var modules = ghost.getModules();

            source.sendSuccess(
                    () -> Component.literal(
                            "实体：" + ghost.getName().getString()
                                    + "（UUID：" + ghost.getUUID() + "）"
                    ),
                    false
            );

            if (modules.isEmpty()) {
                source.sendSuccess(
                        () -> Component.literal(
                                "  尚未装载任何厉鬼模块。"
                        ),
                        false
                );
                continue;
            }

            for (GhostModuleData.Entry entry : modules) {
                source.sendSuccess(
                        () -> Component.literal(
                                "  - " + entry.id()
                                        + "（灵异强度："
                                        + entry.intensity()
                                        + "）"
                        ),
                        false
                );
            }
        }

        if (entityCount == 0) {
            source.sendFailure(
                    Component.literal(
                            "选择的实体中没有模块化厉鬼实体。"
                    )
            );
            return 0;
        }

        return entityCount;
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