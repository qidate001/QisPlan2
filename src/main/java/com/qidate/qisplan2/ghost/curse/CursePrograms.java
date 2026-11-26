package com.qidate.qisplan2.ghost.curse;

import com.qidate.qisplan2.ghost.module.component.GhostComponentProgram;
import com.qidate.qisplan2.ghost.module.event.ModuleTickEvent;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** 可选的周期持续能力预设。真正的规则、媒介和能力仍由所属模块挑选。 */
public final class CursePrograms {
    private CursePrograms() {}
    private static final ResourceLocation TICK_ID=ResourceLocation.fromNamespaceAndPath("qisplan2","curse/continue");
    public static final GhostComponentProgram<CurseAbilityContext> TICK=new GhostComponentProgram<>(
        List.of(new GhostComponentProgram.Ability<>(TICK_ID,c->c.advance()!=null,c->c.advance().run())),
        List.of(new GhostComponentProgram.Rule<>(ModuleTickEvent.class,c->true,TICK_ID)));
}
