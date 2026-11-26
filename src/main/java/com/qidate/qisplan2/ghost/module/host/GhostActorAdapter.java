package com.qidate.qisplan2.ghost.module.host;

import net.minecraft.world.entity.ai.util.LandRandomPos;

import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.GhostAvoidanceSystem;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.environment.GhostActor;
import com.qidate.qisplan2.ghost.module.event.EntityNearbyEvent;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensing;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;
import java.util.function.Predicate;

/** 自主行动适配器只认识物理能力；这里没有任何厉鬼 ID、规则或专属行为选择表。 */
public record GhostActorAdapter(GhostModuleHost host, ResourceLocation module, PathfinderMob body) implements GhostActor {
    /** 无自主寻路身体的宿主不提供此端口，主动能力仍可正常使用其他世界出口。 */
    public static GhostActor bind(GhostModuleHost host,ResourceLocation module){
        var body=GhostEnvironmentAdapters.bearer(host);return body instanceof PathfinderMob mob?new GhostActorAdapter(host,module,mob):null;
    }
    /** 读取当前世界事实。 */
    public Level level(){return body.level();}
    /** 读取来源空间位置。 */
    public Vec3 position(){return body.position();}
    /** 读取行动范围的碰撞箱。 */
    public AABB getBoundingBox(){return body.getBoundingBox();}
    /** 返回稳定的行动来源标识，不返回来源对象。 */
    public UUID getUUID(){return body.getUUID();}
    /** 提供寻路控制能力。 */
    public PathNavigation getNavigation(){return body.getNavigation();}
    /** 提供视线控制能力。 */
    public LookControl getLookControl(){return body.getLookControl();}
    /** 提供目标感知能力。 */
    public Sensing getSensing(){return body.getSensing();}
    /** 读取当前行动目标。 */
    public LivingEntity getTarget(){return body.getTarget();}
    /** 设置当前行动目标。 */
    public void setTarget(LivingEntity target){body.setTarget(target);}
    /** 提供身体属性的物理接口。 */
    public AttributeInstance getAttribute(Holder<Attribute> attribute){return body.getAttribute(attribute);}
    /** 判断当前模块是否仍允许自主行动。 */
    public boolean isActive(){return host.carriesModule(module)&&body.isAlive()&&!body.isRemoved()&&host.suppression(module).canAct()
            &&host.getModules().stream().anyMatch(e->e.id().equals(module)&&GhostModuleCombatState.available(e));}
    /** 排除来源自身，调用方无需取得宿主。 */
    public boolean isOrigin(Entity entity){return entity==body;}
    /** 读取重力开关以便结束行为时恢复。 */
    public boolean isNoGravity(){return body.isNoGravity();}
    /** 修改重力开关。 */
    public void setNoGravity(boolean value){body.setNoGravity(value);}
    /** 读取碰撞开关。 */
    public boolean noPhysics(){return body.noPhysics;}
    /** 修改碰撞开关。 */
    public void setNoPhysics(boolean value){body.noPhysics=value;}
    /** 设置行动速度。 */
    public void setDeltaMovement(Vec3 movement){body.setDeltaMovement(movement);}
    /** 执行位置移动。 */
    public void teleportTo(double x,double y,double z){body.teleportTo(x,y,z);}
    /** 查询目标是否允许被感知。 */
    public boolean canDetectPlayer(Player player){return !(body instanceof ModularGhostEntity ghost)||ghost.canDetectPlayer(player);}
    /** 查询目标是否允许被攻击。 */
    public boolean canAttackPlayer(Player player){return !(body instanceof ModularGhostEntity ghost)||ghost.canAttackPlayer(player);}
    /** 读取目标优先级。 */
    public int getPlayerTargetPriority(Player player){return body instanceof ModularGhostEntity ghost?ghost.getPlayerTargetPriority(player):0;}
    /** 查询环境效果提供的优先目标。 */
    public LivingEntity preferredTarget(double range,Predicate<LivingEntity> predicate){return GhostAvoidanceSystem.preferredTarget(body,range,predicate);}
    public boolean noCollision(AABB box){return body.level().noCollision(body,box);}
    public Vec3 randomPosition(int horizontal,int vertical){return LandRandomPos.getPos(body,horizontal,vertical);}
    public RandomSource getRandom(){return body.getRandom();}
    /** 读取物理行动时间。 */
    public int age(){return body.tickCount;}
    public double intensity(){return new GhostModuleContext(host,module).intensity();}
    /** 模块只能读写自己声明范围内的数据，不能从行动端口探查其他模块。 */
    public CompoundTag getModuleCustomData(ResourceLocation id){return module.equals(id)&&host instanceof GhostModuleCustomDataModifier m?m.getModuleCustomData(id):new CompoundTag();}
    /** 写回绑定模块的数据，不能探查其他模块。 */
    public void setModuleCustomData(ResourceLocation id,CompoundTag data){if(module.equals(id)&&host instanceof GhostModuleCustomDataModifier m)m.setModuleCustomData(id,data);}
    /** 检查绑定实例是否仍存在。 */
    public boolean hasGhostModule(ResourceLocation id){return module.equals(id)&&host.hasModule(id);}
    /** 修改绑定模块的强度。 */
    public void setIntensity(ResourceLocation id,double value){if(module.equals(id)&&host instanceof GhostModuleDataModifier m)m.setIntensity(id,value);}
    /** 行动请求仍经过统一执行器，不能直接越过压制、媒介或死机状态。 */
    public void execute(ResourceLocation ability,GhostAbilityParameters parameters){GhostAbilityExecutor.tryExecuteWithResult(new GhostAbilityContext(host,module,ability,GhostAbilityContext.Source.BEHAVIOR,null,()->{},parameters));}
    /** 将附近目标转换成已经发生的空间事实。 */
    public void nearby(LivingEntity target){GhostModuleRuntime.dispatch(host,new EntityNearbyEvent(target));}
}
