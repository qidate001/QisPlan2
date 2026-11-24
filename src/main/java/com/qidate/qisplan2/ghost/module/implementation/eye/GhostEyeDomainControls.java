package com.qidate.qisplan2.ghost.module.implementation.eye;

import com.qidate.qisplan2.ghost.module.GhostModuleContext;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.domain.*;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 鬼眼专属控制声明；只接触模块数据和世界接口，不引用客户端或宿主实现。 */
public final class GhostEyeDomainControls {
    public static final ResourceLocation TOGGLE_EYE=ResourceLocation.parse("qisplan2:ghost_eye/toggle_eye");
    private GhostEyeDomainControls() {}
    /** 十只眼睛按模块布局排列，眼睛开闭状态由模块自己的数据解释。 */
    public static GhostDomainPanel panel() {
        List<GhostDomainPanel.Control> controls = new ArrayList<>();
        double[][] positions = {
                {.5, .13}, {.28, .29},
                {.72,.29}, {.13, .49},
                {.5, .47}, {.87, .49},
                {.27, .69}, {.73, .69},
                {.41,.86}, {.59, .86}
        };

        for (int i = 0; i < 10; i++) {
            final int eye = i;
            controls.add(new GhostDomainPanel.Control("screen.qisplan2.domain.eye_" + (i + 1),
                positions[i][0], positions[i][1], GhostDomainPanel.Style.EYE, TOGGLE_EYE, i,
                c -> (mask(c) & (1 << eye)) != 0)
            );
        }

        return new GhostDomainPanel(controls,List.of(ResourceLocation.parse("qisplan2:ghost_eye/teleport"),ResourceLocation.parse("qisplan2:ghost_eye/reboot")),
                GhostDomainPresets.layers(GhostEyeModule.ID,10),Map.of("toggle",GhostEyeModule.TOGGLE_DOMAIN,
                        "domain_up",ResourceLocation.parse("qisplan2:ghost_eye/domain_up"),
                        "domain_down",ResourceLocation.parse("qisplan2:ghost_eye/domain_down"),
                        "self_up",ResourceLocation.parse("qisplan2:ghost_eye/self_up"),
                        "self_down",ResourceLocation.parse("qisplan2:ghost_eye/self_down")));
    }

    /** 普通层数能力也能修改鬼域；保存的眼睛数量不一致时按实际层数重建表现。 */
    public static int mask(GhostModuleContext c) {
        var data = c.getCustomData();
        boolean open = data.getBoolean("domainOpen");
        int count = open ? Math.clamp(data.getInt("domainLayer"), 1, 10) : 0;
        int saved = data.getInt("eyeMask") & 1023;
        return Integer.bitCount(saved) == count ? saved : (1 << count) - 1;
    }

    /** 每次点击切换一只眼睛，层数等于睁眼数量；全部闭眼时关闭鬼域。 */
    public static GhostAbility ability() {return new GhostAbility(){
        /** 返回本模块声明的点眼能力身份。 */
        public ResourceLocation getId() {return TOGGLE_EYE;}
        /** 输入是十只眼睛的零基索引，不是要设置的总层数。 */
        public GhostIntegerInput integerInput() {return new GhostIntegerInput(0,9,0);}
        /** 独立验证索引和鬼域出口，网络与其他调用渠道遵守相同条件。 */
        public boolean canExecute(GhostAbilityContext c) {return c.parameters() instanceof GhostInvocationParameters p && p.number()!=null && integerInput().accepts(p.number()) && c.getModuleContext().world().canToggleDomain();}
        /** 无结果调用沿用同一条效果实现。 */
        public void execute(GhostAbilityContext c) {executeWithResult(c);}
        /** 先更新实际鬼域，再保存眼睛位图；任一步失败都不伪造成功状态。 */
        public boolean executeWithResult(GhostAbilityContext c){
            var context=c.getModuleContext(); int bits=mask(context)^(1<<((GhostInvocationParameters)c.parameters()).number());
            int count=Integer.bitCount(bits); var domain=context.world().domain();
            if (count > 0 && !domain.setLayer(count)) return false;
            if (!domain.setOpen(count > 0)) return false;
            var data=context.getCustomData();
            data.putInt("eyeMask",bits);context.setCustomData(data);
            return true;
        }
    };}
}
