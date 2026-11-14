package com.qidate.qisplan2.client.screen;

/** 与渲染无关的轮盘几何规则，中心和圆环外均为取消区。 */
public final class AbilityWheelLayout {
    /** 工具类不需要实例化。 */
    private AbilityWheelLayout() {}
    /** 第一个扇区居中朝上，后续顺时针排列。 */
    public static double start(int index, int count) { return -Math.PI / 2 + (index - 0.5) * Math.PI * 2 / count; }
    /** 仅命中可见圆环；槽位数量为零时始终不选中。 */
    public static int index(double x, double y, double inner, double outer, int count) {
        double radius = Math.hypot(x, y);
        if (count <= 0 || radius < inner || radius > outer) return -1;
        double angle = (Math.atan2(y, x) - start(0, count)) % (Math.PI * 2);
        if (angle < 0) angle += Math.PI * 2;
        return Math.min(count - 1, (int)(angle / (Math.PI * 2 / count)));
    }
}
