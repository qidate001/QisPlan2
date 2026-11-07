package com.qidate.qisplan2.ghost.module.event;

/** 当前世界因玩家睡眠跳过了一段时间 */
public record SleepTimeSkippedEvent(long skippedTicks) implements GhostEvent {}
