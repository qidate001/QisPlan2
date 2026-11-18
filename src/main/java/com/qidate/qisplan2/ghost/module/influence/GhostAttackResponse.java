package com.qidate.qisplan2.ghost.module.influence;

import com.qidate.qisplan2.ghost.module.GhostModuleContext;

/** 模块声明的袭击对抗方式。没有声明的模块不会执行任何对抗判断。 */
public interface GhostAttackResponse {
    /** 小值先执行：屏蔽在前，付费抵挡居中，累计型持续保护在后。相同值保持收集顺序。 */
    default int order() { return 100; }

    /** 自行通过语义、媒介和状态决定是否参与；只有支付代价成功才能修改剩余强度。 */
    void respond(GhostModuleContext module, GhostAttackContext attack);
}
