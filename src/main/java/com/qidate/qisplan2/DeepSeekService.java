package com.qidate.qisplan2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class DeepSeekService {

    // 使用 Java 11+ 内置的 HttpClient
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    // DeepSeek API 地址
    private static final String API_URL = "https://api.deepseek.com/chat/completions";

    /**
     * 异步发送消息给 DeepSeek，并返回一个 CompletableFuture 以便后续处理
     * @param userMessage 用户输入的消息
     * @param apiKey 从配置中读取的 API Key
     * @param modelName 从配置中读取的模型名称
     * @return 包含 AI 回复内容的 CompletableFuture
     */
    public static CompletableFuture<String> sendMessage(String userMessage, String apiKey, String modelName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. 构建请求体 (JSON)
                // 创建 messages 数组
                // 构建 messages 数组
                ArrayNode messagesArray = MAPPER.createArrayNode();

                // 添加 system 提示
                ObjectNode systemMessage = MAPPER.createObjectNode();
                systemMessage.put("role", "system");
                systemMessage.put("content",
                        "你是一个 Minecraft 助手。用户会向你许愿，你需要理解他们的愿望，并返回一个 JSON 对象，包含 action 和 params。\n" +
                                "当前支持的 action：\n" +
                                "1. weather: 改变天气，params 包含 weather 字段，值为 'clear'、'rain' 或 'thunder'。\n" +
                                "2. time: 设置时间，params 包含 time 字段，值为 'day'、'night' 或数字（0-24000）。\n" +
                                "3. say: 发送聊天消息，params 包含 message 字段。\n" +
                                "4. give: 给予物品，params 包含 item 和 count（可选）。\n" +
                                "5. teleport: 传送，params 包含 x, y, z。\n" +
                                "如果无法理解愿望，返回 {\"action\": \"say\", \"params\": {\"message\": \"抱歉，我无法执行该愿望\"}}。\n" +
                                "只返回 JSON，不要有其他文字。"
                );
                messagesArray.add(systemMessage);

                // 然后添加用户消息（原有代码）
                ObjectNode userMessageNode = MAPPER.createObjectNode();
                userMessageNode.put("role", "user");
                userMessageNode.put("content", userMessage);
                messagesArray.add(userMessageNode);

                // 构建根节点
                ObjectNode requestBody = MAPPER.createObjectNode();
                requestBody.put("model", modelName); // 使用配置的模型
                requestBody.set("messages", messagesArray);
                // 可选：设置温度等参数，这里保持默认
                // requestBody.put("temperature", 0.7);

                String jsonBody = MAPPER.writeValueAsString(requestBody);

                // 2. 创建 HTTP 请求
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey) // API Key 认证
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                // 3. 发送请求并获取响应
                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                // 4. 检查响应状态并解析
                if (response.statusCode() == 200) {
                    // 解析 JSON 响应，提取 AI 的回复内容
                    // 响应结构: {"choices":[{"message":{"content":"回复内容"}}]}
                    var rootNode = MAPPER.readTree(response.body());
                    return rootNode.path("choices").path(0).path("message").path("content").asText();
                } else {
                    // 记录错误日志，方便调试
                    QisPlan2.LOGGER.error("DeepSeek API 请求失败，状态码: {}, 响应: {}", response.statusCode(), response.body());
                    return "请求失败: " + response.statusCode();
                }

            } catch (Exception e) {
                QisPlan2.LOGGER.error("与 DeepSeek API 通信时发生错误", e);
                return "出错了: " + e.getMessage();
            }
        });
    }
}