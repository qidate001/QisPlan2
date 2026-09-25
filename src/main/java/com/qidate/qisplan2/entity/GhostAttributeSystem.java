package com.qidate.qisplan2.entity;

/**
 * 管理实体鬼共有的灵异属性。
 *
 * <p>
 * 所有实体鬼都拥有灵异强度和灵异防御。
 * 具体厉鬼可以通过自身的成长机制修改这些属性。
 */
public final class GhostAttributeSystem {

    private GhostAttributeSystem() {
    }

    /**
     * 获取厉鬼的灵异强度。
     *
     * @param ghost 厉鬼
     */
    public static double getSupernaturalStrength(
            AbstractGhostEntity ghost
    ) {
        return ghost.getSupernaturalStrengthValue();
    }

    /**
     * 设置厉鬼的灵异强度。
     *
     * @param ghost 厉鬼
     * @param value 灵异强度
     */
    public static void setSupernaturalStrength(
            AbstractGhostEntity ghost,
            double value
    ) {
        ghost.setSupernaturalStrengthValue(value);
    }

    /**
     * 增加厉鬼的灵异强度。
     *
     * @param ghost 厉鬼
     * @param value 增加的强度
     */
    public static void addSupernaturalStrength(
            AbstractGhostEntity ghost,
            double value
    ) {
        ghost.setSupernaturalStrengthValue(
                ghost.getSupernaturalStrengthValue() + value
        );
    }

    /**
     * 获取厉鬼的灵异防御。
     *
     * @param ghost 厉鬼
     */
    public static double getSupernaturalDefense(
            AbstractGhostEntity ghost
    ) {
        return ghost.getSupernaturalDefenseValue();
    }

    /**
     * 设置厉鬼的灵异防御。
     *
     * @param ghost 厉鬼
     * @param value 灵异防御
     */
    public static void setSupernaturalDefense(
            AbstractGhostEntity ghost,
            double value
    ) {
        ghost.setSupernaturalDefenseValue(value);
    }

    /**
     * 增加厉鬼的灵异防御。
     *
     * @param ghost 厉鬼
     * @param value 增加的防御
     */
    public static void addSupernaturalDefense(
            AbstractGhostEntity ghost,
            double value
    ) {
        ghost.setSupernaturalDefenseValue(
                ghost.getSupernaturalDefenseValue() + value
        );
    }
}