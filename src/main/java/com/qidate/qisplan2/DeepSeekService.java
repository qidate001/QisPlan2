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
                ArrayNode messagesArray = MAPPER.createArrayNode();
                // 添加用户消息
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