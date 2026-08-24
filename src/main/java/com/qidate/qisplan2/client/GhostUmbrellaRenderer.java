package com.qidate.qisplan2.client;

import com.mojang.math.Axis;
import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.item.GhostUmbrellaItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class GhostUmbrellaRenderer
        extends BlockEntityWithoutLevelRenderer {

    private final Minecraft minecraft;

    private BakedModel closedModel;
    private BakedModel openModel;

    public GhostUmbrellaRenderer() {
        super(
                Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels()
        );

        this.minecraft =
                Minecraft.getInstance();
    }

    private void loadModels() {

        if (closedModel != null
                && openModel != null) {
            return;
        }

        var modelManager =
                minecraft.getModelManager();

        ModelResourceLocation closedLocation =
                ModelResourceLocation.standalone(
                        ResourceLocation.fromNamespaceAndPath(
                                QisPlan2.MODID,
                                "item/ghost_umbrella_closed"
                        )
                );

        ModelResourceLocation openLocation =
                ModelResourceLocation.standalone(
                        ResourceLocation.fromNamespaceAndPath(
                                QisPlan2.MODID,
                                "item/ghost_umbrella_open"
                        )
                );

        QisPlan2.LOGGER.info(
                "[QisPlan2] 加载鬼雨伞模型：{} / {}",
                closedLocation,
                openLocation
        );

        closedModel =
                modelManager.getModel(
                        closedLocation
                );

        openModel =
                modelManager.getModel(
                        openLocation
                );

        QisPlan2.LOGGER.info(
                "[QisPlan2] 鬼雨伞模型结果：closed={}, open={}",
                closedModel,
                openModel
        );
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        loadModels();

        BakedModel model =
                GhostUmbrellaItem.isOpen(stack)
                        ? openModel
                        : closedModel;

        if (model == null) {
            return;
        }

        poseStack.pushPose();

        /*
         * ========================================
         * 第一人称
         * ========================================
         */
        if (displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {

            /*
             * 整体放大 3 倍。
             */
            poseStack.scale(
                    3.0F,
                    3.0F,
                    3.0F
            );

            /*
             * 从手中心附近向上移动。
             */
            poseStack.translate(
                    0.0D,
                    0.15D,
                    0.0D
            );

            /*
             * 让伞柄朝上，
             * 呈现“手持雨伞”的感觉。
             */
            poseStack.mulPose(
                    com.mojang.math.Axis.XP.rotationDegrees(
                            -15.0F
                    )
            );

            /*
             * 稍微向身体外侧偏移。
             */
            if (displayContext
                    == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {

                poseStack.translate(
                        0.2D,
                        0.0D,
                        0.0D
                );

            } else {

                poseStack.translate(
                        0.10D,
                        0.0D,
                        0.0D
                );
            }
        }

        /*
         * ========================================
         * 第三人称
         * ========================================
         */
        /*
         * ========================================
         * 第三人称
         * ========================================
         */
        else if (displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {

            boolean open =
                    GhostUmbrellaItem.isOpen(stack);

            /*
             * ==============================
             * 大小
             * ==============================
             */
            poseStack.scale(
                    2.2F,
                    2.2F,
                    2.2F
            );

            /*
             * ==============================
             * 把伞整体抬到手的位置
             * ==============================
             */
            poseStack.translate(
                    0.11D,
                    0.6D,
                    0.3D
            );

            /*
             * ==============================
             * 让伞竖起来
             * ==============================
             */
            poseStack.mulPose(
                    Axis.XP.rotationDegrees(
                            15.0F
                    )
            );

            /*
             * ==============================
             * 左右手稍微错开
             * ==============================
             */
            if (displayContext
                    == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {

                poseStack.translate(
                        0.12D,
                        0.0D,
                        0.0D
                );

            } else {

                poseStack.translate(
                        -0.12D,
                        0.0D,
                        0.0D
                );
            }
        }

        /*
         * ========================================
         * 物品栏 / GUI
         * ========================================
         */
        else if (displayContext == ItemDisplayContext.GUI) {

            /*
             * 物品栏里的伞缩小一点，
             * 并把模型居中。
             */
            poseStack.scale(
                    0.85F,
                    0.85F,
                    0.85F
            );

            poseStack.translate(
                    0.56D,
                    0.6D,
                    0.0D
            );
        }

        /*
         * ========================================
         * 正常渲染
         * ========================================
         */
        minecraft.getItemRenderer()
                .render(
                        stack,
                        displayContext,
                        false,
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay,
                        model
                );

        poseStack.popPose();
    }
}