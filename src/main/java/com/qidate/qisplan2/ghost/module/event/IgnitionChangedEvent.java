package com.qidate.qisplan2.ghost.module.event;

/** 点燃状态已经发生变化的语义事实 */
public record IgnitionChangedEvent(boolean lit) implements GhostEvent {}
