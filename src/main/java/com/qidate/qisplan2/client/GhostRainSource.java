package com.qidate.qisplan2.client;

/**
 * 一个鬼雨领域的渲染源。
 *
 * 只保存世界坐标和领域半径。
 */
public record GhostRainSource(
        double x,
        double z,
        double radius
) {
}