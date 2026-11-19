package com.qidate.qisplan2.ghost.module.implementation;

import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.*;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.ghost.module.environment.DoorTransitOutcome;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import java.util.List;

/** 完整鬼门牌：按模块中的号码连接门，传送与危险后果共用同一个能力。 */
public final class GhostDoorPlateModule implements GhostModule {
    public static final ResourceLocation ID = ResourceLocation.parse("qisplan2:ghost_door_plate");
    public static final ResourceLocation TRAVEL = ResourceLocation.parse("qisplan2:ghost_door_plate/travel");
    public static final ResourceLocation NUMBER = ResourceLocation.parse("qisplan2:ghost_door_plate/set_number");
    public static final int DEFAULT_NUMBER = 666;

    /** 返回模块标识。 */
    public ResourceLocation getId() { return ID; }
    /** 保留旧门牌基础袭击强度。 */
    public GhostModuleInitialData getInitialData() { return new GhostModuleInitialData(20, 0); }
    /** 端点登记是环境维护，不自行传送，也不绕过死机启动规则。 */
    public void tickAbilities(GhostModuleContext context) { context.world().doors().maintainLink(number(context)); }
    /** 读取号码时兼容没有初始化专属字段的新模块。 */
    public static int number(GhostModuleContext m) { var d = m.getCustomData(); return d.contains("number") ? d.getInt("number") : DEFAULT_NUMBER; }
    /** 穿门者来自语义事件或可信主动调用参数，不查询来源载体。 */
    private static LivingEntity target(GhostAbilityContext c) {
        return c.event() instanceof DoorEnteredEvent e ? e.target()
                : c.parameters() instanceof GhostInvocationParameters p ? p.recipient() : null;
    }

    /** 观察门洞中的实体，交给相同的能力校验与执行链。 */
    public List<GhostRule> getRules() {
        return List.of(new GhostRule() {
            /** 返回规则标识。 */
            public ResourceLocation getId() { return ResourceLocation.parse("qisplan2:ghost_door_plate/entry"); }
            /** 接受周期观察。 */
            public boolean supports(GhostEvent e) { return e instanceof ModuleTickEvent; }
            /** 只扫描暴露的世界环境。 */
            public boolean canTrigger(GhostRuleContext c, GhostEvent e) { return c.getModuleContext().world().present(); }
            /** 进入已打开的关联门洞才触犯沉睡规则；不执行传送。 */
            public boolean disturbedWhileSleeping(GhostRuleContext c, GhostEvent event) {
                var doors = c.getModuleContext().world().doors(); var pos = doors.linkedDoor();
                return pos != null && doors.open(pos) && doors.nearby(pos).stream().anyMatch(entity -> doors.inside(entity, pos));
            }
            /** 所有生命实体都使用相同门洞判定，不局限于玩家。 */
            public void onTrigger(GhostRuleContext c, GhostEvent e) {
                var doors = c.getModuleContext().world().doors(); var pos = doors.linkedDoor();
                if (pos == null || !doors.open(pos)) return;
                for (var entity : doors.nearby(pos)) if (doors.inside(entity, pos)) {
                    var run = c.prepareAbility(TRAVEL, new DoorEnteredEvent(entity, pos));
                    if (run != null) run.run();
                }
            }
        });
    }

    /** 分别公开传送与修改号码；编辑号码不继承穿门媒介。 */
    public List<GhostAbility> getIntrinsicAbilities() {
        return List.of(new GhostAbility() {
            /** 返回传送标识。 */
            public ResourceLocation getId() { return TRAVEL; }
            /** 声明传送图标。 */
            public ResourceLocation iconTexture() { return ResourceLocation.parse("minecraft:textures/item/ender_pearl.png"); }
            /** 只有站在关联门洞且门开启时，才满足传送媒介。 */
            public List<GhostMedium> media(GhostAbilityContext ignored) {
                return List.of(new GhostMedium() {
                    /** 返回门洞媒介标识。 */
                    public ResourceLocation getId() { return ResourceLocation.parse("qisplan2:ghost_door_plate/inside"); }
                    /** 始终重新检查实时位置，拒绝失效的延迟事件。 */
                    public boolean allows(GhostAbilityContext c) {
                        var d = c.getModuleContext().world().doors(); var pos = d.linkedDoor();
                        return pos != null && d.open(pos) && d.inside(target(c), pos);
                    }
                });
            }

            /** 路由可以没有目的门，执行时返回失败，不选择任意目的地。 */
            public boolean canExecute(GhostAbilityContext c) { return target(c) != null; }
            /** 兼容无返回值入口。 */
            public void execute(GhostAbilityContext c) { executeWithResult(c); }
            /** 多目的门只袭击不传送；单个关闭目的门经裁决后送入虚空。 */
            public boolean executeWithResult(GhostAbilityContext c) {
                var m = c.getModuleContext(); var d = m.world().doors(); var t = target(c);
                var routes = d.destinations(number(m), d.linkedDoor());
                var outcome = DoorTransitOutcome.decide(routes.size(), !routes.isEmpty() && routes.getFirst().open());
                if (outcome == DoorTransitOutcome.NO_DESTINATION) return false;
                d.cooldown(t);
                if (outcome == DoorTransitOutcome.SPLIT) {
                    double strength = m.intensity() * (routes.size() - 1);
                    var hit = GhostAbilityInfluence.of(t, GhostInfluenceType.ATTACK, strength);
                    return hit != null && hit.execute(c, () -> m.world().attack(t, ModDamageTypes.GHOST_DOOR_PLATE, strength));
                }
                var route = routes.getFirst();
                if (outcome == DoorTransitOutcome.VOID) {
                    var hit = GhostAbilityInfluence.of(t, GhostInfluenceType.ATTACK, m.intensity());
                    return hit != null && hit.execute(c, () -> d.travel(t, route, true));
                }
                return d.travel(t, route, false);
            }
        }, new GhostAbility() {
            /** 返回编辑号码技能标识。 */
            public ResourceLocation getId() { return NUMBER; }
            /** 使用命名牌图标。 */
            public ResourceLocation iconTexture() { return ResourceLocation.parse("minecraft:textures/item/name_tag.png"); }
            /** 通用数字输入由模块声明，UI 与网络不识别鬼门牌类型。 */
            public GhostIntegerInput integerInput() { return new GhostIntegerInput(0, 999999999, DEFAULT_NUMBER); }
            /** 显示已保存号码；只有没有 number 字段的新实例才回退为 666。 */
            public int integerInputInitial(GhostModuleContext context) { return number(context); }
            /** 无需站在门中即可编辑自己的模块号码。 */
            public boolean canExecute(GhostAbilityContext c) {
                return c.parameters() instanceof GhostInvocationParameters p && p.number() != null && integerInput().accepts(p.number());
            }
            /** 将号码保存在此模块实例的专属数据中。 */
            public void execute(GhostAbilityContext c) {
                var m = c.getModuleContext();
                var data = m.getCustomData();
                data.putInt("number", ((GhostInvocationParameters)c.parameters()).number()); m.setCustomData(data);
            }
        });
    }
}
