package com.qidate.qisplan2.ghost.module.environment;

import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import net.minecraft.world.entity.LivingEntity;

/** 必死诅咒的目标状态出口；反噬只提供不透明作用对象，不返回来源载体。 */
public interface GhostDeathCurseAccess {
    GhostDeathCurseAccess NONE = new GhostDeathCurseAccess() {};
    interface Subject {
        /** 读取目标身上共享的诅咒层数。 */
        int stacks();
        /** 更新层数，并由适配层同步旧有 HUD 数据。 */
        void stacks(int value);
        /** 对该对象结算灵异裁决与袭击；这里只执行模块已决定的效果。 */
        void strike(GhostAbilityContext context, double intensity, boolean backlash);
    }
    /** 将合法近距离目标解释为可施加诅咒的对象。 */
    default Subject target(LivingEntity target) { return null; }
    /** 返回反噬作用端口，模块不能借此获取来源实体或宿主类型。 */
    default Subject backlash() { return null; }
    /** 提供随机抽样；反噬概率由模块决定。 */
    default double random() { return 1; }
    /** 触发十层后的物理消耗由载体解释；物品破碎，没有物品形态时不凭空删除模块。 */
    default void exhaust() {}
}
