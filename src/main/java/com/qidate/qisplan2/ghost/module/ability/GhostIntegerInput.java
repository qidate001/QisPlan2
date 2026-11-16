package com.qidate.qisplan2.ghost.module.ability;
/** 能力声明的整数输入范围；客户端提供输入，服务端仍独立校验。 */
public record GhostIntegerInput(int minimum, int maximum, int initial) {
    /** 判断外部输入是否位于声明范围。 */
    public boolean accepts(int value) { return value >= minimum && value <= maximum; }
}
