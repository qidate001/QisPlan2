package com.qidate.qisplan2.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.agent.AgentExecutor;
import com.qidate.qisplan2.agent.DeepSeekService;
import com.qidate.qisplan2.core.QisConfig;
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

        // 检查游戏规则（提前拦截，避免无谓的 API 调用）
        var level = source.getLevel();
        if (level == null) {
            source.sendFailure(Component.literal("§c该命令只能由玩家执行"));
            return 0;
        }
        if (!level.getGameRules().getBoolean(QisPlan2.ISAY_ENABLED)) {
            source.sendFailure(Component.literal("§c/isay 功能已被管理员禁用"));
            return 0;
        }

        // 检查 API Key
        String apiKey = QisConfig.CLIENT.API_KEY.get();
        String modelName = QisConfig.CLIENT.MODEL_NAME.get();
        if (apiKey == null || apiKey.isEmpty()) {
            source.sendFailure(Component.literal("§c错误: 请先在模组配置中设置 API Key！"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("§e正在思考，请稍候..."), false);

        // 异步请求 DeepSeek
        CompletableFuture<String> future = DeepSeekService.sendMessage(message, apiKey, modelName, DeepSeekService.PromptProfile.WISH_GHOST);
        future.thenAcceptAsync(reply -> AgentExecutor.execute(reply, source))
                .exceptionally(throwable -> {
                    source.sendFailure(Component.literal("§c处理请求时发生错误: " + throwable.getMessage()));
                    return null;
                });

        return 1;
    }
}