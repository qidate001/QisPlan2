package com.qidate.qisplan2.client;

import com.qidate.qisplan2.agent.DeepSeekService;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GhostBookScreen extends Screen {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    "qisplan2",
                    "textures/gui/ghost_book.png"
            );

    /*
     * 鬼书贴图尺寸。
     */
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    /*
     * 书页上的文字区域。
     */
    private static final int TEXT_LEFT = 34;
    private static final int TEXT_TOP = 42;
    private static final int TEXT_WIDTH = 188;

    /*
     * 输入框。
     */
    private static final int INPUT_WIDTH = 300;
    private static final int INPUT_HEIGHT = 20;

    /*
     * DeepSeek 配置。
     */
    private final String apiKey;
    private final String modelName;

    /*
     * 输入框。
     */
    private EditBox inputBox;

    /*
     * 书页上的对话。
     *
     * 这里保存的是已经显示的内容。
     */
    private final List<ChatEntry> chatHistory =
            new ArrayList<>();

    /*
     * 当前是否正在等待 AI。
     */
    private boolean waitingForResponse = false;

    /*
     * 当前正在显示的 AI 回复。
     */
    private String currentResponse = "";

    private int left;
    private int top;


    public GhostBookScreen(
            String apiKey,
            String modelName
    ) {
        super(
                Component.translatable(
                        "screen.qisplan2.ghost_book"
                )
        );

        this.apiKey = apiKey;
        this.modelName = modelName;
    }


    @Override
    protected void init() {

        super.init();

        left = (this.width - TEXTURE_WIDTH) / 2;
        top = (this.height - TEXTURE_HEIGHT) / 2;

        /*
         * 输入框放在书页底部。
         */
        int inputX = left + 28;
        int inputY = top + 218;

        inputBox = new EditBox(
                this.font,
                inputX,
                inputY,
                200,
                18,
                Component.translatable(
                        "qisplan2.ghost_book.input"
                )
        );

        inputBox.setMaxLength(500);

        inputBox.setHint(
                Component.translatable(
                        "qisplan2.ghost_book.input_hint"
                )
        );

        /*
         * 输入框样式。
         *
         * 关闭默认背景，
         * 这样它不会出现一个突兀的黑色矩形。
         */
        inputBox.setBordered(false);

        /*
         * 设置文本颜色。
         */
        inputBox.setTextColor(0xFF222222);

        /*
         * 设置光标颜色。
         */
        inputBox.setCursorPosition(0);

        addRenderableWidget(inputBox);

        /*
         * 默认获取焦点。
         */
        setInitialFocus(inputBox);

        inputBox.setFocused(true);
    }


    /**
     * 按 Enter 发送。
     */
    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {

        /*
         * 回车发送。
         */
        if ((keyCode == 257 || keyCode == 335)
                && inputBox != null
                && inputBox.isFocused()) {

            sendMessage();

            return true;
        }

        return super.keyPressed(
                keyCode,
                scanCode,
                modifiers
        );
    }


    /**
     * 发送玩家消息。
     */
    private void sendMessage() {

        if (waitingForResponse) {
            return;
        }

        String message =
                inputBox.getValue()
                        .trim();

        if (message.isEmpty()) {
            return;
        }

        /*
         * 清空输入框。
         */
        inputBox.setValue("");

        /*
         * 记录玩家消息。
         */
        chatHistory.add(
                new ChatEntry(
                        true,
                        message
                )
        );

        /*
         * 开始等待。
         */
        waitingForResponse = true;

        currentResponse =
                "……";


        /*
         * ========================================
         * 异步请求 DeepSeek
         * ========================================
         */
        CompletableFuture<String> future =
                DeepSeekService.sendMessage(
                        message,
                        apiKey,
                        modelName,
                        DeepSeekService.PromptProfile.GHOST_BOOK
                );


        future.whenComplete(
                (response, throwable) -> {

                    /*
                     * 切回 Minecraft 客户端线程。
                     */
                    Minecraft.getInstance().execute(
                            () -> {

                                waitingForResponse = false;

                                if (throwable != null) {

                                    currentResponse =
                                            "鬼书暂时没有回应。";

                                    chatHistory.add(
                                            new ChatEntry(
                                                    false,
                                                    currentResponse
                                            )
                                    );

                                    return;
                                }

                                if (response == null
                                        || response.isBlank()) {

                                    currentResponse =
                                            "……";

                                } else {

                                    currentResponse =
                                            response;
                                }

                                chatHistory.add(
                                        new ChatEntry(
                                                false,
                                                currentResponse
                                        )
                                );
                            }
                    );
                }
        );
    }


    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        /*
         * 先压暗游戏画面。
         */
        graphics.fill(
                0,
                0,
                this.width,
                this.height,
                0x99000000
        );

        /*
         * Screen 自己处理背景、模糊等。
         */
        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );

        /*
         * 绘制鬼书。
         */
        graphics.blit(
                TEXTURE,
                left,
                top,
                0,
                0,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT
        );

        /*
         * 绘制书页文字。
         */
        drawBookText(graphics);

        /*
         * 最后绘制输入框，
         * 保证输入文字和光标位于鬼书之上。
         */
        if (inputBox != null) {
            inputBox.render(
                    graphics,
                    mouseX,
                    mouseY,
                    partialTick
            );
        }
    }


    /**
     * 在书页上绘制对话。
     */
    private void drawBookText(
            GuiGraphics graphics
    ) {

        /*
         * 最近的消息优先显示。
         *
         * 我们最多取最后 4 条，
         * 防止把书页挤满。
         */
        int maxMessages = 4;

        int start =
                Math.max(
                        0,
                        chatHistory.size()
                                - maxMessages
                );

        int y =
                top + TEXT_TOP;

        for (int i = start;
             i < chatHistory.size();
             i++) {

            ChatEntry entry =
                    chatHistory.get(i);

            String prefix =
                    entry.player
                            ? "你："
                            : "鬼：";

            int color =
                    entry.player
                            ? 0xFF555555
                            : 0xFF222222;

            List<String> lines =
                    font.getSplitter()
                            .splitLines(
                                    prefix
                                            + entry.text,
                                    TEXT_WIDTH,
                                    net.minecraft.network.chat.Style.EMPTY
                            )
                            .stream()
                            .map(
                                    formattedText ->
                                            formattedText
                                                    .getString()
                            )
                            .toList();

            for (String line : lines) {

                graphics.drawString(
                        font,
                        line,
                        left + TEXT_LEFT,
                        y,
                        color,
                        false
                );

                y += 10;

                /*
                 * 超出书页就不继续画。
                 */
                if (y >
                        top
                                + TEXTURE_HEIGHT
                                - 32) {

                    return;
                }
            }

            y += 4;
        }

        /*
         * 正在等待 AI。
         */
        if (waitingForResponse) {

            graphics.drawString(
                    font,
                    "鬼书正在回应……",
                    left + TEXT_LEFT,
                    top + TEXTURE_HEIGHT  - 30,
                    0xFF444444,
                    false
            );
        }
    }


    @Override
    public boolean isPauseScreen() {
        return false;
    }


    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }


    private record ChatEntry(
            boolean player,
            String text
    ) {
    }
}