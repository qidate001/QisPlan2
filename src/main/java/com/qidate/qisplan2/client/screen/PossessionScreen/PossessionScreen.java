package com.qidate.qisplan2.client.screen.PossessionScreen;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.client.curse.ClientCurseState;
import com.qidate.qisplan2.client.curse.CurseClientRegistry;
import com.qidate.qisplan2.client.key.ModKeyMappings;
import com.qidate.qisplan2.core.ModAttachments;
import com.qidate.qisplan2.core.QisConfig;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostState;
import com.qidate.qisplan2.ghost.possession.manager.GhostSuppressionAllocationHandler;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import com.qidate.qisplan2.ghost.possession.ability.GhostAbilityRegistry;
import com.qidate.qisplan2.ghost.possession.ability.PossessedGhostAbility;
import com.qidate.qisplan2.ghost.corrosion.CorrosionMatrix;
import com.qidate.qisplan2.ghost.corrosion.CorrosionType;
import com.qidate.qisplan2.ghost.possession.manager.SuppressionAllocation;
import com.qidate.qisplan2.ghost.possession.suppression.GhostSuppressionSystem;
import com.qidate.qisplan2.network.possession.GhostPossessionNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

public class PossessionScreen extends Screen {

    private static final int PANEL_WIDTH = 600;
    private static final int PANEL_HEIGHT = 320;

    private float panelScale = 1.0F;

    private int panelX;
    private int panelY;

    private static final int GHOST_WORKBENCH_CARD_WIDTH = 172;
    private static final int GHOST_WORKBENCH_CARD_HEIGHT = 76;

    private int currentPage = 0;

    private boolean draggingSuppression = false;

    private ResourceLocation draggingSourceGhost = null;

    private ResourceLocation draggingOriginalTargetGhost = null;

    private int draggingSlotIndex = -1;

    private int draggingMouseX = 0;
    private int draggingMouseY = 0;

