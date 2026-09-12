package com.qidate.qisplan2.ghost.domain;

public final class GhostDomainPriority {

    private GhostDomainPriority() {
    }

    /**
     * 判断 domainA 是否可以覆盖 domainB。
     *
     * 优先级：
     * 1. 层数
     * 2. 强度
     */
    public static boolean canOverride(
            GhostDomain domainA,
            GhostDomain domainB
    ) {

        if (domainA.getLayer() != domainB.getLayer()) {
            return domainA.getLayer() > domainB.getLayer();
        }

        return domainA.getStrength() > domainB.getStrength();
    }
}