package com.qidate.qisplan2;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = QisPlan2.MODID)
public class ModCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("isay")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ModCommands::executeIsay)
                )
        );
    }

    private static int executeIsay(CommandContext<CommandSourceStack> context) {
        String message = StringArgumentType.getString(context, "message");
        CommandSourceStack source = context.getSource();

        // 输出到控制台（IDEA 的 Run 窗口或日志）
        QisPlan2.LOGGER.info("[玩家消息] " + message);

        // 可选：给执行者一个反馈
        source.sendSuccess(() -> Component.literal("消息已发送至控制台: " + message), true);

        return 1; // 命令执行成功
    }
}