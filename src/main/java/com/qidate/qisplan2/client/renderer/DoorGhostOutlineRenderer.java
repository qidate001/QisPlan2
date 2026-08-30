package com.qidate.qisplan2.client.renderer;

import com.qidate.qisplan2.client.DoorGhostMarkClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import com.mojang.blaze3d.vertex.PoseStack;

@EventBusSubscriber(
        modid = "qisplan2",
        value = Dist.CLIENT
)
public final class DoorGhostOutlineRenderer {

    private DoorGhostOutlineRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(
            RenderLevelStageEvent event
    ) {

        if (event.getStage()
                != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {

            return;
        }

        if (DoorGhostMarkClient.isEmpty()) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        Camera camera =
                event.getCamera();

        PoseStack poseStack =
                event.getPoseStack();

        EntityRenderDispatcher dispatcher =
                minecraft.getEntityRenderDispatcher();

        RenderBuffers renderBuffers =
                minecraft.renderBuffers();

        OutlineBufferSource outlineBuffer =
                renderBuffers.outlineBufferSource();

        /*
         * ========================================================
         * 设定轮廓颜色
         *
         * 这里暂时用经典绿色。
         * ========================================================
         */
        outlineBuffer.setColor(
                0,
                255,
                0,
                255
        );

        double cameraX =
                camera.getPosition().x;

        double cameraY =
                camera.getPosition().y;

        double cameraZ =
                camera.getPosition().z;

        for (Integer entityId :
                DoorGhostMarkClient.getMarkedEntities()) {

            Entity entity =
                    minecraft.level.getEntity(
                            entityId
                    );

            if (entity == null
                    || entity.isRemoved()) {

                continue;
            }

            double x =
                    entity.getX()
                            - cameraX;

            double y =
                    entity.getY()
                            - cameraY;

            double z =
                    entity.getZ()
                            - cameraZ;

            dispatcher.render(
                    entity,
                    x,
                    y,
                    z,
                    entity.getYRot(),
                    event.getPartialTick().getGameTimeDeltaPartialTick(false),
                    poseStack,
                    outlineBuffer,
                    dispatcher.getPackedLightCoords(
                            entity,
                            event.getPartialTick()
                                    .getGameTimeDeltaPartialTick(false)
                    )
            );
        }

        /*
         * 提交 Outline Buffer。
         */
        outlineBuffer.endOutlineBatch();
    }
}