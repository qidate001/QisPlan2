package com.qidate.qisplan2.ghost.module.environment;

import java.util.UUID;
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
     * 推进已有夜游鬼猎杀行为的世界操作。
     * 当前适配器仅在具备模块化实体行动出口时驱动该行为。
     */
    void tickHunt();

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
    void knock(BlockPos position, double intensity);

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

    /** 是否有可持久化并可定位的鬼域出口。 */
    boolean canMaintainDomain();

    /**
     * 判断当前适配器是否提供鬼域开关出口。
     * 当前实现要求有效的服务端玩家模块载体。
     */
    boolean canToggleDomain();

    /**
     * 通过已有鬼眼控制器切换鬼域开关。
     * 不具备出口时返回 false；开启后返回实际开启状态，关闭成功返回 true。
     */
    boolean toggleDomain();

    /**
     * 维护鬼雾运行时鬼域。
     * 实体来源创建或保持对应鬼域，玩家来源沿用持久开关状态驱动控制器。
     */
    void maintainDomain();

    /**
     * 检查目标是否仍属于指定鬼域的有效覆盖集合。
     * 同时验证目标资格和鬼域 ID，避免旧曝光事实作用于已更换的鬼域。
     */
    boolean containsInDomain(UUID domainId, LivingEntity target);

    /**
     * 在当前鬼域存在时委托控制器结算鬼雾成长。
     * 调用方应仅在袭击实际成功后调用。
     */
    void growDomain();

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
    default int contactIntervalTicks(LivingEntity target, int fallback) { return fallback; }

    /** 播放接触袭击的世界表现；此接口不计算计时也不触发规则。 */
    void contactAttackFeedback(LivingEntity target);

    /**
     * 以世界目标的位置为搜索中心，按配置扩展附近已有的方块群落。
     * 目标只用于确定世界与坐标，不代表模块载体；返回实际成功新增的方块数量。
     */
    int spreadBlocks(LivingEntity anchor, BlockSpreadSpec spec);
}
