package com.qidate.qisplan2.ghost;

import net.minecraft.server.level.ServerPlayer;

/**
 * 一个可以被驾驭的目标。
 *
 * 驾驭系统本身不关心目标究竟是实体、物品
 * 还是其他形式的灵异存在。
 */
public interface GhostPossessionTarget {

    /**
     * 驾驭成功。
     *
     * @return 是否真正完成了驾驭
     */
    boolean onSuccess(
            ServerPlayer player
    );

    /**
     * 驾驭失败。
     */
    void onFailure(
            ServerPlayer player
    );
}