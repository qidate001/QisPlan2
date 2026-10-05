package com.qidate.qisplan2.client.screen.PossessionScreen;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.core.ModAttachments;
import com.qidate.qisplan2.ghost.possession.ability.GhostAbilityRegistry;
import com.qidate.qisplan2.ghost.possession.ability.PossessedGhostAbility;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostData;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostState;
import com.qidate.qisplan2.ghost.possession.manager.GhostSuppressionAllocationHandler;
import com.qidate.qisplan2.ghost.possession.manager.SuppressionAllocation;
import com.qidate.qisplan2.network.possession.GhostPossessionNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * 驭鬼者灵异页渲染器。
 *
 * <p>
 * 负责灵异页的全部视觉内容以及压制系统的交互。
 *
 * <p>
 * 包括：
 * <ul>
 *     <li>驭鬼工作台卡片</li>
 *     <li>压制额度槽位</li>
 *     <li>已经分配的压制额度</li>
 *     <li>正在拖动的压制额度</li>
 *     <li>压制额度的点击、拖动与释放</li>
 * </ul>
 *
 * <p>
 * 不负责 Screen 生命周期、页面切换以及面板坐标变换。
 */
public class PossessionSuppressionRenderer {

    /**
     * 灵异页中单张驭鬼卡片的宽度。
     */
    private static final int GHOST_WORKBENCH_CARD_WIDTH = 172;

    /**
     * 灵异页中单张驭鬼卡片的高度。
     */
    private static final int GHOST_WORKBENCH_CARD_HEIGHT = 76;

    /**
     * 驭鬼者面板宽度。
     *
     * <p>
     * 当前阶段暂时与 PossessionScreen 保持一致。
     */
    private static final int PANEL_WIDTH = 600;

    /**
     * 驭鬼者面板高度。
     *
     * <p>
     * 当前阶段暂时与 PossessionScreen 保持一致。
     */
    private static final int PANEL_HEIGHT = 320;

