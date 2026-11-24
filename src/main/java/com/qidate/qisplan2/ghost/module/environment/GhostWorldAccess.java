package com.qidate.qisplan2.ghost.module.environment;

import java.util.function.BooleanSupplier;
import net.minecraft.world.level.GameRules;

import com.qidate.qisplan2.ghost.module.event.DoorKnockHeardEvent;

import java.util.function.Consumer;

import java.util.UUID;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvent;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 模块对世界的能力端口。只提供环境事实和操作，不返回承载模块的对象。
 * 不具备的物理能力通过 canMove 等查询表达，模块不能据此猜测载体类型。
 * 目标实体是世界中的作用对象，与模块自己的载体不是同一个概念。
 */
public interface GhostWorldAccess {
    /** 可选自主行动端口；缺少寻路身体时返回 null，不影响其他能力出口。 */
    default GhostActor actor() { return null; }

    /** 面向世界坐标。物品忽略；方块仅在具有朝向属性时调整，返回是否应用。 */
    default boolean facePosition(Vec3 target) { return false; }
    /** 面向世界方向向量；零向量和非有限数值无效，不包含位置移动。 */
    default boolean faceDirection(Vec3 direction) { return false; }

    /** 造成原版数值伤害；与灵异袭击 attack 的裁决语义明确分离。 */
    default boolean hurt(LivingEntity target, ResourceKey<DamageType> type, float amount) { return false; }
    /** 对承担动作的身体施加停步效果，具体身体由适配层解释。 */
    default void immobilizeSource() {}

    /** 独立附着来源的建立与媒介查询接口。 */
    default GhostAttachmentAccess attachments() { return GhostAttachmentAccess.NONE; }

    /** 必死诅咒的共享层数、反噬对象及消耗出口。 */
    default GhostDeathCurseAccess deathCurses() { return GhostDeathCurseAccess.NONE; }
    /** 校验交互者是否可以打开指定的能力界面。 */
    default boolean canOpenScreen(ResourceLocation screen, LivingEntity user) { return false; }
    /** 服务端授权打开界面；客户端配置与凭据不会进入网络消息。 */
    default boolean openScreen(ResourceLocation screen, LivingEntity user) { return false; }

    /** 查询相对当前环境位置的 AABB 内目标；适配器使用世界实体空间索引，不遍历方块。 */
    default List<LivingEntity> livingEntities(AABB relativeBounds) { return List.of(); }
    /** 重查目标碰撞箱是否仍与相对范围相交，用于延迟能力的媒介验证。 */
    default boolean inArea(LivingEntity target, AABB relativeBounds) { return false; }
    /** 按通用物品条件检查目标携带物；背包和装备差异由适配器处理。 */
    default boolean hasCarriedItem(LivingEntity target, Predicate<ItemStack> condition) { return false; }
    /** 取得当前模块的工作台出口。 */
    default GhostWorkstationAccess workstations() { return GhostWorkstationAccess.NONE; }
    /** 播放指定粒子与声音的袭击表现，不触发伤害或规则。 */
    default void attackFeedback(LivingEntity target, SimpleParticleType particle,
            SoundEvent sound) {}

    /** 门相关空间事实和世界操作由适配层统一实现。 */
    default GhostDoorAccess doors() { return GhostDoorAccess.NONE; }
    /** 观察者的视野与无遮挡视线是否实际覆盖当前来源。 */
    default boolean sourceVisibleTo(LivingEntity observer) { return false; }
    /** 将目标视角朝实际来源转动，包含俯仰；返回本次实际转角，不能越过来源朝向。 */
    default float turnTargetTowardSource(LivingEntity target, float maximumDegrees) { return 0; }

    /** 目标是否能接收本次喊名；无听力标签与屏蔽由环境解释。 */
    default boolean canHearCall(LivingEntity target) { return false; }

    /** 当前世界时间，供模块保存冷却与复苏间隔。 */
    default long gameTime() { return 0; }
    /** 返回门的有效下半部位置，避免重复操作上半部门。 */
    default BlockPos normalizeDoor(BlockPos pos) { return pos; }
    /** 查询直接听见当前敲门的目标数量，计费策略由模块决定。 */
    default int hearingTargets(BlockPos pos) { return 0; }
    /** 查询是否属于可传播敲门的特殊门。 */
    default boolean supernaturalDoor(BlockPos pos) { return false; }
    /** 摧毁指定方块，不附加技能规则。 */
    default void destroyBlock(BlockPos pos) {}

    /** 当前模块的鬼域操作能力；不提供出口时返回空实现。 */
    default GhostDomainAccess domain() { return GhostDomainAccess.NONE; }

    /** 解析当前输入指向的世界目标，不暴露来源对象。 */
    default LivingEntity aimedTarget(double range) { return null; }

    /** 解析当前输入指向的方块；缺少输入或无命中时返回 null。 */
    default BlockPos aimedBlock(double range) { return null; }

    /** 查询目标是否正在与来源接触；宿主如何形成接触由适配层解释。 */
    default boolean touching(LivingEntity target) { return false; }

    /** 为模块解析标记对应的目标，布尔值表示开门或关门事实。 */
    default LivingEntity doorMarkedTarget(boolean opening) { return null; }

    /** 移动到目标旁的安全位置，返回是否成功。 */
    default boolean approach(LivingEntity target) { return false; }

