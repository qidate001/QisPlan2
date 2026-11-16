package com.qidate.qisplan2.ghost.module.environment;

/** 路由风险判定，不选择任意目的门，也不接触世界对象。 */
public enum DoorTransitOutcome {
    NO_DESTINATION, TELEPORT, VOID, SPLIT;
    /** 计数指排除来源并按实际门去重后的目的门数；多目的门优先判定为撕裂。 */
    public static DoorTransitOutcome decide(int destinations, boolean singleDoorOpen) {
        if (destinations < 0) throw new IllegalArgumentException("Negative destination count");
        if (destinations == 0) return NO_DESTINATION;
        if (destinations > 1) return SPLIT;
        return singleDoorOpen ? TELEPORT : VOID;
    }
}
