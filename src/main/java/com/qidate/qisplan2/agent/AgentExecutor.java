package com.qidate.qisplan2.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.qidate.qisplan2.QisPlan2;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public class AgentExecutor {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void execute(String reply, CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        if (level == null) {
            source.sendFailure(Component.literal("§c该命令只能在游戏内执行"));
            return;
        }
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
                    case "weather" -> {
                        String weather = params.path("weather").asText();
                        changeWeather(level, weather);
                        source.sendSuccess(() -> Component.literal("§a天气已改变为: " + weather), false);
                    }
                    case "time" -> {
                        String time = params.path("time").asText();
                        setTime(level, time);
                        source.sendSuccess(() -> Component.literal("§a时间已设置为: " + time), false);
                    }
                    case "say" -> {
                        String msg = params.path("message").asText();
                        source.getServer().getPlayerList().broadcastSystemMessage(
                                Component.literal("§d[DeepSeek] " + msg), false
                        );
                    }
                    case "give" -> {
                        ServerPlayer player = source.getEntity() instanceof ServerPlayer p ? p : null;
                        if (player == null) {
                            source.sendFailure(Component.literal("§c该操作需要玩家身份"));
                            return;
                        }
                        String itemName = params.path("item").asText();
                        int count = params.path("count").asInt(1);
                        giveItem(player, itemName, count);
                        source.sendSuccess(() -> Component.literal("§a已给予 " + count + " 个 " + itemName), false);
                    }
                    case "teleport" -> {
                        ServerPlayer player = source.getEntity() instanceof ServerPlayer p ? p : null;
                        if (player == null) {
                            source.sendFailure(Component.literal("§c该操作需要玩家身份"));
                            return;
                        }
                        double x = params.path("x").asDouble();
                        double y = params.path("y").asDouble();
                        double z = params.path("z").asDouble();
                        teleportPlayer(player, x, y, z);
                        source.sendSuccess(() -> Component.literal("§a已传送至 (" + x + ", " + y + ", " + z + ")"), false);
                    }
                    default -> source.sendFailure(Component.literal("§c未知动作: " + action));
                }
            });
        } catch (Exception e) {
            // 解析失败，当作普通文本回复
            source.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("§d[DeepSeek] " + reply), false
            );
        }
    }

    // ========== 具体操作实现 ==========

    private static void changeWeather(ServerLevel level, String weather) {
        switch (weather.toLowerCase()) {
            case "clear" -> level.setWeatherParameters(0, 6000, false, false);
            case "rain" -> level.setWeatherParameters(0, 6000, true, false);
            case "thunder" -> level.setWeatherParameters(0, 6000, true, true);
            default -> level.setWeatherParameters(0, 6000, false, false);
        }
    }

    private static void setTime(ServerLevel level, String time) {
        long dayTime = switch (time.toLowerCase()) {
            case "day" -> 1000;
            case "noon" -> 6000;
            case "night" -> 13000;
            case "midnight" -> 18000;
            default -> {
                try {
                    yield Long.parseLong(time);
                } catch (NumberFormatException e) {
                    yield 0L;
                }
            }
        };
        level.setDayTime(dayTime);
    }

    private static void giveItem(ServerPlayer player, String itemName, int count) {
        // 处理物品 ID（支持命名空间缩写）
        ResourceLocation itemId;
        if (itemName.contains(":")) {
            itemId = ResourceLocation.parse(itemName);
        } else {
            itemId = ResourceLocation.parse("minecraft:" + itemName);
        }

        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null || item == BuiltInRegistries.ITEM.get(ResourceLocation.parse("minecraft:air"))) {
            player.sendSystemMessage(Component.literal("§c未找到物品: " + itemName));
            return;
        }

        ItemStack stack = new ItemStack(item, Math.min(count, 64));
        boolean added = player.getInventory().add(stack);
        if (!added) {
            // 如果背包满了，丢到脚下
            player.drop(stack, false);
        }
    }

    private static void teleportPlayer(ServerPlayer player, double x, double y, double z) {
        player.teleportTo(x, y, z);
    }
}