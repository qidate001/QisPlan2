package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.bearing.HostComposition;
import com.qidate.qisplan2.ghost.bearing.GhostBearing;

import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import com.qidate.qisplan2.ghost.module.environment.GhostTargetState;
import com.qidate.qisplan2.ghost.module.host.GhostTargetStateAdapters;
import com.qidate.qisplan2.ghost.module.environment.GhostEnvironment;
import com.qidate.qisplan2.ghost.module.environment.GhostWorldAccess;
import com.qidate.qisplan2.ghost.module.host.GhostEnvironmentAdapters;
import com.qidate.qisplan2.ghost.module.host.GhostWorldAdapters;

/**
 * 可以承载厉鬼模块的宿主。
 *
 * 定义宿主读取模块数据的能力，以及宿主是否允许
 * 能力刺激修改模块的复苏值。
 *
 * 宿主能够保存模块数据，不代表它必须允许模块状态变化。
 */
public interface GhostModuleHost {
    /** 物品和方块暂用一个万能部位；其他宿主可自行声明任意组成。 */
    default HostComposition composition() { return HostComposition.UNIVERSAL; }

    /** 按当前模块数据推导占位，移除后自然释放，无需维护第二份存档状态。 */
    default GhostBearing.Allocation bearing() { return GhostBearing.allocate(composition(), getModules()); }

    /** 是否真正承载此模块；旧存档中的冲突实例保留数据，但禁止规则和能力运行。 */
    default boolean carriesModule(ResourceLocation id) { return bearing().accepted().contains(id); }

    /** 安装前统一检查指定部位是否存在、是否有容量。 */
    default boolean canInstallModule(ResourceLocation id) {
        return GhostBearing.canInstall(composition(), getModules(), id);
    }


    /**
     * 获取宿主携带的全部厉鬼模块。
     */
    List<GhostModuleData.Entry> getModules();

    /** 为模块提供目标关联状态；新增载体可覆盖其存储实现。 */
    default GhostTargetState moduleTargetState(ResourceLocation id) {
        return GhostTargetStateAdapters.bind(this, id);
    }

    /** 载体解释暴露、消耗和效果输出；新增载体无需修改厉鬼定义。 */
    default GhostEnvironment moduleEnvironment(ResourceLocation id) {
        return GhostEnvironmentAdapters.bind(this, id);
    }

    /** 载体只提供世界操作端口，不把自身交给模块。 */
    default GhostWorldAccess moduleWorld(ResourceLocation id) {
        return GhostWorldAdapters.bind(this, id);
    }


    /**
     * 判断宿主是否携带指定模块。
     */
    default boolean hasModule(ResourceLocation moduleId) {
        return getModules().stream()
                .anyMatch(entry -> entry.id().equals(moduleId));
    }

    /**
     * 判断宿主是否允许能力刺激修改复苏值。
     *
     * 默认允许，以保持现有可变宿主的兼容性。
     * 不可变宿主必须覆盖此方法并返回 false。
     *
     * 此方法只约束能力刺激结算，不代表宿主必然参与
     * 自然复苏，也不代表宿主的其他模块属性可以修改。
     */
    default boolean allowsAbilityRevivalStimulation() {
        return true;
    }
}