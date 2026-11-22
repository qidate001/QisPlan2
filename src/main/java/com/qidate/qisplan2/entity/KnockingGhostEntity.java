package com.qidate.qisplan2.entity;

import com.qidate.qisplan2.ghost.bearing.HostComposition;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.implementation.knocking.KnockingGhostModule;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import java.util.List;

/** 敲门鬼的独特外观宿主。除初始模块外没有专属规则、能力或 AI。 */
public final class KnockingGhostEntity extends AnimatedModularGhostEntity {
    /** 新实体自带敲门鬼模块；之后可自由增删，读档完全沿用父类的模块数据。 */
    public KnockingGhostEntity(EntityType<? extends KnockingGhostEntity> type,Level level) {
        super(type, level, HostComposition.HUMANOID,
                List.of(new GhostModuleData.Entry(
                        KnockingGhostModule.ID,
                        KnockingGhostModule.INITIAL_INTENSITY,
                        0
                ))
        );
    }
}
