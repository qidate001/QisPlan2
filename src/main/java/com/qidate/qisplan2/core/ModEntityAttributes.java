package com.qidate.qisplan2.core;

import com.qidate.qisplan2.entity.calling.CallingGhost;
import com.qidate.qisplan2.entity.ghostdoor.closing.ClosingGhost;
import com.qidate.qisplan2.entity.ghostdoor.opening.OpeningGhost;
import com.qidate.qisplan2.entity.invisible.InvisibleGhost;
import com.qidate.qisplan2.entity.ghostdoor.knocking.KnockingGhost;
import com.qidate.qisplan2.entity.nightwanderer.NightWanderer;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

public final class ModEntityAttributes {

    private ModEntityAttributes() {}

    public static void register(
            EntityAttributeCreationEvent event
    ) {
        // 夜游鬼
        event.put(
                ModEntities.NIGHT_WANDERER.get(),
                NightWanderer.createAttributes()
                        .build()
        );

        // 不可视之鬼
        event.put(
                ModEntities.INVISIBLE_GHOST.get(),
                InvisibleGhost.createAttributes()
                        .build()
        );

        // 敲门鬼
        event.put(
                ModEntities.KNOCKING_GHOST.get(),
                KnockingGhost.createAttributes()
                        .build()
        );

        // 开门鬼
        event.put(
                ModEntities.OPENING_GHOST.get(),
                OpeningGhost.createAttributes()
                        .build()
        );

        // 关门鬼
        event.put(
                ModEntities.CLOSING_GHOST.get(),
                ClosingGhost.createAttributes()
                        .build()
        );

        // 喊人鬼
        event.put(
                ModEntities.CALLING_GHOST.get(),
                CallingGhost.createAttributes()
                        .build()
        );
    }
}