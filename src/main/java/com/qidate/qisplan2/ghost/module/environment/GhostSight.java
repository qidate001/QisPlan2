package com.qidate.qisplan2.ghost.module.environment;

import net.minecraft.world.phys.Vec3;

/** 统一视野几何；遮挡和实体资格由世界适配器额外检查。 */
public final class GhostSight {
    private GhostSight() {}
    /** 前方半角六十度，使用三维向量，抬头或低头看不到来源时不触发。 */
    public static boolean inView(Vec3 look, Vec3 toSource) {
        return look.lengthSqr() > 1.0e-8 && toSource.lengthSqr() > 1.0e-8
                && look.normalize().dot(toSource.normalize()) >= 0.5;
    }
}
