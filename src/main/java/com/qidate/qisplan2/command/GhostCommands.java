package com.qidate.qisplan2.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.qidate.qisplan2.core.ModTags;
import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.corrosion.CorrosionMatrix;
import com.qidate.qisplan2.ghost.bearing.SpiritualPart;
import com.qidate.qisplan2.ghost.curse.Curse;
import com.qidate.qisplan2.ghost.curse.CurseManager;
import com.qidate.qisplan2.ghost.curse.CurseRegistry;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.GhostModuleCombatState;
import com.qidate.qisplan2.ghost.bearing.GhostModuleForm;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import net.minecraft.network.chat.HoverEvent;
import com.qidate.qisplan2.ghost.module.GhostModuleDataModifier;
import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemModuleHost;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostData;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostData;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostState;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

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
                                                            GhostModuleRegistry.ids()) {

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
                                                            GhostModuleRegistry.ids()) {

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
                                                                            GhostModuleRegistry.ids()) {

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
                                                            GhostModuleRegistry.ids()) {

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
         * /qisplan2 module item add <module>
         * /qisplan2 module item remove <module>
         * /qisplan2 module item info
         *
         * /qisplan2 module player info [targets]
         *
         * /qisplan2 module entity add <targets> <module>
         * /qisplan2 module entity remove <targets> <module>
         * /qisplan2 module entity info <targets>
         *
         * /qisplan2 module block add <x> <y> <z> <module>
         * /qisplan2 module block remove <x> <y> <z> <module>
         * /qisplan2 module block info <x> <y> <z>
         * ========================================================
         */

        root.then(
                Commands.literal("module")
                        .then(
                                Commands.literal("list")
                                        .executes(GhostCommands::moduleList)
                        )
                        .then(Commands.literal("player")
                                .executes(GhostCommands::modulePlayerInfoSelf)
                                .then(Commands.literal("info")
                                        .executes(GhostCommands::modulePlayerInfoSelf)
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(GhostCommands::modulePlayerInfo))))
                        .then(Commands.literal("item")
                                .then(Commands.literal("list").executes(GhostCommands::moduleList))
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
                        .then(
                                Commands.literal("block")
                                        .then(
                                                Commands.literal("add")
                                                        .then(
                                                                Commands.argument(
                                                                                "pos",
                                                                                BlockPosArgument.blockPos()
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
                                                                                                GhostCommands::moduleBlockAdd
                                                                                        )
                                                                        )
                                                        )
                                        )
                                        .then(
                                                Commands.literal("remove")
                                                        .then(
                                                                Commands.argument(
                                                                                "pos",
                                                                                BlockPosArgument.blockPos()
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
                                                                                                GhostCommands::moduleBlockRemove
                                                                                        )
                                                                        )
                                                        )
                                        )
                                        .then(
                                                Commands.literal("info")
                                                        .then(
                                                                Commands.argument(
                                                                                "pos",
                                                                                BlockPosArgument.blockPos()
                                                                        )
                                                                        .executes(
                                                                                GhostCommands::moduleBlockInfo
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
                        "module."
                                + ghost.getNamespace()
                                + "."
                                + ghost.getPath()
                ).getString();

        /*
         * 未知厉鬼
         */
        if (!GhostModuleRegistry.contains(
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
                        "module."
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
                PlayerGhostData.views(player);

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
                            "module."
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
                        "module."
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
                        "module."
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

        for (SpiritualPart type :
                SpiritualPart.playerCorrosionTargets()) {

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
            SpiritualPart target,
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
                            "module."
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
                CurseManager.getAll();

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
     * 模块命令统一使用翻译组件，格式化在客户端按当前语言完成。
     * 原版翻译参数只接受组件、数字、布尔值和字符串；资源标识、UUID 等须先转为字符串。
     * 保留嵌套组件，避免在服务端提前展开译名；使用新数组，不修改调用方参数。
     */
    private static MutableComponent moduleText(String key, Object... args) {
        Object[] safeArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object value = args[i];
            safeArgs[i] = value instanceof Component || value instanceof Number
                    || value instanceof Boolean || value instanceof String ? value : String.valueOf(value);
        }
        return Component.translatable("command.qisplan2.module." + key, safeArgs);
    }

    /** 模块名称翻译缺失时显示注册 ID，未知或旧存档模块也可以检查。 */
    private static Component moduleName(ResourceLocation id) {
        return Component.translatableWithFallback("module." + id.getNamespace() + "." + id.getPath(), id.toString());
    }

    /** 复用既有部位译名，自定义部位缺少翻译时保留完整标识。 */
    private static Component partName(SpiritualPart part) {
        return Component.translatableWithFallback("corrosion." + part.id().getNamespace() + "."
                + part.id().getPath(), part.id().toString());
    }

    /** 布尔状态也交给语言文件，不输出写死的中文或英文。 */
    private static Component moduleBoolean(boolean value) { return moduleText(value ? "yes" : "no"); }

    /** 列出所有模块定义；根 list 和 item list 共用，不读取物品实例。 */
    private static int moduleList(CommandContext<CommandSourceStack> context) {
        var modules = GhostModuleRegistry.getAll();
        context.getSource().sendSuccess(() -> moduleText("registered", modules.size()), false);
        for (var module : modules)
            context.getSource().sendSuccess(() -> moduleText("name", moduleName(module.getId()), module.getId()), false);
        return Math.max(1, modules.size());
    }

    /** 获取主手灵异物品，控制台或非灵异物品均给出可翻译提示。 */
    private static GhostItemModuleHost moduleItemHost(CommandSourceStack source) {
        var player = source.getPlayer();
        if (player == null) { source.sendFailure(moduleText("player_only")); return null; }
        var stack = player.getMainHandItem();
        if (stack.isEmpty() || !stack.is(ModTags.Items.SUPERNATURAL_ITEMS)) {
            source.sendFailure(moduleText("hold_item"));
            return null;
        }
        return new GhostItemModuleHost(stack, player);
    }

    /** 解析命令指定模块，未知标识统一报错。 */
    private static GhostModule commandModule(CommandContext<CommandSourceStack> context) {
        var id = ResourceLocationArgument.getId(context, "module");
        var module = GhostModuleRegistry.get(id);
        if (module == null) context.getSource().sendFailure(moduleText("unknown", id));
        return module;
    }

    /** 物品安装前区分重复模块与承载冲突，避免把占位失败误报为已安装。 */
    private static int moduleAdd(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        var module = commandModule(context);
        if (module == null) return 0;
        var host = moduleItemHost(source);
        if (host == null) return 0;
        if (host.hasModule(module.getId())) {
            source.sendFailure(moduleText("duplicate", module.getId())); return 0;
        }
        if (!host.canInstallModule(module.getId())) {
            source.sendFailure(moduleText("capacity", module.getId())); return 0;
        }
        if (!host.addModule(module)) {
            source.sendFailure(moduleText("install_failed", module.getId())); return 0;
        }
        var initial = module.getInitialData();
        source.sendSuccess(() -> moduleText("item_added", module.getId(), initial.intensity(), initial.revival()), true);
        return 1;
    }

    /** 从主手物品移除模块；占位随实际数据移除自动释放。 */
    private static int moduleRemove(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        var host = moduleItemHost(source);
        if (host == null) return 0;
        var id = ResourceLocationArgument.getId(context, "module");
        if (!host.removeModule(id)) {
            source.sendFailure(moduleText("remove_failed", id)); return 0;
        }
        source.sendSuccess(() -> moduleText("item_removed", id), true);
        return 1;
    }

    /** 显示主手物品实例，复用全部宿主的详细输出格式。 */
    private static int moduleInfo(CommandContext<CommandSourceStack> context) {
        var host = moduleItemHost(context.getSource());
        return host == null ? 0 : printModuleInfo(context.getSource(), host,
                moduleText("item_header", host.stack().getHoverName()));
    }

    /** 无目标时查看自己；控制台应使用带玩家选择器的版本。 */
    private static int modulePlayerInfoSelf(CommandContext<CommandSourceStack> context) {
        var player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(moduleText("player_target_required")); return 0;
        }
        return printModuleInfo(context.getSource(), new PlayerGhostModuleHost(player),
                moduleText("player_header", player.getDisplayName(), player.getUUID()));
    }

    /** 查询指定玩家自身的模块，不混入其背包或穿戴物品的模块。 */
    private static int modulePlayerInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var players = EntityArgument.getPlayers(context, "targets");
        for (var player : players)
            printModuleInfo(context.getSource(), new PlayerGhostModuleHost(player),
                    moduleText("player_header", player.getDisplayName(), player.getUUID()));
        return players.size();
    }

    /** 批量安装实体模块，分别统计不支持的实体与重复或容量不足的拒绝项。 */
    private static int moduleEntityAdd(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var source = context.getSource();
        var module = commandModule(context);
        if (module == null) return 0;
        int installed = 0, unsupported = 0, rejected = 0;
        for (var target : EntityArgument.getEntities(context, "targets")) {
            if (!(target instanceof ModularGhostEntity host)) unsupported++;
            else if (host.addModule(module)) installed++;
            else rejected++;
        }
        var result = moduleText("entity_added", module.getId(), installed, unsupported, rejected);
        if (installed == 0) source.sendFailure(result);
        else source.sendSuccess(() -> result, true);
        return installed;
    }

    /** 批量移除实体模块，目标不支持或没有该模块时仅计为跳过。 */
    private static int moduleEntityRemove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var source = context.getSource();
        var id = ResourceLocationArgument.getId(context, "module");
        int removed = 0, skipped = 0;
        for (var target : EntityArgument.getEntities(context, "targets")) {
            if (target instanceof ModularGhostEntity host && host.removeModule(id)) removed++;
            else skipped++;
        }
        var result = moduleText("entity_removed", id, removed, skipped);
        if (removed == 0) source.sendFailure(result);
        else source.sendSuccess(() -> result, true);
        return removed;
    }

    /** 查询实体模块及真实承载分配，不支持的选择目标不影响其他实体查询。 */
    private static int moduleEntityInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int count = 0;
        for (var target : EntityArgument.getEntities(context, "targets")) {
            if (!(target instanceof ModularGhostEntity host)) continue;
            printModuleInfo(context.getSource(), host, moduleText("entity_header", host.getDisplayName(), host.getUUID()));
            count++;
        }
        if (count == 0) context.getSource().sendFailure(moduleText("no_entities"));
        return count;
    }

    /** 为可变方块宿主安装模块，承载校验失败不修改已有数据。 */
    private static int moduleBlockAdd(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var source = context.getSource();
        var module = commandModule(context);
        if (module == null) return 0;
        var pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        if (!(source.getLevel().getBlockEntity(pos) instanceof GhostModuleDataModifier host)) {
            source.sendFailure(moduleText("block_not_mutable", pos.toShortString())); return 0;
        }
        if (host.hasModule(module.getId())) {
            source.sendFailure(moduleText("duplicate", module.getId())); return 0;
        }
        if (!host.canInstallModule(module.getId())) {
            source.sendFailure(moduleText("capacity", module.getId())); return 0;
        }
        if (!host.addModule(module)) {
            source.sendFailure(moduleText("install_failed", module.getId())); return 0;
        }
        var initial = module.getInitialData();
        source.sendSuccess(() -> moduleText("block_added", pos.toShortString(), module.getId(),
                initial.intensity(), initial.revival()), true);
        return 1;
    }

    /** 方块移除仍要求可变数据接口，静态方块只允许查询。 */
    private static int moduleBlockRemove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var source = context.getSource();
        var pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        var id = ResourceLocationArgument.getId(context, "module");
        if (!(source.getLevel().getBlockEntity(pos) instanceof GhostModuleDataModifier host)) {
            source.sendFailure(moduleText("block_not_mutable", pos.toShortString())); return 0;
        }
        if (!host.removeModule(id)) {
            source.sendFailure(moduleText("remove_failed", id)); return 0;
        }
        source.sendSuccess(() -> moduleText("block_removed", pos.toShortString(), id), true);
        return 1;
    }

    /** 同时支持有方块实体的宿主，以及鬼草丛、鬼地毯等静态方块宿主。 */
    private static int moduleBlockInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var source = context.getSource();
        var pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        var blockEntity = source.getLevel().getBlockEntity(pos);
        GhostModuleHost host = blockEntity instanceof GhostModuleHost modules ? modules
                : source.getLevel().getBlockState(pos).getBlock() instanceof GhostModuleHost modules ? modules : null;
        if (host == null) {
            source.sendFailure(moduleText("block_not_host", pos.toShortString())); return 0;
        }
        return printModuleInfo(source, host, moduleText("block_header", pos.toShortString()));
    }

    /**
     * 全部宿主共用详细信息输出。占位来自承载分配结果，不能将模块要求的皮肤误报为物品实际占据的部位。
     * 临时死机 tick、永久死机和裁决死机分别显示；规则就绪不代表当前能力媒介已经满足。
     * 独有数据放在悬浮文本里，避免较大的 NBT 直接刷满聊天栏。
     */
    private static int printModuleInfo(CommandSourceStack source, GhostModuleHost host, Component header) {
        var entries = host.getModules();
        var allocation = host.bearing();
        source.sendSuccess(() -> header, false);
        source.sendSuccess(() -> moduleText("summary", entries.size(), host.composition().capacities().size()), false);
        if (entries.isEmpty()) source.sendSuccess(() -> moduleText("empty"), false);
        for (var entry : entries) {
            var definition = GhostModuleRegistry.get(entry.id());
            boolean replacement = definition != null && definition.form().kind() == GhostModuleForm.Kind.REPLACEMENT;
            Component form = moduleText(definition == null ? "unknown_form" : replacement ? "replacement" : "independent");
            Component required = replacement ? partName(definition.form().part()) : moduleText("none");
            MutableComponent occupied = Component.empty();
            for (var slot : allocation.occupants().entrySet()) {
                if (!slot.getValue().contains(entry.id())) continue;
                if (!occupied.getSiblings().isEmpty()) occupied.append(moduleText("separator"));
                occupied.append(moduleText("slot", partName(slot.getKey()), slot.getValue().size(),
                        host.composition().capacities().get(slot.getKey())));
            }
            Component actual = occupied.getSiblings().isEmpty() ? moduleText("none") : occupied;
            boolean carried = allocation.accepted().contains(entry.id());
            Component status = moduleText(carried ? "carried" : definition == null ? "unregistered" : "unavailable");
            Component maximum = definition == null ? moduleText("unknown_value") : Component.literal(Double.toString(definition.getMaxRevival()));
            source.sendSuccess(() -> moduleText("name", moduleName(entry.id()), entry.id()), false);
            source.sendSuccess(() -> moduleText("values", entry.intensity(), entry.revival(), maximum), false);
            source.sendSuccess(() -> moduleText("bearing", form, required, actual, status), false);
            source.sendSuccess(() -> moduleText("states", moduleBoolean(entry.sleeping()),
                    moduleBoolean(entry.deadlocked()), GhostModuleCombatState.ticks(entry),
                    moduleBoolean(GhostModuleCombatState.permanent(entry)),
                    moduleBoolean(carried && GhostModuleCombatState.available(entry))), false);
            source.sendSuccess(() -> moduleText("custom_data", entry.customData().getAllKeys().size())
                    .withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            Component.literal(entry.customData().toString())))), false);
        }
        return Math.max(1, entries.size());
    }

    /*
     * ============================================================
     * 器官名称
     * ============================================================
     */

    private static String getCorrosionName(
            SpiritualPart type
    ) {
        return Component.translatable(
                "corrosion.qisplan2." +
                        type.name().toLowerCase(Locale.ROOT)
        ).getString();
    }
}