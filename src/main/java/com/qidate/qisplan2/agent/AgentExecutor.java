package com.qidate.qisplan2.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.qidate.qisplan2.QisPlan2;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class AgentExecutor {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void execute(String reply, CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        if (!level.getGameRules().getBoolean(QisPlan2.ISAY_ENABLED)) {
            source.sendFailure(Component.literal("§c/isay 功能已被管理员禁用"));
            return;
        }

        try {
            ObjectNode root = (ObjectNode) MAPPER.readTree(reply);
            String action = root.path("action").asText();
            ObjectNode params = (ObjectNode) root.path("params");

            source.getServer().execute(() -> {
                switch (action) {
                    case "weather" -> source.getServer().getCommands()
                            .performPrefixedCommand(source, "weather " + params.path("weather").asText());
                    case "time" -> source.getServer().getCommands()
                            .performPrefixedCommand(source, "time set " + params.path("time").asText());
                    case "say" -> {
                        String msg = params.path("message").asText();
                        source.sendSuccess(() -> Component.literal("[DeepSeek] " + msg), false);
                    }
                    case "give" -> {
                        String item = params.path("item").asText();
                        int count = params.path("count").asInt(1);
                        source.getServer().getCommands()
                                .performPrefixedCommand(source, "give @s " + item + " " + count);
                    }
                    case "teleport" -> {
                        double x = params.path("x").asDouble();
                        double y = params.path("y").asDouble();
                        double z = params.path("z").asDouble();
                        source.getServer().getCommands()
                                .performPrefixedCommand(source, "tp @s " + x + " " + y + " " + z);
                    }
                    default -> source.sendFailure(Component.literal("§c未知动作: " + action));
                }
            });
        } catch (Exception e) {
            // 解析失败，当作普通文本回复
            source.sendSuccess(() -> Component.literal("§a[DeepSeek] " + reply), false);
        }
    }
}