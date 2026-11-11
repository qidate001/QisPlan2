package com.qidate.qisplan2.ghost.module.host;

import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.GhostModuleCustomDataModifier;
import com.qidate.qisplan2.ghost.module.environment.GhostTargetState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * 目标关系数据的存储适配。
 * 可变模块把关系放入自身 customData；静态模块没有逐方块实例数据，
 * 因此在目标上按模块 ID 保存进度。同类静态地毯共享该目标的累计接触时间。
 * 不使用静态实体映射，离开、区块卸载及读档均不会凭空增加接触时间。
 */
public final class GhostTargetStateAdapters {
    private static final String TARGETS = "qisplan2:targets";
    private static final String STATIC_TARGETS = "qisplan2:static_module_targets";

    /** 工具类只提供绑定入口。 */
    private GhostTargetStateAdapters() {}

    /** 绑定模块的关系状态出口；模块只能读取目标状态，不取得载体对象。 */
    public static GhostTargetState bind(GhostModuleHost host, ResourceLocation id) {
        return new GhostTargetState() {
            /** 可变模块按目标 UUID 隔离；静态模块在每个目标自身的数据中按模块 ID 隔离。 */
            public CompoundTag read(LivingEntity target) {
                if (host instanceof GhostModuleCustomDataModifier modifier)
                    return modifier.getModuleCustomData(id).getCompound(TARGETS)
                            .getCompound(target.getUUID().toString()).copy();
                return target.getPersistentData().getCompound(STATIC_TARGETS).getCompound(id.toString()).copy();
            }

            /** 读取最新外层数据后写入该目标，避免覆盖模块或目标的其他专属字段。 */
            public void write(LivingEntity target, CompoundTag data) {
                if (host instanceof GhostModuleCustomDataModifier modifier) {
                    var custom = modifier.getModuleCustomData(id);
                    var targets = custom.getCompound(TARGETS);
                    targets.put(target.getUUID().toString(), data.copy());
                    custom.put(TARGETS, targets);
                    modifier.setModuleCustomData(id, custom);
                } else {
                    var persistent = target.getPersistentData();
                    var targets = persistent.getCompound(STATIC_TARGETS);
                    targets.put(id.toString(), data.copy());
                    persistent.put(STATIC_TARGETS, targets);
                }
            }
        };
    }
}
