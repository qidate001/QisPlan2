package com.qidate.qisplan2.ghost.module.influence;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 灵异影响互斥关系注册表。
 *
 * 负责注册和查询影响类型之间的互斥关系。
 *
 * 互斥关系具有对称性：
 * 如果 A 与 B 互斥，那么 B 与 A 也互斥。
 *
 * 本类只负责维护互斥关系，不负责冲突强度比较、
 * 裁决结果计算或游戏效果执行。
 */
public final class GhostInfluenceConflictRegistry {

    private static final Set<TypePair> MUTUAL_EXCLUSIONS =
            new HashSet<>();

    static {
        register(
                GhostInfluenceType.PROTECTION,
                GhostInfluenceType.ATTACK
        );
    }

    private GhostInfluenceConflictRegistry() {
    }

    /**
     * 注册两种影响类型之间的互斥关系。
     *
     * 注册关系自动视为双向关系。
     *
     * @param first 第一种影响类型
     * @param second 第二种影响类型
     */
    public static void register(
            GhostInfluenceType first,
            GhostInfluenceType second
    ) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");

        if (first == second) {
            throw new IllegalArgumentException(
                    "An influence type cannot conflict with itself."
            );
        }

        MUTUAL_EXCLUSIONS.add(new TypePair(first, second));
    }

    /**
     * 判断两种影响类型是否互斥。
     *
     * 未注册的关系默认不互斥。
     *
     * @param first 第一种影响类型
     * @param second 第二种影响类型
     * @return 两种类型是否互斥
     */
    public static boolean areMutuallyExclusive(
            GhostInfluenceType first,
            GhostInfluenceType second
    ) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");

        if (first == second) {
            return false;
        }

        return MUTUAL_EXCLUSIONS.contains(
                new TypePair(first, second)
        );
    }

    /**
     * 无序的影响类型组合。
     *
     * 规范化类型顺序，确保 A-B 与 B-A 是同一关系。
     */
    private record TypePair(
            GhostInfluenceType first,
            GhostInfluenceType second
    ) {
        private TypePair {
            Objects.requireNonNull(first, "first");
            Objects.requireNonNull(second, "second");

            if (first.ordinal() > second.ordinal()) {
                GhostInfluenceType temporary = first;
                first = second;
                second = temporary;
            }
        }
    }
}