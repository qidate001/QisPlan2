package com.qidate.qisplan2.core;

import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.entity.TestGeckoEntity;
import com.qidate.qisplan2.entity.ghostmist.GhostMist;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

public final class ModEntityAttributes {

    private ModEntityAttributes() {}

    public static void register(
            EntityAttributeCreationEvent event
    ) {
        // 通用模块化厉鬼
        event.put(
                ModEntities.MODULAR_GHOST.get(),
                ModularGhostEntity.createAttributes()
                        .build()
        );

        // 鬼雾
        event.put(
                ModEntities.GHOST_MIST.get(),
                GhostMist.createAttributes()
                        .build()
        );

        // GeckoLib 测试实体
        event.put(
                ModEntities.TEST_GECKO.get(),
                TestGeckoEntity.createAttributes()
                        .build()
        );
    }
}