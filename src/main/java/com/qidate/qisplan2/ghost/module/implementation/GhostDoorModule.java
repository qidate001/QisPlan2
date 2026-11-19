package com.qidate.qisplan2.ghost.module.implementation;

import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.*;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import com.qidate.qisplan2.death.ModDamageTypes;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** 完整鬼门：开门后随机等待，关门时袭击门前、门后与门中目标。 */
public final class GhostDoorModule implements GhostModule {
    public static final ResourceLocation ID = ResourceLocation.parse("qisplan2:ghost_door");
    public static final ResourceLocation CLOSE = ResourceLocation.parse("qisplan2:ghost_door/close_attack");
    /** 返回模块标识。 */
    public ResourceLocation getId() { return ID; }
    /** 初始强度为 20，后续始终读取模块实例强度。 */
    public GhostModuleInitialData getInitialData() { return new GhostModuleInitialData(20, 0); }

    /** 站在门前属于硬性媒介；开门计时属于规则。 */
    public List<GhostMedium> getMedia() {
        return List.of(new GhostMedium() {
            /** 返回媒介标识。 */
            public ResourceLocation getId() { return ResourceLocation.parse("qisplan2:ghost_door/in_front"); }
            /** 门必须真实打开，且处于环境报告的正面作用范围。 */
            public boolean allows(GhostAbilityContext c) {
                var doors = c.getModuleContext().world().doors();
                return doors.frontDoor() != null && doors.open(doors.frontDoor());
            }
        });
    }

    /** 同一扇门明确发生开关变化时清除旧计时，避免同一 tick 手动关开沿用上一轮截止时间。 */
    public void onAbilityEvent(GhostModuleContext context, GhostEvent event) {
        if (!(event instanceof DoorChangedEvent changed)) return;
        var data = context.getCustomData();
        if (data.contains("timedDoor") && data.getLong("timedDoor") == changed.doorPos().asLong()) {
            data.remove("closeAt"); data.remove("timedDoor"); context.setCustomData(data);
        }
    }

    /** 自动行为只走规则运行时，因此死机与沉睡后停止。 */
    public List<GhostRule> getRules() {
        return List.of(new GhostRule() {
            /** 返回计时规则标识。 */
            public ResourceLocation getId() { return ResourceLocation.parse("qisplan2:ghost_door/auto_close"); }
            /** 接受周期语义。 */
            public boolean supports(GhostEvent e) { return e instanceof ModuleTickEvent; }
            /** 只在暴露的世界环境中计时。 */
            public boolean canTrigger(GhostRuleContext c, GhostEvent e) { return c.getModuleContext().world().present(); }
            /** 沉睡期间只维护开门计时；到点且射线上有人才惊醒，不关门、不袭击。 */
            public boolean disturbedWhileSleeping(GhostRuleContext c, GhostEvent event) {
                var m = c.getModuleContext(); var doors = m.world().doors(); var pos = doors.frontDoor();
                var data = m.getCustomData();
                if (pos == null || !doors.open(pos)) {
                    if (data.contains("closeAt")) { data.remove("closeAt"); data.remove("timedDoor"); m.setCustomData(data); }
                    return false;
                }
                long now = m.world().gameTime();
                if (!data.contains("closeAt") || data.getLong("timedDoor") != pos.asLong()) {
                    data.putLong("timedDoor", pos.asLong());
                    data.putLong("closeAt", now + ThreadLocalRandom.current().nextInt(300, 801));
                    m.setCustomData(data); return false;
                }
                if (now < data.getLong("closeAt")) return false;
                data.remove("closeAt"); m.setCustomData(data);
                return doors.attackLine(pos, 16).stream().anyMatch(target -> !m.world().isOrigin(target));
            }
            /** 新门或新一轮开启重新随机计时；手动关闭会清除本轮，避免旧延迟误关新一轮。 */
            public void onTrigger(GhostRuleContext c, GhostEvent e) {
                var m = c.getModuleContext(); var doors = m.world().doors(); var pos = doors.frontDoor();
                var data = m.getCustomData();
                if (pos == null || !doors.open(pos)) {
                    if (data.contains("closeAt")) { data.remove("closeAt"); data.remove("timedDoor"); m.setCustomData(data); }
                    return;
                }
                long now = m.world().gameTime();
                if (!data.contains("closeAt") || data.getLong("timedDoor") != pos.asLong()) {
                    data.putLong("timedDoor", pos.asLong());
                    data.putLong("closeAt", now + ThreadLocalRandom.current().nextInt(300, 801));
                    m.setCustomData(data);
                } else if (now >= data.getLong("closeAt")) {
                    data.remove("closeAt"); m.setCustomData(data);
                    var run = c.prepareAbility(CLOSE, e); if (run != null) run.run();
                }
            }
        });
    }

    /** 公共关门袭击：主动调用绕过计时，但仍校验正面媒介。 */
    public List<GhostAbility> getIntrinsicAbilities() {
        return List.of(new GhostAbility() {
            /** 返回独立技能标识。 */
            public ResourceLocation getId() { return CLOSE; }
            /** 使用原版门图标，避免依赖不存在的纹理。 */
            public ResourceLocation iconTexture() { return ResourceLocation.parse("qisplan2:textures/item/ghost_door.png"); }
            /** 媒介负责门状态校验。 */
            public boolean canExecute(GhostAbilityContext c) { return true; }
            /** 兼容无返回值的执行入口。 */
            public void execute(GhostAbilityContext c) { executeWithResult(c); }
            /** 先完成一次关门，再为每位目标分别收集保护并进行灵异裁决。 */
            public boolean executeWithResult(GhostAbilityContext c) {
                var m = c.getModuleContext(); var doors = m.world().doors(); var pos = doors.frontDoor();
                if (pos == null || !doors.close(pos)) return false;
                var data = m.getCustomData(); data.remove("closeAt"); data.remove("timedDoor"); m.setCustomData(data);
                for (var target : doors.attackLine(pos, 16)) {
                    if (!c.isModuleAvailable()) break;
                    if (m.world().isOrigin(target)) continue;
                    var influence = GhostAbilityInfluence.of(target, GhostInfluenceType.ATTACK, m.intensity());
                    if (influence != null) influence.execute(c, () -> m.world().attack(target, ModDamageTypes.GHOST_DOOR, m.intensity()));
                }
                return true;
            }
        });
    }
}
