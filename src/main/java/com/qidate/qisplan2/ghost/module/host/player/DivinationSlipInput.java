package com.qidate.qisplan2.ghost.module.host.player;
import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.corrosion.CorrosionType;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityExecutor;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluence;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceSession;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.module.event.DivinationSlipResultEvent;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceCollector;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostState;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import static com.qidate.qisplan2.QisPlan2.MODID;
import com.qidate.qisplan2.ghost.module.implementation.divinationslip.GhostDivinationSlipModule;

/** 网络签文结果转换为主动能力输入，不复制规则或效果。 */
public final class DivinationSlipInput {
    /**
     * 禁止外部实例化此工具类；功能统一通过静态入口调用。
     */
    private DivinationSlipInput() {}
    private static final ResourceLocation ID = GhostDivinationSlipModule.ID;
    private static final ResourceLocation USE_ABILITY_ID = GhostDivinationSlipModule.USE_ABILITY_ID;
    private static final int LIFE_RESULT = 0, GHOST_RESULT = 2;
    private static final double REVIVAL_GAIN = 20;

    /**
     * 将服务端确认的签文结果转换为鬼签主动能力调用。
     * 准备能力后结算复苏；生死签提交裁决，鬼签结果直接执行已准备效果。
     */
    public static void useResult(
            ServerPlayer player,
            int result
    ) {
        var host = new PlayerGhostModuleHost(player);
        if (result < 0 || result > 2 || !player.isAlive() || !host.hasModule(ID)) return;
        // 签文结果是事实；直接准备 POSSESSION 能力，不经过规则运行时。
        var context = new GhostAbilityContext(host, ID, USE_ABILITY_ID,
                GhostAbilityContext.Source.POSSESSION, new DivinationSlipResultEvent(player, result));
        Runnable effect = GhostAbilityExecutor.prepareDeferred(context);
        if (effect == null) return;
        addRevival(player);
        if (result == GHOST_RESULT) {
            effect.run();
            return;
        }
        GhostInfluenceSession session = new GhostInfluenceSession();
        GhostInfluenceCollector.collectCurses(session, host, ID, USE_ABILITY_ID,
                player.getUUID(), player.serverLevel().getServer());
        session.submit(new GhostInfluence(host, ID, USE_ABILITY_ID, player.getUUID(),
                result == LIFE_RESULT ? GhostInfluenceType.PROTECTION : GhostInfluenceType.ATTACK,
                context.getModuleContext().intensity(), session.getSourceId(host, ID), session.getBatchId()), effect);
        session.resolveAndApply();
    }

    /**
     * 结算本次操作引起的复苏变化，并通过现有状态入口写回模块数据。
     */
    public static void addRevival(
            ServerPlayer player
    ) {

        PossessedGhostState state =
                PossessionHandler.getState(
                        player,
                        ID
                );

        if (state == null) {
            return;
        }

        PossessedGhostState newState =
                PossessionHandler.addRevival(
                        player,
                        ID,
                        state,
                        REVIVAL_GAIN,
                        CorrosionType.GLOBAL
                );

        PossessionHandler.setState(
                player,
                ID,
                newState
        );
    }

}
