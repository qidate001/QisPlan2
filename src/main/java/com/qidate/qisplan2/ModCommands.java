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

import java.util.concurrent.CompletableFuture;

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

        // 1. 从配置中读取 API Key 和模型名称
        String apiKey = QisConfig.CLIENT.API_KEY.get();
        String modelName = QisConfig.CLIENT.MODEL_NAME.get();

        // 2. 检查 API Key 是否已配置
        if (apiKey == null || apiKey.isEmpty()) {
            source.sendFailure(Component.literal("§c错误: 请先在模组配置中设置 API Key！"));
            return 0;
        }

        // 3. 给玩家一个反馈，表示指令已被接收
        source.sendSuccess(() -> Component.literal("§e正在思考，请稍候..."), false);

        // 4. 异步调用 DeepSeek API
        CompletableFuture<String> future = DeepSeekService.sendMessage(message, apiKey, modelName);

        // 5. 当 API 响应完成时，处理结果
        future.thenAcceptAsync(reply -> {
            // 在游戏主线程中发送消息
            source.sendSuccess(() -> Component.literal("§a[DeepSeek] " + reply), false);
        }).exceptionally(throwable -> {
            // 处理可能出现的异常
            source.sendFailure(Component.literal("§c处理请求时发生错误: " + throwable.getMessage()));
            return null;
        });

        return 1; // 命令执行成功
    }
}