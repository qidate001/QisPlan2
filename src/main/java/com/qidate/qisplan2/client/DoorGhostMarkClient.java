package com.qidate.qisplan2.client;

import java.util.HashSet;
import java.util.Set;

public final class DoorGhostMarkClient {

    /**
     * 当前这个客户端需要显示高亮的实体 ID。
     */
    private static final Set<Integer> MARKED =
            new HashSet<>();

    private DoorGhostMarkClient() {
    }

    /**
     * 服务端告诉客户端：
     *
     * 某个实体开始 / 停止被开关门鬼标记。
     */
    public static void apply(
            int entityId,
            boolean marked
    ) {

        if (marked) {

            MARKED.add(
                    entityId
            );

        } else {

            MARKED.remove(
                    entityId
            );
        }
    }

    /**
     * 获取当前所有被标记的实体。
     *
     * 返回副本，避免渲染过程中修改原集合。
     */
    public static Set<Integer> getMarkedEntities() {

        return Set.copyOf(
                MARKED
        );
    }

    /**
     * 当前是否存在任何标记。
     */
    public static boolean isEmpty() {

        return MARKED.isEmpty();
    }

    /**
     * 清空全部标记。
     *
     * 切换世界、退出服务器等场景可以调用。
     */
    public static void clear() {

        MARKED.clear();
    }
}