package com.qidate.qisplan2.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.qidate.qisplan2.entity.AnimatedModularGhostEntity;
import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import com.qidate.qisplan2.ghost.module.host.attachment.GhostAttachments;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/** GeckoLib 宿主的独立附着物表现，读取同一份附着快照，不改变驾驭或压制关系。 */
public final class GhostGeoAttachmentLayer<T extends AnimatedModularGhostEntity> extends GeoRenderLayer<T> {
    /** 独特外观共用本层，无需逐只厉鬼复制棺材钉逻辑。 */
    public GhostGeoAttachmentLayer(GeoRenderer<T> renderer) { super(renderer); }

    /** 在实体局部躯干位置渲染模块声明的附着模型；隐形时与实体模型一同隐藏。 */
    @Override public void render(PoseStack pose,T entity,BakedGeoModel bakedModel,RenderType renderType,
            MultiBufferSource buffers,VertexConsumer buffer,float partialTick,int light,int overlay) {
        if(entity.isInvisible() || buffer==null) return;
        int index=0;
        for(var stack:GhostAttachments.visualItems(entity)) {
            for(var entry:GhostItemData.getModules(stack)) {
                var module=GhostModuleRegistry.get(entry.id());
                if(module==null || module.attachmentModel()==null) continue;
                pose.pushPose();
                pose.translate(0.08+index*0.1,entity.getBbHeight()*0.55,-entity.getBbWidth()*0.4);
                pose.mulPose(Axis.XP.rotationDegrees(-12));
                pose.mulPose(Axis.YP.rotationDegrees(8));
                pose.scale(0.75F,0.75F,0.75F);
                var client=Minecraft.getInstance();
                var model=client.getModelManager().getModel(ModelResourceLocation.standalone(module.attachmentModel()));
                client.getItemRenderer().render(stack,ItemDisplayContext.FIXED,false,pose,buffers,
                        light,OverlayTexture.NO_OVERLAY,model);
                pose.popPose();
                index++;
            }
        }
    }
}
