package com.qidate.qisplan2.ghost.module;

import java.util.List;

/**
 * 可以承载厉鬼模块的宿主。
 */
public interface GhostModuleHost {

    List<GhostModuleData.Entry> getModules();
}