package com.qidate.qisplan2.ghost.module.environment;

import net.minecraft.util.RandomSource;

import com.qidate.qisplan2.ghost.module.ability.GhostAbilityParameters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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

/** 自主行动所需的物理端口，不返回来源实体或具体宿主；模块行为只能操作接口提供的能力。 */
public interface GhostActor {
    /** 读取当前世界事实。 */
    Level level();
    /** 读取来源空间位置。 */
    Vec3 position();
    /** 读取行动范围的碰撞箱。 */
    AABB getBoundingBox();
    /** 返回稳定的行动来源标识，不返回来源对象。 */
    UUID getUUID();
    /** 提供寻路控制能力。 */
    PathNavigation getNavigation();
    /** 提供视线控制能力。 */
    LookControl getLookControl();
    /** 提供目标感知能力。 */
    Sensing getSensing();
    /** 读取当前行动目标。 */
    LivingEntity getTarget();
    /** 设置当前行动目标。 */
    void setTarget(LivingEntity target);
    /** 提供身体属性的物理接口。 */
    AttributeInstance getAttribute(Holder<Attribute> attribute);
    /** 判断当前模块是否仍允许自主行动。 */
    boolean isActive();
    /** 排除来源自身，调用方无需取得宿主。 */
    boolean isOrigin(Entity entity);
    /** 读取重力开关以便结束行为时恢复。 */
    boolean isNoGravity();
    /** 修改重力开关。 */
    void setNoGravity(boolean value);
    /** 读取碰撞开关。 */
    boolean noPhysics();
    /** 修改碰撞开关。 */
    void setNoPhysics(boolean value);
    /** 设置行动速度。 */
    void setDeltaMovement(Vec3 movement);
    /** 执行位置移动。 */
    void teleportTo(double x,double y,double z);
    /** 查询目标是否允许被感知。 */
    boolean canDetectPlayer(Player player);
    /** 查询目标是否允许被攻击。 */
    boolean canAttackPlayer(Player player);
    /** 读取目标优先级。 */
    int getPlayerTargetPriority(Player player);
    /** 查询环境效果提供的优先目标。 */
    LivingEntity preferredTarget(double range,Predicate<LivingEntity> predicate);
    /** 读取绑定模块的专属数据副本。 */
    CompoundTag getModuleCustomData(ResourceLocation id);
    /** 写回绑定模块的数据，不能探查其他模块。 */
    void setModuleCustomData(ResourceLocation id,CompoundTag data);
    /** 检查绑定实例是否仍存在。 */
    boolean hasGhostModule(ResourceLocation id);
    /** 修改绑定模块的强度。 */
    void setIntensity(ResourceLocation id,double intensity);
    /** 经统一能力执行器提交自主行为请求。 */
    void execute(ResourceLocation ability,GhostAbilityParameters parameters);
    /** 发布目标已处于附近的语义事实。 */
    void nearby(LivingEntity target);
    /** 查询移动候选位置的碰撞，适配器负责排除来源自身。 */
    boolean noCollision(AABB box);
    /** 由物理适配层寻找可行走的位置。 */
    Vec3 randomPosition(int horizontal, int vertical);
    /** 行动随机源与时间，不暴露来源实体。 */
    RandomSource getRandom();
    /** 读取物理行动时间。 */
    int age();
    /** 当前模块强度，只读自身实例。 */
    double intensity();
    /** 几何便利方法由坐标事实派生，不要求实体宿主。 */
    default BlockPos blockPosition(){return BlockPos.containing(position());}
    /** 读取空间横坐标。 */
    default double getX(){return position().x;}
    /** 读取空间高度。 */
    default double getY(){return position().y;}
    /** 读取空间纵坐标。 */
    default double getZ(){return position().z;}
    /** 计算与目标的距离平方。 */
    default double distanceToSqr(Entity target){return position().distanceToSqr(target.position());}
    /** 计算与目标的距离平方。 */
    default double distanceToSqr(Vec3 target){return position().distanceToSqr(target);}
    /** 计算与目标的距离平方。 */
    default double distanceToSqr(double x,double y,double z){return position().distanceToSqr(x,y,z);}
}
