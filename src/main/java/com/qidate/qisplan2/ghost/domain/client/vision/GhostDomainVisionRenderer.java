package com.qidate.qisplan2.ghost.domain.client.vision;

import com.mojang.blaze3d.vertex.PoseStack;
import com.qidate.qisplan2.ghost.domain.client.ClientGhostDomainVisionSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;

import java.util.Map;
import java.util.UUID;

/**
 * 鬼域视觉描边渲染器。
 *
 * <p>
 * 负责在鬼域主人客户端中，
 * 为鬼域范围内的实体绘制特殊透视描边。
 *
 * <p>
 * 本类与 {@code ghost.domain.client.renderer}
 * 是两码事哦。
 *
 * <p>
 * {@code renderer} 包负责鬼域本身的画面后处理；
 * 本类负责鬼域赋予实体的视觉效果。
 */
public final class GhostDomainVisionRenderer {

    private GhostDomainVisionRenderer() {
    }

    /**
     * 注册鬼域视觉渲染入口。
     */
    public static void render(
            RenderLevelStageEvent event
    ) {

        /*
         * ============================================================
         * 只在实体已经完成正常渲染之后进行
         * ============================================================
         */
        if (event.getStage()
                != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        ClientLevel level =
                minecraft.level;

        if (level == null) {
            return;
        }

        Map<UUID, Integer> visibleEntities =
                ClientGhostDomainVisionSystem
                        .getVisibleEntities();

        if (visibleEntities.isEmpty()) {
            return;
        }

        Camera camera =
                event.getCamera();

        Frustum frustum =
                event.getFrustum();

        PoseStack poseStack =
                event.getPoseStack();

        if (poseStack == null) {
            return;
        }

        /*
         * ============================================================
         * 当前帧的部分 tick
         * ============================================================
         */
        float partialTick =
                event.getPartialTick()
                        .getGameTimeDeltaPartialTick(true);

        /*
         * ============================================================
         * Minecraft 原版的实体轮廓 Buffer
         *
         * 我们不是自己画一个“假轮廓”，
         * 而是让实体重新进入原版 OutlineBufferSource。
         *
         * 因此最终效果与原版发光轮廓属于同一套渲染机制。
         * ============================================================
         */
        OutlineBufferSource outlineBuffer =
                minecraft.renderBuffers()
                        .outlineBufferSource();

        EntityRenderDispatcher dispatcher =
                minecraft.getEntityRenderDispatcher();

        /*
         * ============================================================
         * 遍历服务端告诉客户端的实体
         * ============================================================
         */
        for (Map.Entry<UUID, Integer> entry :
                visibleEntities.entrySet()) {

            UUID entityUUID =
                    entry.getKey();

            Integer color =
                    entry.getValue();

            if (color == null) {
                continue;
            }

            Entity entity = null;

            for (Entity candidate : level.entitiesForRendering()) {
                if (candidate.getUUID().equals(entityUUID)) {
                    entity = candidate;
                    break;
                }
            }

            if (entity == null) {
                continue;
            }

            if (entity.isRemoved()) {
                continue;
            }

            /*
             * ========================================================
             * 计算实体相对于摄像机的位置
             * ========================================================
             */
            double x =
                    Mth.lerp(
                            partialTick,
                            entity.xOld,
                            entity.getX()
                    ) - camera.getPosition().x;

            double y =
                    Mth.lerp(
                            partialTick,
                            entity.yOld,
                            entity.getY()
                    ) - camera.getPosition().y;

            double z =
                    Mth.lerp(
                            partialTick,
                            entity.zOld,
                            entity.getZ()
                    ) - camera.getPosition().z;

            /*
             * ========================================================
             * 视锥裁剪
             *
             * 透视 ≠ 无限距离渲染。
             * 不在玩家视野中的实体没必要重新渲染。
             * ========================================================
             */
            if (!dispatcher.shouldRender(
                    entity,
                    frustum,
                    x,
                    y,
                    z
            )) {
                continue;
            }

            /*
             * ========================================================
             * 设置本实体的轮廓颜色
             *
             * 0xRRGGBB
             * ========================================================
             */
            int red =
                    (color >> 16) & 0xFF;

            int green =
                    (color >> 8) & 0xFF;

            int blue =
                    color & 0xFF;

            outlineBuffer.setColor(
                    red,
                    green,
                    blue,
                    255
            );

            /*
             * ========================================================
             * 重新渲染实体
             *
             * 关键点：
             *
             * 这里传入的不是普通实体 Buffer，
             * 而是 OutlineBufferSource。
             *
             * 因此实体模型会被写入轮廓缓冲。
             * ========================================================
             */
            poseStack.pushPose();

            float yaw =
                    Mth.lerp(
                            partialTick,
                            entity.yRotO,
                            entity.getYRot()
                    );

            int packedLight =
                    dispatcher.getPackedLightCoords(
                            entity,
                            partialTick
                    );

            dispatcher.render(
                    entity,
                    x,
                    y,
                    z,
                    yaw,
                    partialTick,
                    poseStack,
                    outlineBuffer,
                    packedLight
            );

            poseStack.popPose();
        }

        /*
         * ============================================================
         * 提交轮廓 Buffer
         * ============================================================
         */
        outlineBuffer.endOutlineBatch();
    }

    /**
     * 判断实体当前是否应该进行鬼域视觉描边。
     *
     * @param entity 实体
     * @return 是否需要描边
     */
    public static boolean shouldOutline(
            Entity entity
    ) {

        if (entity == null) {
            return false;
        }

        return ClientGhostDomainVisionSystem
                .shouldOutline(
                        entity.getUUID()
                );
    }

    /**
     * 获取实体当前应该使用的描边颜色。
     *
     * @param entity 实体
     * @return RGB 颜色；没有视觉效果时返回 null
     */
    public static Integer getOutlineColor(
            Entity entity
    ) {

        if (!shouldOutline(entity)) {
            return null;
        }

        return ClientGhostDomainVisionSystem
                .getColor(
                        entity.getUUID()
                );
    }
}