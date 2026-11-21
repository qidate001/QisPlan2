package com.qidate.qisplan2.client.model;

import com.qidate.qisplan2.entity.AnimatedModularGhostEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** 按实体外观类型定位资源。模块的增删不会改变模型，也不按所装模块猜测身份。 */
public final class AnimatedModularGhostModel<T extends AnimatedModularGhostEntity> extends GeoModel<T> {
    /** 同名资源约定让外观实体无需再声明一套模型定位代码。 */
    private ResourceLocation resource(T entity,String prefix,String suffix) {
        var id=BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(),prefix+id.getPath()+suffix);
    }
    /** 读取该实体类型的 GeckoLib 骨骼模型。 */
    @Override public ResourceLocation getModelResource(T entity) { return resource(entity,"geo/",".geo.json"); }
    /** 读取用户提供的实体材质。 */
    @Override public ResourceLocation getTextureResource(T entity) { return resource(entity,"textures/entity/",".png"); }
    /** 读取该实体的通用动作实现。 */
    @Override public ResourceLocation getAnimationResource(T entity) { return resource(entity,"animations/entity/",".animation.json"); }
}
