package com.qidate.qisplan2.client;

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

        closedModel =
                modelManager.getModel(
                        ModelResourceLocation.standalone(
                                ResourceLocation.fromNamespaceAndPath(
                                        QisPlan2.MODID,
                                        "ghost_umbrella_closed"
                                )
                        )
                );

        openModel =
                modelManager.getModel(
                        ModelResourceLocation.standalone(
                                ResourceLocation.fromNamespaceAndPath(
                                        QisPlan2.MODID,
                                        "ghost_umbrella_open"
                                )
                        )
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
         * 这里先不做额外旋转/缩放。
         *
         * 如果 Blockbench 模型在手里方向不对，
         * 再单独调整。
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