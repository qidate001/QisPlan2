package com.qidate.qisplan2.core;

import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.entity.CallingGhostEntity;
import com.qidate.qisplan2.entity.KnockingGhostEntity;
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

        // 独特实体分别声明自身基础属性；静态工厂不会多态分派，必须调用对应实体的方法。
        event.put(ModEntities.CALLING_GHOST.get(),CallingGhostEntity.createAttributes().build());
        event.put(ModEntities.KNOCKING_GHOST.get(),KnockingGhostEntity.createAttributes().build());

        // GeckoLib 测试实体
        event.put(
                ModEntities.TEST_GECKO.get(),
                TestGeckoEntity.createAttributes()
                        .build()
        );
    }
}