package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.death.SupernaturalDamageHelper;
import com.qidate.qisplan2.ghost.module.GhostHostProperties;
import com.qidate.qisplan2.ghost.module.GhostModuleContext;
import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import com.qidate.qisplan2.ghost.module.host.GhostLivingHosts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** 身体承载属性与显式能力效果的物理落地层；携带物品不是身体的内在宿主。 */
@EventBusSubscriber(modid=QisPlan2.MODID)
public final class GhostHostBodyHandler {
    // 沿用旧属性 ID，使旧存档不会同时保留新旧两份生命加成。
    private static final ResourceLocation HEALTH=ResourceLocation.parse("qisplan2:possession_max_health");
    private static final ResourceLocation SPEED=ResourceLocation.parse("qisplan2:module_body_speed");
    private GhostHostBodyHandler(){}
    /** 通用减伤只来自身体自身承载；装备的保护能力仍通过各自媒介与对抗入口执行。 */
    @SubscribeEvent public static void damage(LivingDamageEvent.Pre event){
        var entity=event.getEntity();if(entity.level().isClientSide||SupernaturalDamageHelper.isSupernatural(event.getSource()))return;
        double remaining=1;for(var host:GhostLivingHosts.intrinsic(entity))remaining*=1-GhostHostProperties.damageReduction(host);
        event.setNewDamage((float)(event.getNewDamage()*remaining));
    }
    /** 属性仅在数值变化时更新，短时药水效果通过租期自然收回，不误删其他来源的药水。 */
    @SubscribeEvent public static void tick(EntityTickEvent.Post event){
        if(!(event.getEntity() instanceof LivingEntity entity)||entity.level().isClientSide||!entity.isAlive())return;
        double health=0,speed=0;
        var bindings=GhostLivingHosts.bindings(entity);
        // 身体自身的承载属性不能从手持或穿戴物品继承；物品仍保留自己的模块数据。
        for(var host:GhostLivingHosts.intrinsic(bindings))health+=GhostHostProperties.maxHealthBonus(host);
        // 显式声明且满足媒介的能力效果沿用原有适配，例如物品主动作用于携带者。
        for(var binding:bindings){
            var host=binding.host();
            for(var entry:host.getModules()){
                var module=GhostModuleRegistry.get(entry.id());
                if(module==null||entry.sleeping()||!host.carriesModule(entry.id())||!host.suppression(entry.id()).canUseAbility(host.controlsIntrinsicModules())
                        ||!host.moduleEnvironment(entry.id()).exposed())continue;
                var effects=module.bodyEffects(new GhostModuleContext(host,entry.id()));speed+=effects.movementSpeed();
                if(entity.tickCount%20==0)for(var effect:effects.effects())entity.addEffect(new MobEffectInstance(effect,40,0,false,false,true));
            }
        }
        update(entity,Attributes.MAX_HEALTH,HEALTH,health);update(entity,Attributes.MOVEMENT_SPEED,SPEED,speed);
        if(entity.getHealth()>entity.getMaxHealth())entity.setHealth(entity.getMaxHealth());
    }
    /** 同值不反复标记属性同步；模块卸载后清除其投影。 */
    private static void update(LivingEntity entity,Holder<Attribute> attribute,ResourceLocation id,double value){
        var instance=entity.getAttribute(attribute);if(instance==null)return;var previous=instance.getModifier(id);
        if(value==0){if(previous!=null)instance.removeModifier(id);return;}
        if(previous!=null&&previous.amount()==value)return;
        instance.removeModifier(id);instance.addTransientModifier(new AttributeModifier(id,value,AttributeModifier.Operation.ADD_VALUE));
    }
}
