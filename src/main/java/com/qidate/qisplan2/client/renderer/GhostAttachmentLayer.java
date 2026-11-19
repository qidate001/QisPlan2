package com.qidate.qisplan2.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import com.qidate.qisplan2.ghost.module.host.attachment.GhostAttachments;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;

/** 独立附着物渲染层。人形跟随胸部骨骼，其余原版模型使用躯干附近的保守锚点。 */
public final class GhostAttachmentLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    /** 使用宿主渲染器现有模型，不替换皮肤与姿态。 */
    public GhostAttachmentLayer(RenderLayerParent<T, M> parent) { super(parent); }

    /** 只渲染服务端已同步的附着快照，模型纹理与物品本身使用同一资源。 */
    @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity,
            float limbSwing, float limbAmount, float partialTick, float age, float yaw, float pitch) {
        if (entity.isInvisible()) return;
        int index = 0;
        for (var stack : GhostAttachments.visualItems(entity)) {
            for (var entry : GhostItemData.getModules(stack)) {
                var module = GhostModuleRegistry.get(entry.id());
                if (module == null || module.attachmentModel() == null) continue;
                pose.pushPose();
                if (getParentModel() instanceof HumanoidModel<?> humanoid) {
                    humanoid.body.translateAndRotate(pose);
                    pose.translate(0.08 + index * 0.1, 0.24, -0.15);
                } else {
                    pose.translate(0, 0.55, -entity.getBbWidth() * 0.4);
                }
                // 模型沿 Z 轴伸展，尖端朝身体内部；略微倾斜避免像悬浮的平面图标。
                pose.mulPose(Axis.XP.rotationDegrees(-12));
                pose.mulPose(Axis.YP.rotationDegrees(8));
                pose.scale(0.75f, 0.75f, 0.75f);
                var client = Minecraft.getInstance();
                var model = client.getModelManager().getModel(ModelResourceLocation.standalone(module.attachmentModel()));
                client.getItemRenderer().render(stack, ItemDisplayContext.FIXED, false, pose, buffers,
                        light, OverlayTexture.NO_OVERLAY, model);
                pose.popPose();
                index++;
            }
        }
    }
}
