package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.environment.GhostWorldAccess;
import com.qidate.qisplan2.ghost.module.environment.GhostEnvironment;
import net.minecraft.nbt.CompoundTag;

/** 规则的服务端口：不提供宿主引用、载体类型或来源实体。 */
public interface GhostRuleServices {
    /** 环境查询和物理操作由中间层适配。 */
    GhostWorldAccess world();

    /** 查询通用能力媒介和暴露条件。 */
    GhostEnvironment environment();

    /** 当前规则自己的持久数据副本，不能混入其他规则的状态。 */
    CompoundTag state();

    /** 写回当前规则状态；与模块其他 customData 字段合并。 */
    boolean saveState(CompoundTag state);
}
