package com.qidate.qisplan2.ghost.module.environment;

/** 主动操作端口描述。描述效果种类，不携带宿主类型或客户端伪造的世界目标。 */
public enum GhostActiveAction {
    DIVINATION_LIFE, DIVINATION_DEATH, DIVINATION_GHOST,
    DOOR_HUNT, MELEE, KNOCK, TOGGLE_DOMAIN,
    DOMAIN_UP, DOMAIN_DOWN, SELF_UP, SELF_DOWN, REBOOT,
    TELEPORT, RAISE_TARGET, LOWER_TARGET
}
