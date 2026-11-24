package com.qidate.qisplan2.ghost.module.implementation.door;

import com.qidate.qisplan2.ghost.module.GhostModuleContext;
import com.qidate.qisplan2.ghost.module.event.DoorChangedEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.Comparator;

/** 门鬼的目标记忆：由模块声明调用，使用目标状态端口，不依赖玩家附件或专属宿主。 */
public final class DoorGhostMemory {
    private DoorGhostMemory() {}

    /** 记录附近触门者；十秒有效期随模块实例保存，开关门各自属于对应模块。 */
    public static void observe(GhostModuleContext context, DoorChangedEvent event, boolean opening) {
        var world = context.world();
        if(event.opening()!=opening || !world.present() || !world.validTarget(event.source())
                || !world.sameWorld(event.source().level())
                || world.position().distanceToSqr(Vec3.atCenterOf(event.doorPos()))>10000) return;
        var state=context.targetState().read(event.source());
        state.putLong("doorMarkExpires",world.gameTime()+200);
        context.targetState().write(event.source(),state);
    }

    /** 从仍有效的标记中选择最近目标；宿主只有坐标和目标查询能力即可参与。 */
    public static LivingEntity nearest(GhostModuleContext context) {
        var world = context.world();
        return world.livingEntities(new AABB(-100,-100,-100,100,100,100)).stream()
                .filter(world::validTarget)
                .filter(target->context.targetState().read(target).getLong("doorMarkExpires")>world.gameTime())
                .min(Comparator.comparingDouble(target->target.position().distanceToSqr(world.position()))).orElse(null);
    }

    /** 完成追袭后仅清除此模块对该目标的标记，不影响其他厉鬼的数据。 */
    public static void clear(GhostModuleContext context, LivingEntity target) {
        var state = context.targetState().read(target);
        state.remove("doorMarkExpires");
        context.targetState().write(target,state);
    }
}
