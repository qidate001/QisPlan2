package com.qidate.qisplan2.ghost.module;

/**
 * 单次模块执行的上下文。
 */
public record GhostModuleContext(
        GhostModuleHost host,
        double intensity
) {
}