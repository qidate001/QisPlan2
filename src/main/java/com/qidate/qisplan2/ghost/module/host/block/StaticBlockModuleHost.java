package com.qidate.qisplan2.ghost.module.host.block;

import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

/**
 * 静态方块厉鬼宿主。
 *
 * 模块列表由方块的固定定义提供，
 * 不支持运行时修改，也不需要 BlockEntity。
 */
public final class StaticBlockModuleHost implements GhostModuleHost {

    private final List<GhostModuleData.Entry> modules;

    public StaticBlockModuleHost(
            ResourceLocation moduleId,
            double intensity
    ) {
        Objects.requireNonNull(moduleId, "moduleId");

        if (!Double.isFinite(intensity) || intensity < 0.0D) {
            throw new IllegalArgumentException(
                    "Module intensity must be finite and non-negative"
            );
        }

        this.modules = List.of(
                new GhostModuleData.Entry(moduleId, intensity)
        );
    }

    @Override
    public List<GhostModuleData.Entry> getModules() {
        return modules;
    }
}