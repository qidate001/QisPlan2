package com.qidate.qisplan2.ghost.module.domain;

/**
 * 鬼域的独立表现声明。实现可以携带自己的参数，不向通用鬼域定义添加专用开关。
 * 这里只允许服务端安全的数据；客户端通过对应适配器实现表现，不在模块中引用渲染类。
 * 影响世界的规则和能力仍由 GhostModule 声明，不能在客户端适配器中实施伤害。
 */
public interface GhostDomainEffect {}