    /** 消费已经完成追袭的门标记。 */
    default void clearDoorMarks(LivingEntity target) {}

    /** 尝试永久支付最大生命代价；不足时不修改属性，成功后保留指定生命下限。 */
    default boolean consumeMaxHealth(ResourceLocation lossId, double cost, double minimum) { return false; }

    /** 降低当前生命；没有有效作用对象时返回失败，具体属性操作留在适配层。 */
    default boolean drainHealth(float amount) { return false; }

    /**
     * 判断当前来源是否位于服务端世界且满足暴露条件。
     */
    boolean present();

    /**
     * 判断事件中的实体是否对应当前能力来源。
     * 只返回关联关系，不把来源对象交给模块。
     */
    boolean isOrigin(Entity entity);

    /**
     * 判断给定世界是否与当前来源处于同一世界实例，防止跨维度作用。
     */
    boolean sameWorld(Level level);

    /**
     * 查询当前环境是否为白天；当前实现缺少世界时按白天处理。
     */
    boolean isDay();
    /** 位置处的方块光照，不暴露用于定位的具体宿主。 */
    default int blockLight() { return 0; }
    /** 位置处的天空光照。 */
    default int skyLight() { return 0; }

    /**
     * 读取当前环境位置，用于距离和移动计算。
     * 缺少位置时返回零向量；调用方应先检查环境是否有效。
     */
    Vec3 position();

    /**
     * 判断当前环境是否能够接受移动操作。
     * 不具备移动出口时，模块不能假定瞬移或速度设置会产生效果。
     */
    boolean canMove();

    /**
     * 将可移动来源传送到指定坐标。
     * 有自主 AI 时同步朝向目标、停止旧寻路并清除旧攻击目标。
     */
    void teleport(double x, double y, double z, LivingEntity lookAt);

    /**
     * 设置当前可移动来源的速度和朝向。
     * 不具备移动能力时不修改世界状态。
     */
    void move(Vec3 velocity, float yaw);

    /**
     * 停止可用的导航并播放主手挥动动作，供袭击能力表现使用。
     */
    void stopAndSwing();

    /**
     * 检查目标存活、维度、来源排除及环境屏蔽等目标资格。
     * 不合格目标不能继续进入追踪或袭击媒介。
     */
    boolean validTarget(LivingEntity target);

    /**
     * 从本模块 customData 中保存的目标 UUID 恢复生命实体。
     * 目标不存在或已失效时返回 null，不创建第二份持久目标状态。
     */
    LivingEntity target();

    /**
     * 校验目标资格与空间阻隔，判断是否允许继续追踪。
     */
    boolean canTrack(LivingEntity target);

    /**
     * 使用指定伤害类型和强度尝试灵异击杀。
     * 先检查目标世界，再交给统一死亡处理；返回实际击杀是否成功。
     */
    boolean attack(LivingEntity target, ResourceKey<DamageType> type, double intensity);

    /**
     * 判断指定位置是否满足敲门系统认可的门条件。
     */
    boolean isDoor(BlockPos position);

    /**
     * 发起敲门的世界操作，声音传播与后续听声处理由敲门系统负责。
     */
    void knock(BlockPos position, double intensity, BooleanSupplier valid, Consumer<DoorKnockHeardEvent> heard);

    /**
     * 将已准备好的袭击排入敲门延迟队列。
     * 延迟任务执行时仍由准备好的能力调用重新检查有效性，不制造请求型事件。
     */
    void scheduleKnockAttack(Runnable effect);

    /**
     * 在目标位置播放可用的喊名声音。
     * 目标不受支持或没有声音资源时不播放。
     */
    void playCall(LivingEntity target);

    /**
     * 检查目标是否存在可播放的喊名声音；没有对应声音时返回 false。
     */
    boolean hasCall(LivingEntity target);

    /**
     * 判断当前适配器是否提供鬼域开关出口。
     * 要求已声明鬼域、能够持久化并暴露于服务端世界。
     */
    boolean canToggleDomain();

    /**
     * 切换当前模块 customData 中的鬼域开关。
     * 不具备出口时返回 false；开启后返回实际开启状态，关闭成功返回 true。
     */
    boolean toggleDomain();

    /**
     * 检查目标是否仍属于指定鬼域的有效覆盖集合。
     * 同时验证目标资格和鬼域 ID，避免旧曝光事实作用于已更换的鬼域。
     */
    boolean containsInDomain(UUID domainId, LivingEntity target);

    /**
     * 检查适配器是否提供题字能力出口。
     */
    boolean canInscribe();

    /**
     * 把题字操作交给具备题字能力的载体，传入实际交互者。
     * 不支持题字时不执行操作。
     */
    void inscribe(LivingEntity user);

    /** 读取当前世界对接触袭击间隔的配置；无专用配置时使用模块给定的默认 tick 数。 */
    default int integerRule(LivingEntity target, GameRules.Key<GameRules.IntegerValue> key, int fallback) { return fallback; }

    /** 播放接触袭击的世界表现；此接口不计算计时也不触发规则。 */
    void contactAttackFeedback(LivingEntity target);

    /**
     * 以世界目标的位置为搜索中心，按配置扩展附近已有的方块群落。
     * 目标只用于确定世界与坐标，不代表模块载体；返回实际成功新增的方块数量。
     */
    int spreadBlocks(LivingEntity anchor, BlockSpreadSpec spec);
}
