
package com.qidate.qisplan2.block;

import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.List;
import java.util.Objects;

/**
 * 普通灵异方块的基础类。
 *
 * 方块自身声明其承载的鬼模块及对应强度。
 */
public class GhostStoneBlock extends Block implements GhostModuleHost {

    private final List<GhostModuleData.Entry> modules;

    public GhostStoneBlock(
            BlockBehaviour.Properties properties,
            ResourceLocation moduleId,
            double intensity
    ) {
        super(properties);

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