    /**
     * 默认压制额度图标。
     *
     * <p>
     * 当某个灵异没有提供专属压制图标时，
     * 使用该图标作为回退显示。
     */
    private static final ResourceLocation DEFAULT_SUPPRESSION_ICON =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "textures/gui/suppression.png"
            );

    /**
     * 当前是否正在拖动压制额度。
     */
    private boolean draggingSuppression = false;

    /**
     * 当前拖动的压制额度来自哪个鬼。
     *
     * <p>
     * 对应压制槽位的来源鬼。
     */
    private ResourceLocation draggingSourceGhost = null;

    /**
     * 当前拖动的压制额度原本分配给哪个鬼。
     *
     * <p>
     * 为 null 时，表示这是从来源鬼的空闲槽位直接拖出的新分配。
     */
    private ResourceLocation draggingOriginalTargetGhost = null;

    /**
     * 当前拖动的压制额度对应的来源槽位。
     */
    private int draggingSlotIndex = -1;

    /**
     * 当前拖动图标的 X 坐标。
     *
     * <p>
     * 使用面板局部坐标。
     */
    private int draggingMouseX = 0;

    /**
     * 当前拖动图标的 Y 坐标。
     *
     * <p>
     * 使用面板局部坐标。
     */
    private int draggingMouseY = 0;

    /**
     * 绘制灵异页。
     *
     * <p>
     * 当前方法只负责绘制，
     * 坐标已经由 PossessionScreen 完成面板变换。
     */
    public void render(
            GuiGraphics graphics
    ) {
        drawSuppressionPage(
                graphics
        );

        if (draggingSuppression) {
            drawDraggingSuppression(
                    graphics
            );
        }
    }

    /**
     * 绘制灵异页主体。
     *
     * <p>
     * 当前页面采用 3 列工作台布局。
     */
    private void drawSuppressionPage(
            GuiGraphics graphics
    ) {

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        Map<ResourceLocation, PossessedGhostData> ghosts =
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
                    entry.getValue().state(),
                    x,
                    y
            );

            index++;
        }
    }

    /**
     * 绘制单张驭鬼工作台卡片。
     *
     * <p>
     * 卡片包含：
     * <ul>
     *     <li>鬼图标</li>
     *     <li>鬼名称</li>
     *     <li>灵异强度</li>
     *     <li>压制额度槽位</li>
     *     <li>已经分配出去的压制额度</li>
     * </ul>
     */
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
                GhostAbilityRegistry.get(
                        ghostId
                );

        drawGhostIcon(
                graphics,
                ability,
                x + 6,
                y + 6,
                18
        );

        graphics.drawString(
                Minecraft.getInstance().font,
                getGhostName(ghostId),
                x + 30,
                y + 6,
                0xFFFFFFFF
        );

        graphics.drawString(
                Minecraft.getInstance().font,
                String.format(
                        "%.0f",
                        state.strength()
                ),
                x + 132,
                y + 6,
                0xFFCCCCCC
        );

        graphics.drawString(
                Minecraft.getInstance().font,
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

    /**
     * 绘制当前正在拖动的压制额度。
     *
     * <p>
     * 拖动过程中图标跟随鼠标，
     * 但真正的分配只有在释放鼠标时才提交。
     */
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
            texture =
                    DEFAULT_SUPPRESSION_ICON;
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

    /**
     * 绘制已经分配出去的压制额度。
     *
     * <p>
     * 分配结构为：
     *
     * <pre>
     * sourceGhost
     *     ↓
     * targetGhost
     *     ↓
     * allocation
     * </pre>
     *
     * <p>
     * 当前方法只绘制目标为当前鬼的分配。
     */
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
                    targets.get(
                            targetGhost
                    );

            if (list == null || list.isEmpty()) {
                continue;
            }

            /*
             * 根据来源鬼获取压制额度图标。
             *
             * 每个鬼可以拥有自己的压制额度图标。
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
             * 绘制当前目标鬼所拥有的全部压制额度。
             */
            for (
                    SuppressionAllocation allocation
                    : list
            ) {

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

    /**
     * 绘制一个鬼自身拥有的压制槽位。
     *
     * <p>
     * 未分配的槽位显示压制图标，
     * 已经分配出去的槽位显示为空槽。
     */
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
            texture =
                    DEFAULT_SUPPRESSION_ICON;
        }

        for (int i = 0; i < slots; i++) {

            int column =
                    i % 5;

            int row =
                    i / 5;

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
             * 已经分配出去：
             *
             * 只显示空槽框。
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
             * 显示正常压制图标。
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

    /**
     * 绘制鬼的灵异图标。
     *
     * <p>
     * 如果灵异没有注册或没有提供图标，
     * 使用简单的占位显示。
     */
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

            // 暂时使用纯红色作为默认灵异图标。
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

    /**
     * 处理灵异页鼠标按下。
     *
     * <p>
     * 负责从：
     * <ul>
     *     <li>空闲压制槽</li>
     *     <li>已经分配出去的压制额度</li>
     * </ul>
     * 开始拖动。
     */
    public boolean mouseClicked(
            double mouseX,
            double mouseY
    ) {

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return false;
        }

        Map<ResourceLocation, PossessedGhostData> ghosts =
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
             * =========================================================
             * 第一优先级：
             *
             * 检查已经分配出去的压制额度。
             *
             * 这样可以从目标鬼卡片上重新抓起一个已经存在的分配。
             * =========================================================
             */
            SuppressionAllocationHit allocated =
                    getAllocatedSuppressionAt(
                            mouseX,
                            mouseY,
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
                        (int) mouseX;

                draggingMouseY =
                        (int) mouseY;

                return true;
            }

            /*
             * =========================================================
             * 第二优先级：
             *
             * 检查来源鬼自己的空闲压制槽。
             * =========================================================
             */
            int slot =
                    getSuppressionSlotAt(
                            mouseX,
                            mouseY,
                            cardX,
                            cardY,
                            ability.suppressionUnits()
                    );

            if (slot >= 0) {

                /*
                 * 已经分配出去的槽位不能再次拖动。
                 */
                if (
                        GhostSuppressionAllocationHandler
                                .isSlotAllocated(
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
                        (int) mouseX;

                draggingMouseY =
                        (int) mouseY;

                return true;
            }

            index++;
        }

        return false;
    }

    /**
     * 更新当前正在拖动的压制额度位置。
     */
    public boolean mouseDragged(
            double mouseX,
            double mouseY
    ) {

        if (!draggingSuppression) {
            return false;
        }

        draggingMouseX =
                (int) mouseX;

        draggingMouseY =
                (int) mouseY;

        return true;
    }

    /**
     * 处理灵异页鼠标释放。
     *
     * <p>
     * 根据释放位置决定：
     * <ul>
     *     <li>撤回原分配</li>
     *     <li>移动已有分配</li>
     *     <li>调整已有分配位置</li>
     *     <li>建立新的压制分配</li>
     *     <li>什么都不做</li>
     * </ul>
     */
    public boolean mouseReleased(
            double mouseX,
            double mouseY
    ) {

        if (!draggingSuppression) {
            return false;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            clearDraggingState();
            return true;
        }

        ResourceLocation targetGhost =
                getWorkbenchGhostAt(
                        mouseX,
                        mouseY
                );

        /*
         * =========================================================
         * 已经存在的分配
         * =========================================================
         */
        if (draggingOriginalTargetGhost != null) {

            /*
             * -----------------------------------------------------
             * 情况 1-A：
             *
             * 拖回自己的来源鬼。
             *
             * → 删除原分配。
             * -----------------------------------------------------
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
             * -----------------------------------------------------
             * 情况 1-B：
             *
             * 拖到了其他鬼。
             *
             * → 移动分配。
             * -----------------------------------------------------
             */
            else if (
                    targetGhost != null
                            && !targetGhost.equals(
                            draggingSourceGhost
                    )
            ) {

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

                    clearDraggingState();
                    return true;
                }

                sendSuppressionMove(
                        minecraft,
                        mouseX,
                        mouseY,
                        targetGhost
                );
            }

            /*
             * -----------------------------------------------------
             * 情况 1-C：
             *
             * 仍然拖回原来的目标鬼。
             *
             * → 目标不变，只更新位置。
             * -----------------------------------------------------
             */
            else if (
                    targetGhost != null
                            && targetGhost.equals(
                            draggingOriginalTargetGhost
                    )
            ) {

                sendSuppressionMove(
                        minecraft,
                        mouseX,
                        mouseY,
                        targetGhost
                );
            }

            /*
             * 情况 1-D：
             *
             * targetGhost == null
             *
             * → 什么都不做。
             */
        }

        /*
         * =========================================================
         * 尚未分配的空闲槽
         * =========================================================
         */
        else {

            /*
             * 只有拖到其他鬼卡片才建立新的分配。
             */
            if (
                    targetGhost != null
                            && !targetGhost.equals(
                            draggingSourceGhost
                    )
            ) {

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

                    clearDraggingState();
                    return true;
                }

                sendSuppressionAllocation(
                        minecraft,
                        mouseX,
                        mouseY,
                        targetGhost
                );
            }
        }

        clearDraggingState();

        return true;
    }

    /**
     * 提交一个新的压制分配。
     */
    private void sendSuppressionAllocation(
            Minecraft minecraft,
            double mouseX,
            double mouseY,
            ResourceLocation targetGhost
    ) {

        int targetIndex =
                getWorkbenchGhostIndex(
                        minecraft,
                        targetGhost
                );

        int targetCardX =
                getWorkbenchCardX(
                        targetIndex
                );

        int targetCardY =
                getWorkbenchCardY(
                        targetIndex
                );

        double relativeX =
                mouseX - targetCardX;

        double relativeY =
                mouseY - targetCardY;

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

    /**
     * 移动一个已经存在的压制分配。
     */
    private void sendSuppressionMove(
            Minecraft minecraft,
            double mouseX,
            double mouseY,
            ResourceLocation targetGhost
    ) {

        int targetIndex =
                getWorkbenchGhostIndex(
                        minecraft,
                        targetGhost
                );

        int targetCardX =
                getWorkbenchCardX(
                        targetIndex
                );

        int targetCardY =
                getWorkbenchCardY(
                        targetIndex
                );

        double relativeX =
                mouseX - targetCardX;

        double relativeY =
                mouseY - targetCardY;

        QisPlan2.LOGGER.info(
                "[灵异配平] 移动压制额度：{} 的 slot {}：{} → {}，位置=({}, {})",
                draggingSourceGhost,
                draggingSlotIndex,
                draggingOriginalTargetGhost,
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

    /**
     * 清理当前压制拖动状态。
     */
    private void clearDraggingState() {

        draggingSuppression = false;

        draggingSourceGhost = null;

        draggingOriginalTargetGhost = null;

        draggingSlotIndex = -1;

        draggingMouseX = 0;

        draggingMouseY = 0;
    }

    /**
     * 获取工作台中指定鬼卡片的 X 坐标。
     */
    private int getWorkbenchCardX(
            int index
    ) {

        int columns = 3;
        int gapX = 18;

        int totalWidth =
                columns * GHOST_WORKBENCH_CARD_WIDTH
                        + (columns - 1) * gapX;

        int startX =
                (PANEL_WIDTH - totalWidth) / 2;

        int column =
                index % columns;

        return startX
                + column
                * (GHOST_WORKBENCH_CARD_WIDTH + gapX);
    }

    /**
     * 获取工作台中指定鬼卡片的 Y 坐标。
     */
    private int getWorkbenchCardY(
            int index
    ) {

        int gapY = 12;
        int startY = 52;

        int row =
                index / 3;

        return startY
                + row
                * (GHOST_WORKBENCH_CARD_HEIGHT + gapY);
    }

    /**
     * 获取指定鬼在当前工作台中的索引。
     */
    private int getWorkbenchGhostIndex(
            Minecraft minecraft,
            ResourceLocation targetGhost
    ) {

        int index = 0;

        for (
                ResourceLocation ghostId
                : minecraft.player
                .getData(
                        ModAttachments.POSSESSED_GHOSTS
                )
                .keySet()
        ) {

            if (ghostId.equals(targetGhost)) {
                return index;
            }

            index++;
        }

        return -1;
    }

    /**
     * 根据鼠标位置获取当前点击的鬼卡片。
     */
    private ResourceLocation getWorkbenchGhostAt(
            double mouseX,
            double mouseY
    ) {

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return null;
        }

        Map<ResourceLocation, PossessedGhostData> ghosts =
                minecraft.player.getData(
                        ModAttachments.POSSESSED_GHOSTS
                );

        int index = 0;

        for (ResourceLocation ghostId : ghosts.keySet()) {

            int x =
                    getWorkbenchCardX(index);

            int y =
                    getWorkbenchCardY(index);

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

    /**
     * 获取指定压制槽位的 X 坐标。
     */
    private int getSuppressionSlotX(
            int cardX,
            int slotIndex
    ) {

        int size = 10;
        int gap = 4;

        int column =
                slotIndex % 5;

        return cardX
                + 8
                + column * (size + gap);
    }

    /**
     * 获取指定压制槽位的 Y 坐标。
     */
    private int getSuppressionSlotY(
            int cardY,
            int slotIndex
    ) {

        int size = 10;
        int gap = 4;

        int row =
                slotIndex / 5;

        return cardY
                + 50
                + row * (size + gap);
    }

    /**
     * 根据鼠标位置获取压制槽位。
     *
     * @return 命中的槽位索引；没有命中时返回 -1。
     */
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

    /**
     * 获取鼠标位置下已经存在的压制分配。
     */
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
                        >
                        sourceEntry
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

            for (
                    SuppressionAllocation allocation
                    : list
            ) {

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

    /**
     * 判断一个鬼能否压制另一个鬼。
     *
     * <p>
     * 同一个鬼不能压制自己，
     * 且双方都必须已经注册对应的灵异能力。
     */
    private boolean canSuppress(
            ResourceLocation sourceGhost,
            ResourceLocation targetGhost
    ) {

        if (
                sourceGhost == null
                        || targetGhost == null
        ) {
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

        if (
                sourceAbility == null
                        || targetAbility == null
        ) {
            return false;
        }

        return sourceAbility.canSuppress(
                targetAbility
        );
    }

    /**
     * 获取鬼的显示名称。
     */
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

    /**
     * 当前压制拖动状态。
     */
    private record SuppressionAllocationHit(
            ResourceLocation sourceGhost,
            ResourceLocation targetGhost,
            SuppressionAllocation allocation
    ) {}
}