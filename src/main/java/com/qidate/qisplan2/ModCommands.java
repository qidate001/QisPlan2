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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = QisPlan2.MODID)
public class ModCommands {
    private static final ObjectMapper MAPPER = new ObjectMapper();

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

        String apiKey = QisConfig.CLIENT.API_KEY.get();
        String modelName = QisConfig.CLIENT.MODEL_NAME.get();

        if (apiKey == null || apiKey.isEmpty()) {
            source.sendFailure(Component.literal("§c错误: 请先在模组配置中设置 API Key！"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("§e正在思考，请稍候..."), false);

        CompletableFuture<String> future = DeepSeekService.sendMessage(message, apiKey, modelName);

        future.thenAcceptAsync(reply -> {
            try {
                // 解析 JSON
                ObjectNode root = (ObjectNode) MAPPER.readTree(reply);
                String action = root.path("action").asText();
                ObjectNode params = (ObjectNode) root.path("params");

                // 所有游戏操作必须在主线程执行
                source.getServer().execute(() -> {
                    switch (action) {
                        case "weather" -> {
                            String weather = params.path("weather").asText();
                            // 直接使用 source 执行命令
                            source.getServer().getCommands().performPrefixedCommand(
                                    source,
                                    "weather " + weather
                            );
                        }
                        case "time" -> {
                            String time = params.path("time").asText();
                            source.getServer().getCommands().performPrefixedCommand(
                                    source,
                                    "time set " + time
                            );
                        }
                        case "say" -> {
                            String msg = params.path("message").asText();
                            source.sendSuccess(() -> Component.literal("[DeepSeek] " + msg), false);
                        }
                        case "give" -> {
                            String item = params.path("item").asText();
                            int count = params.path("count").asInt(1);
                            source.getServer().getCommands().performPrefixedCommand(
                                    source,
                                    "give @s " + item + " " + count
                            );
                        }
                        case "teleport" -> {
                            double x = params.path("x").asDouble();
                            double y = params.path("y").asDouble();
                            double z = params.path("z").asDouble();
                            source.getServer().getCommands().performPrefixedCommand(
                                    source,
                                    "tp @s " + x + " " + y + " " + z
                            );
                        }
                        default -> source.sendFailure(Component.literal("§c未知动作: " + action));
                    }
                });
            } catch (Exception e) {
                // 解析失败，当作普通文本回复
                source.sendSuccess(() -> Component.literal("§a[DeepSeek] " + reply), false);
            }
        }).exceptionally(throwable -> {
            source.sendFailure(Component.literal("§c处理请求时发生错误: " + throwable.getMessage()));
            return null;
        });

        return 1;
    }
}