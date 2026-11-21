package com.qidate.qisplan2.core;

import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.entity.TestGeckoEntity;
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

        // 独特外观沿用通用宿主属性，强度与规则仍由模块决定。
        event.put(ModEntities.CALLING_GHOST.get(),ModularGhostEntity.createAttributes().build());

        // GeckoLib 测试实体
        event.put(
                ModEntities.TEST_GECKO.get(),
                TestGeckoEntity.createAttributes()
                        .build()
        );
    }
}