    public static final ResourceLocation DEFAULT_SUPPRESSION_ICON =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "textures/gui/suppression.png"
            );

    private final PossessionStatusRenderer statusRenderer;



    public PossessionScreen() {

        super(
                Component.literal(
                        "驭鬼者状态"
                )
        );

        statusRenderer =
                new PossessionStatusRenderer();
    }

    @Override
    protected void init() {

        panelScale = Math.min(
                (float) this.width / (PANEL_WIDTH + 2),
                (float) this.height / (PANEL_HEIGHT + 2)
        );

        int scaledWidth =
                Math.round(PANEL_WIDTH * panelScale);

        int scaledHeight =
                Math.round(PANEL_HEIGHT * panelScale);

        panelX =
                (this.width - scaledWidth) / 2;

        panelY =
                (this.height - scaledHeight) / 2;
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        this.renderBackground(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );

        graphics.pose().pushPose();

        graphics.pose().translate(
                panelX,
                panelY,
                0
        );

        graphics.pose().scale(
                panelScale,
                panelScale,
                1.0F
        );

        drawPanel(graphics);
        drawTabs(graphics);

        if (currentPage == 0) {

            statusRenderer.render(
                    graphics,
                    panelX,
                    panelY,
                    panelScale
            );

        } else {

            drawSuppressionPage(
                    graphics
            );
        }

        if (draggingSuppression) {
            drawDraggingSuppression(
                    graphics
            );
        }

        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {

        if (button != 0) {
            return super.mouseClicked(
                    mouseX,
                    mouseY,
                    button
            );
        }

        /*
         * 将屏幕坐标转换为面板局部坐标。
         */
        double localX =
                (mouseX - panelX) / panelScale;

        double localY =
                (mouseY - panelY) / panelScale;

        /**
         * 状态页的滚动条。
         */
        if (currentPage == 0) {

            if (
                    statusRenderer.mouseClicked(
                            localX,
                            localY
                    )
            ) {
                return true;
            }
        }

        /*
         * 灵异页的压制交互。
         */
        if (
                currentPage == 1
                        && button == 0
        ) {

            Minecraft minecraft =
                    Minecraft.getInstance();

            if (minecraft.player != null) {

                Map<ResourceLocation, PossessedGhostState> ghosts =
                        minecraft.player.getData(
                                ModAttachments.POSSESSED_GHOSTS
                        );

                int index = 0;

                for (var entry : ghosts.entrySet()) {

                    ResourceLocation ghostId =
                            entry.getKey();

                    PossessedGhostAbility ability =
                            GhostAbilityRegistry.get(
                                    ghostId
                            );

                    if (ability == null) {
                        index++;
                        continue;
                    }

                    int cardX =
                            getWorkbenchCardX(index);

                    int cardY =
                            getWorkbenchCardY(index);

                    /*
                     * 先检查已经分配出去的眼睛。
                     *
                     * 已经被拖走的 slot 不能再次从原槽位取出，
                     * 但可以从目标鬼卡片上重新抓起。
                     */
                    SuppressionAllocationHit allocated =
                            getAllocatedSuppressionAt(
                                    localX,
                                    localY,
                                    ghostId,
                                    cardX,
                                    cardY
                            );

                    if (allocated != null) {

                        draggingSuppression = true;

                        draggingSourceGhost =
                                allocated.sourceGhost();

                        draggingOriginalTargetGhost =
                                allocated.targetGhost();

                        draggingSlotIndex =
                                allocated.allocation().slotIndex();

                        draggingMouseX =
                                (int) localX;

                        draggingMouseY =
                                (int) localY;

                        return true;
                    }

                    /*
                     * 再检查 Ghost Eye 自己的压制槽位。
                     */
                    int slot =
                            getSuppressionSlotAt(
                                    localX,
                                    localY,
                                    cardX,
                                    cardY,
                                    ability.suppressionUnits()
                            );

                    if (slot >= 0) {

                        /*
                         * 已经被分配出去的 slot
                         * 不能再次使用。
                         */
                        if (
                                GhostSuppressionAllocationHandler.isSlotAllocated(
                                        minecraft.player,
                                        ghostId,
                                        slot
                                )
                        ) {
                            return true;
                        }

                        draggingSuppression = true;

                        draggingOriginalTargetGhost =
                                null;

                        draggingSourceGhost =
                                ghostId;

                        draggingSlotIndex =
                                slot;

                        draggingMouseX =
                                (int) localX;

                        draggingMouseY =
                                (int) localY;

                        return true;
                    }

                    index++;
                }
            }
        }

        int tabY = 24;
        int tabWidth = 70;
        int tabHeight = 20;

        int statusX = 12;
        int suppressionX =
                statusX + tabWidth + 4;

        /*
         * 状态页。
         */
        if (isInside(
                localX,
                localY,
                statusX,
                tabY,
                tabWidth,
                tabHeight
        )) {

            currentPage = 0;
            return true;
        }

        /*
         * 灵异页。
         */
        if (isInside(
                localX,
                localY,
                suppressionX,
                tabY,
                tabWidth,
                tabHeight
        )) {

            currentPage = 1;
            return true;
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY
    ) {

        if (currentPage == 0) {

            double localX =
                    (mouseX - panelX)
                            / panelScale;

            double localY =
                    (mouseY - panelY)
                            / panelScale;

            if (
                    statusRenderer.mouseScrolled(
                            localX,
                            localY,
                            scrollY
                    )
            ) {
                return true;
            }
        }

        return super.mouseScrolled(
                mouseX,
                mouseY,
                scrollX,
                scrollY
        );
    }

    @Override
    public boolean mouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY
    ) {

        /*
         * 将屏幕坐标转换为面板局部坐标。
         */
        double localX =
                (mouseX - panelX)
                        / panelScale;

        double localY =
                (mouseY - panelY)
                        / panelScale;

        /*
         * =========================================================
         * 滚动条拖动
         * =========================================================
         */
        if (
                currentPage == 0
                        && button == 0
        ) {

            if (
                    statusRenderer.mouseDragged(
                            localX,
                            localY
                    )
            ) {
                return true;
            }
        }

        // =========================================================
        // 压制拖动
        // =========================================================

        if (
                draggingSuppression
                        && button == 0
        ) {

            draggingMouseX =
                    (int) localX;

            draggingMouseY =
                    (int) localY;

            return true;
        }

        return super.mouseDragged(
                mouseX,
                mouseY,
                button,
                dragX,
                dragY
        );
    }

    @Override
    public boolean mouseReleased(
            double mouseX,
            double mouseY,
            int button
    ) {

        /*
         * =========================================================
         * 状态页滚动条
         * =========================================================
         */
        if (
                currentPage == 0
                        && button == 0
        ) {

            if (
                    statusRenderer.mouseReleased()
            ) {
                return true;
            }
        }

        if (
                draggingSuppression
                        && button == 0
        ) {

            double localX =
                    (mouseX - panelX)
                            / panelScale;

            double localY =
                    (mouseY - panelY)
                            / panelScale;

            ResourceLocation targetGhost =
                    getWorkbenchGhostAt(
                            localX,
                            localY
                    );

            Minecraft minecraft =
                    Minecraft.getInstance();

            if (minecraft.player != null) {

                /*
                 * =====================================================
                 * 情况 1：
                 *
                 * 原本就是一个已经分配出去的压制额度。
                 * =====================================================
                 */
                if (draggingOriginalTargetGhost != null) {

                    /*
                     * -------------------------------------------------
                     * 情况 1-A：
                     *
                     * 拖回自己的来源鬼。
                     *
                     * → 删除原分配
                     * → slot 自动重新成为来源鬼的空闲槽位
                     * -------------------------------------------------
                     */
                    if (
                            targetGhost != null
                                    && targetGhost.equals(
                                    draggingSourceGhost
                            )
                    ) {

                        QisPlan2.LOGGER.info(
                                "[灵异配平] 撤回压制额度：{} 的 slot {} ← {}",
                                draggingSourceGhost,
                                draggingSlotIndex,
                                draggingOriginalTargetGhost
                        );

                        GhostPossessionNetwork
                                .sendSuppressionRemove(
                                        draggingSourceGhost,
                                        draggingOriginalTargetGhost,
                                        draggingSlotIndex
                                );
                    }

                    /*
                     * -------------------------------------------------
                     * 情况 1-B：
                     *
                     * 拖到了其他鬼。
                     *
                     * → 移动分配
                     * -------------------------------------------------
                     */
                    else if (
                            targetGhost != null
                                    && !targetGhost.equals(
                                    draggingSourceGhost
                            )
                    ) {

                        /*
                         * 目标鬼必须能够被来源鬼的灵异压制。
                         */
                        if (
                                !canSuppress(
                                        draggingSourceGhost,
                                        targetGhost
                                )
                        ) {

                            QisPlan2.LOGGER.info(
                                    "[灵异配平] {} 无法压制 {}，拒绝移动",
                                    draggingSourceGhost,
                                    targetGhost
                            );

                            /*
                             * 不发送任何网络请求。
                             *
                             * 原来的分配保持不变。
                             */
                            draggingSuppression = false;
                            draggingSourceGhost = null;
                            draggingOriginalTargetGhost = null;
                            draggingSlotIndex = -1;

                            return true;
                        }

                        int targetIndex = 0;

                        for (
                                ResourceLocation ghostId
                                : minecraft.player
                                .getData(
                                        ModAttachments.POSSESSED_GHOSTS
                                )
                                .keySet()
                        ) {

                            if (ghostId.equals(targetGhost)) {
                                break;
                            }

                            targetIndex++;
                        }

                        int targetCardX =
                                getWorkbenchCardX(
                                        targetIndex
                                );

                        int targetCardY =
                                getWorkbenchCardY(
                                        targetIndex
                                );

                        double relativeX =
                                localX - targetCardX;

                        double relativeY =
                                localY - targetCardY;

                        QisPlan2.LOGGER.info(
                                "[灵异配平] 移动压制额度：{} 的 slot {}：{} → {}，位置=({}, {})",
                                draggingSourceGhost,
                                draggingSlotIndex,
                                draggingOriginalTargetGhost,
                                targetGhost,
                                relativeX,
                                relativeY
                        );

                        /*
                         * 这里不能直接调用 allocate()。
                         *
                         * 因为这个 slot 原本已经存在。
                         *
                         * 因此需要：
                         * 原目标 → 新目标
                         */
                        GhostPossessionNetwork
                                .sendSuppressionMove(
                                        draggingSourceGhost,
                                        draggingOriginalTargetGhost,
                                        targetGhost,
                                        draggingSlotIndex,
                                        relativeX,
                                        relativeY
                                );
                    }

                    /*
                     * -------------------------------------------------
                     * 情况 1-C：
                     *
                     * 拖到了原来的目标鬼卡片。
                     *
                     * → 仍然是移动
                     * → 只是目标没变
                     * → 更新图标位置
                     * -------------------------------------------------
                     */
                    else if (
                            targetGhost != null
                                    && targetGhost.equals(
                                    draggingOriginalTargetGhost
                            )
                    ) {

                        int targetIndex = 0;

                        for (
                                ResourceLocation ghostId
                                : minecraft.player
                                .getData(
                                        ModAttachments.POSSESSED_GHOSTS
                                )
                                .keySet()
                        ) {

                            if (ghostId.equals(targetGhost)) {
                                break;
                            }

                            targetIndex++;
                        }

                        int targetCardX =
                                getWorkbenchCardX(
                                        targetIndex
                                );

                        int targetCardY =
                                getWorkbenchCardY(
                                        targetIndex
                                );

                        double relativeX =
                                localX - targetCardX;

                        double relativeY =
                                localY - targetCardY;

                        QisPlan2.LOGGER.info(
                                "[灵异配平] 调整压制额度位置：{} 的 slot {} → {}，位置=({}, {})",
                                draggingSourceGhost,
                                draggingSlotIndex,
                                targetGhost,
                                relativeX,
                                relativeY
                        );

                        GhostPossessionNetwork
                                .sendSuppressionMove(
                                        draggingSourceGhost,
                                        draggingOriginalTargetGhost,
                                        targetGhost,
                                        draggingSlotIndex,
                                        relativeX,
                                        relativeY
                                );
                    }

                    /*
                     * -------------------------------------------------
                     * 情况 1-D：
                     *
                     * targetGhost == null
                     *
                     * → 什么都不做
                     * → 原来的分配保持不变
                     * -------------------------------------------------
                     */
                }

                /*
                 * =====================================================
                 * 情况 2：
                 *
                 * 从一个尚未分配的空闲 slot 拖出来。
                 * =====================================================
                 */
                else {

                    /*
                     * 只有拖到其他鬼卡片才建立分配。
                     */
                    if (
                            targetGhost != null
                                    && !targetGhost.equals(
                                    draggingSourceGhost
                            )
                    ) {

                        /*
                         * 目标鬼必须能够被来源鬼的灵异压制。
                         */
                        if (
                                !canSuppress(
                                        draggingSourceGhost,
                                        targetGhost
                                )
                        ) {

                            QisPlan2.LOGGER.info(
                                    "[灵异配平] {} 无法压制 {}，拒绝分配",
                                    draggingSourceGhost,
                                    targetGhost
                            );

                            draggingSuppression = false;
                            draggingSourceGhost = null;
                            draggingOriginalTargetGhost = null;
                            draggingSlotIndex = -1;

                            return true;
                        }

                        int targetIndex = 0;

                        for (
                                ResourceLocation ghostId
                                : minecraft.player
                                .getData(
                                        ModAttachments.POSSESSED_GHOSTS
                                )
                                .keySet()
                        ) {

                            if (ghostId.equals(targetGhost)) {
                                break;
                            }

                            targetIndex++;
                        }

                        int targetCardX =
                                getWorkbenchCardX(
                                        targetIndex
                                );

                        int targetCardY =
                                getWorkbenchCardY(
                                        targetIndex
                                );

                        double relativeX =
                                localX - targetCardX;

                        double relativeY =
                                localY - targetCardY;

                        QisPlan2.LOGGER.info(
                                "[灵异配平] 新增压制额度：{} 的 slot {} → {}，位置=({}, {})",
                                draggingSourceGhost,
                                draggingSlotIndex,
                                targetGhost,
                                relativeX,
                                relativeY
                        );

                        GhostPossessionNetwork
                                .sendSuppressionAllocation(
                                        draggingSourceGhost,
                                        targetGhost,
                                        draggingSlotIndex,
                                        relativeX,
                                        relativeY
                                );
                    }
                }
            }

            /*
             * 清理客户端拖动状态。
             */
            draggingSuppression = false;

            draggingSourceGhost = null;

            draggingOriginalTargetGhost = null;

            draggingSlotIndex = -1;

            return true;
        }

        return super.mouseReleased(
                mouseX,
                mouseY,
                button
        );
    }

    private void drawPanel(
            GuiGraphics graphics
    ) {

        /*
         * 外框。
         */
        graphics.fill(
                -1,
                -1,
                PANEL_WIDTH + 1,
                PANEL_HEIGHT + 1,
                0xFF606060
        );

        /*
         * 主背景。
         */
        graphics.fill(
                0,
                0,
                PANEL_WIDTH,
                PANEL_HEIGHT,
                0xF0101016
        );

        /*
         * 顶部标题区域。
         */
        graphics.fill(
                0,
                0,
                PANEL_WIDTH,
                24,
                0xF0181820
        );

        /*
         * 标题。
         */

        graphics.drawCenteredString(
                this.font,
                "驭鬼者状态",
                PANEL_WIDTH / 2,
                7,
                0xFFFFFFFF
        );
    }

    private void drawTabs(
            GuiGraphics graphics
    ) {

        int tabY = 24;

        int tabWidth = 70;
        int tabHeight = 20;

        int statusX = 12;
        int suppressionX = statusX + tabWidth + 4;

        // =========================
        // 状态
        // =========================

        drawTab(
                graphics,
                statusX,
                tabY,
                tabWidth,
                tabHeight,
                "状态",
                currentPage == 0
        );

        // =========================
        // 灵异
        // =========================

        drawTab(
                graphics,
                suppressionX,
                tabY,
                tabWidth,
                tabHeight,
                "灵异",
                currentPage == 1
        );
    }

    private void drawTab(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            String text,
            boolean selected
    ) {

        int backgroundColor =
                selected
                        ? 0xFF30303A
                        : 0xFF181820;

        int borderColor =
                selected
                        ? 0xFF80808C
                        : 0xFF383840;

        // 外框
        graphics.fill(
                x,
                y,
                x + width,
                y + height,
                borderColor
        );

        // 内部
        graphics.fill(
                x + 1,
                y + 1,
                x + width - 1,
                y + height - 1,
                backgroundColor
        );

        graphics.drawCenteredString(
                this.font,
                text,
                x + width / 2,
                y + 6,
                selected
                        ? 0xFFFFFFFF
                        : 0xFFAAAAAA
        );
    }

    private void drawSuppressionPage(
            GuiGraphics graphics
    ) {

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        Map<ResourceLocation, PossessedGhostState> ghosts =
                minecraft.player.getData(
                        ModAttachments.POSSESSED_GHOSTS
                );

        int columns = 3;

        int gapX = 18;
        int gapY = 12;

        int totalWidth =
                columns * GHOST_WORKBENCH_CARD_WIDTH
                        + (columns - 1) * gapX;

        int startX =
                (PANEL_WIDTH - totalWidth) / 2;

        int startY = 52;

        int index = 0;

        for (var entry : ghosts.entrySet()) {

            int column =
                    index % columns;

            int row =
                    index / columns;

            int x =
                    startX
                            + column
                            * (GHOST_WORKBENCH_CARD_WIDTH + gapX);

            int y =
                    startY
                            + row
                            * (GHOST_WORKBENCH_CARD_HEIGHT + gapY);

            drawWorkbenchGhostCard(
                    graphics,
                    entry.getKey(),
                    entry.getValue(),
                    x,
                    y
            );

            index++;
        }
    }

    private void drawWorkbenchGhostCard(
            GuiGraphics graphics,
            ResourceLocation ghostId,
            PossessedGhostState state,
            int x,
            int y
    ) {

        graphics.fill(
                x,
                y,
                x + GHOST_WORKBENCH_CARD_WIDTH,
                y + GHOST_WORKBENCH_CARD_HEIGHT,
                0xD0181820
        );

        graphics.fill(
                x + 6,
                y + 1,
                x + GHOST_WORKBENCH_CARD_WIDTH - 6,
                y + 2,
                0x40FFFFFF
        );

        PossessedGhostAbility ability =
                GhostAbilityRegistry.get(ghostId);

        drawGhostIcon(
                graphics,
                ability,
                x + 6,
                y + 6,
                18
        );

        graphics.drawString(
                this.font,
                getGhostName(ghostId),
                x + 30,
                y + 6,
                0xFFFFFFFF
        );

        graphics.drawString(
                this.font,
                String.format(
                        "%.0f",
                        state.intrinsicStrength()
                ),
                x + 132,
                y + 6,
                0xFFCCCCCC
        );

        graphics.drawString(
                this.font,
                "压制额度",
                x + 8,
                y + 34,
                0xFFAAAAAA
        );

        drawSuppressionSlots(
                graphics,
                x + 8,
                y + 50,
                ability
        );

        drawAllocatedSuppression(
                graphics,
                ghostId,
                x,
                y
        );
    }



    private void drawDraggingSuppression(
            GuiGraphics graphics
    ) {

        if (draggingSourceGhost == null) {
            return;
        }

        PossessedGhostAbility ability =
                GhostAbilityRegistry.get(
                        draggingSourceGhost
                );

        if (ability == null) {
            return;
        }

        ResourceLocation texture =
                ability.suppressionIconTexture();

        if (texture == null) {
            texture = DEFAULT_SUPPRESSION_ICON;
        }

        int size = 14;

        int x =
                draggingMouseX
                        - size / 2;

        int y =
                draggingMouseY
                        - size / 2;

        graphics.blit(
                texture,
                x,
                y,
                0,
                0,
                size,
                size,
                size,
                size
        );
    }

    private void drawAllocatedSuppression(
            GuiGraphics graphics,
            ResourceLocation targetGhost,
            int cardX,
            int cardY
    ) {

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        Map<
                ResourceLocation,
                Map<ResourceLocation, List<SuppressionAllocation>>
                > allocations =
                GhostSuppressionAllocationHandler
                        .getAllocations(
                                minecraft.player
                        );

        int size = 14;

        /*
         * ========================================================
         * 遍历所有：
         *
         * sourceGhost → targetGhost
         *
         * 只绘制当前这张鬼卡片作为目标的分配。
         * ========================================================
         */
        for (
                Map.Entry<
                        ResourceLocation,
                        Map<ResourceLocation, List<SuppressionAllocation>>
                        >
                        sourceEntry
                : allocations.entrySet()
        ) {

            ResourceLocation sourceGhost =
                    sourceEntry.getKey();

            Map<ResourceLocation, List<SuppressionAllocation>>
                    targets =
                    sourceEntry.getValue();

            List<SuppressionAllocation> list =
                    targets.get(targetGhost);

            if (list == null || list.isEmpty()) {
                continue;
            }

            /*
             * ========================================================
             * 根据来源鬼获取压制额度图标。
             *
             * 每个鬼都可以拥有自己的 suppressionIconTexture()。
             *
             * null：
             * 使用全局默认压制额度图标。
             * ========================================================
             */
            PossessedGhostAbility sourceAbility =
                    GhostAbilityRegistry.get(
                            sourceGhost
                    );

            ResourceLocation texture =
                    sourceAbility == null
                            ? DEFAULT_SUPPRESSION_ICON
                            : sourceAbility.suppressionIconTexture();

            if (texture == null) {
                texture =
                        DEFAULT_SUPPRESSION_ICON;
            }

            /*
             * ========================================================
             * 绘制所有已经分配到当前鬼的压制额度。
             * ========================================================
             */
            for (SuppressionAllocation allocation : list) {

                int x =
                        cardX
                                + (int) allocation.x()
                                - size / 2;

                int y =
                        cardY
                                + (int) allocation.y()
                                - size / 2;

                graphics.blit(
                        texture,
                        x,
                        y,
                        0,
                        0,
                        size,
                        size,
                        size,
                        size
                );
            }
        }
    }

    private void drawSuppressionSlots(
            GuiGraphics graphics,
            int x,
            int y,
            PossessedGhostAbility ability
    ) {

        if (ability == null) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        ResourceLocation sourceGhost =
                ability.id();

        int slots =
                ability.suppressionUnits();

        int size = 10;
        int gap = 4;

        ResourceLocation texture =
                ability.suppressionIconTexture();

        if (texture == null) {
            texture = DEFAULT_SUPPRESSION_ICON;
        }

        for (int i = 0; i < slots; i++) {

            int column = i % 5;
            int row = i / 5;

            int sx =
                    x + column * (size + gap);

            int sy =
                    y + row * (size + gap);

            boolean allocated =
                    GhostSuppressionAllocationHandler
                            .isSlotAllocated(
                                    minecraft.player,
                                    sourceGhost,
                                    i
                            );

            /*
             * 已经被分配出去：
             *
             * 只画一个空槽框。
             */
            if (allocated) {

                graphics.fill(
                        sx,
                        sy,
                        sx + size,
                        sy + size,
                        0xFF555555
                );

                graphics.fill(
                        sx + 1,
                        sy + 1,
                        sx + size - 1,
                        sy + size - 1,
                        0xFF202020
                );

                continue;
            }

            /*
             * 尚未分配：
             *
             * 正常显示压制图标。
             */
            graphics.blit(
                    texture,
                    sx,
                    sy,
                    0,
                    0,
                    size,
                    size,
                    size,
                    size
            );
        }
    }

    private void drawGhostIcon(
            GuiGraphics graphics,
            PossessedGhostAbility ability,
            int x,
            int y,
            int size
    ) {

        if (ability == null) {
            graphics.fill(
                    x,
                    y,
                    x + size,
                    y + size,
                    0xFF555565
            );
            return;
        }

        ResourceLocation texture =
                ability.iconTexture();

        if (texture == null) {

            // 暂时使用纯红色作为默认灵异图标
            graphics.fill(
                    x,
                    y,
                    x + size,
                    y + size,
                    0xFFE02020
            );

            return;
        }

        graphics.blit(
                texture,
                x,
                y,
                0,
                0,
                size,
                size,
                size,
                size
        );
    }

    private int getWorkbenchCardX(int index) {

        int columns = 3;
        int gapX = 18;

        int totalWidth =
                columns * GHOST_WORKBENCH_CARD_WIDTH
                        + (columns - 1) * gapX;

        int startX =
                (PANEL_WIDTH - totalWidth) / 2;

        int column = index % columns;

        return startX
                + column
                * (GHOST_WORKBENCH_CARD_WIDTH + gapX);
    }

    private int getWorkbenchCardY(int index) {

        int gapY = 12;
        int startY = 52;

        int row = index / 3;

        return startY
                + row
                * (GHOST_WORKBENCH_CARD_HEIGHT + gapY);
    }

    private ResourceLocation getWorkbenchGhostAt(
            double mouseX,
            double mouseY
    ) {

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return null;
        }

        Map<ResourceLocation, PossessedGhostState> ghosts =
                minecraft.player.getData(
                        ModAttachments.POSSESSED_GHOSTS
                );

        int index = 0;

        for (ResourceLocation ghostId : ghosts.keySet()) {

            int x = getWorkbenchCardX(index);
            int y = getWorkbenchCardY(index);

            if (
                    mouseX >= x
                            && mouseX < x + GHOST_WORKBENCH_CARD_WIDTH
                            && mouseY >= y
                            && mouseY < y + GHOST_WORKBENCH_CARD_HEIGHT
            ) {
                return ghostId;
            }

            index++;
        }

        return null;
    }

    private int getSuppressionSlotX(
            int cardX,
            int slotIndex
    ) {

        int size = 10;
        int gap = 4;

        int column = slotIndex % 5;

        return cardX
                + 8
                + column * (size + gap);
    }

    private int getSuppressionSlotY(
            int cardY,
            int slotIndex
    ) {

        int size = 10;
        int gap = 4;

        int row = slotIndex / 5;

        return cardY
                + 50
                + row * (size + gap);
    }

    private int getSuppressionSlotAt(
            double mouseX,
            double mouseY,
            int cardX,
            int cardY,
            int slots
    ) {

        int size = 10;

        for (int i = 0; i < slots; i++) {

            int x =
                    getSuppressionSlotX(
                            cardX,
                            i
                    );

            int y =
                    getSuppressionSlotY(
                            cardY,
                            i
                    );

            if (
                    mouseX >= x
                            && mouseX < x + size
                            && mouseY >= y
                            && mouseY < y + size
            ) {
                return i;
            }
        }

        return -1;
    }

    private SuppressionAllocationHit getAllocatedSuppressionAt(
            double mouseX,
            double mouseY,
            ResourceLocation targetGhost,
            int cardX,
            int cardY
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return null;
        }

        Map<
                ResourceLocation,
                Map<ResourceLocation, List<SuppressionAllocation>>
                > allocations =
                GhostSuppressionAllocationHandler
                        .getAllocations(
                                minecraft.player
                        );

        int size = 14;

        /*
         * sourceGhost
         *     ↓
         * targetGhost
         *     ↓
         * allocation
         */
        for (
                Map.Entry<
                        ResourceLocation,
                        Map<ResourceLocation, List<SuppressionAllocation>>
                        > sourceEntry
                : allocations.entrySet()
        ) {

            ResourceLocation sourceGhost =
                    sourceEntry.getKey();

            List<SuppressionAllocation> list =
                    sourceEntry
                            .getValue()
                            .get(targetGhost);

            if (list == null) {
                continue;
            }

            for (SuppressionAllocation allocation : list) {

                int x =
                        cardX
                                + (int) allocation.x()
                                - size / 2;

                int y =
                        cardY
                                + (int) allocation.y()
                                - size / 2;

                if (
                        mouseX >= x
                                && mouseX < x + size
                                && mouseY >= y
                                && mouseY < y + size
                ) {

                    return new SuppressionAllocationHit(
                            sourceGhost,
                            targetGhost,
                            allocation
                    );
                }
            }
        }

        return null;
    }

    private boolean isInside(
            double mouseX,
            double mouseY,
            int x,
            int y,
            int width,
            int height
    ) {

        return mouseX >= x
                && mouseX < x + width
                && mouseY >= y
                && mouseY < y + height;
    }

    private boolean canSuppress(
            ResourceLocation sourceGhost,
            ResourceLocation targetGhost
    ) {

        if (sourceGhost == null || targetGhost == null) {
            return false;
        }

        if (sourceGhost.equals(targetGhost)) {
            return false;
        }

        PossessedGhostAbility sourceAbility =
                GhostAbilityRegistry.get(
                        sourceGhost
                );

        PossessedGhostAbility targetAbility =
                GhostAbilityRegistry.get(
                        targetGhost
                );

        if (sourceAbility == null || targetAbility == null) {
            return false;
        }

        return sourceAbility.canSuppress(
                targetAbility
        );
    }

    private String getGhostName(
            ResourceLocation id
    ) {

        PossessedGhostAbility ability =
                GhostAbilityRegistry.get(
                        id
                );

        if (ability != null) {

            return ability
                    .displayName()
                    .getString();
        }

        return id.toString();
    }

    @Override
    public boolean isPauseScreen() {

        return false;
    }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {

        /*
         * H 再按一次关闭。
         */
        if (keyCode ==
                ModKeyMappings
                        .OPEN_POSSESSION_SCREEN
                        .getKey()
                        .getValue()) {

            this.onClose();

            return true;
        }

        return super.keyPressed(
                keyCode,
                scanCode,
                modifiers
        );
    }

    private record SuppressionAllocationHit(
            ResourceLocation sourceGhost,
            ResourceLocation targetGhost,
            SuppressionAllocation allocation
    ) {}
}