package com.qidate.qisplan2.ghost.module.host.player;

import com.qidate.qisplan2.ghost.possession.data.PossessedGhostData;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostState;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class PlayerGhostModuleContext {

    private final ServerPlayer player;

    private final ResourceLocation ghost;

    /**
     * 本次主动调用选中的目标；模块持久数据始终通过宿主实时读取。
     */

    private final LivingEntity target;


    public PlayerGhostModuleContext(
            ServerPlayer player,
            ResourceLocation ghost,
            PossessedGhostData data
    ) {

        this(
                player,
                ghost,
                data,
                null
        );
    }


    public PlayerGhostModuleContext(
            ServerPlayer player,
            ResourceLocation ghost,
            PossessedGhostData data,
            LivingEntity target
    ) {

        this.player = player;
        this.ghost = ghost;
        // 数据由玩家模块宿主持有，构造参数仅保留旧调用签名。
        this.target = target;
    }


    public PlayerGhostModuleHost host() { return new PlayerGhostModuleHost(player); }

    public ServerPlayer player() {

        return player;
    }


    public ResourceLocation ghost() {

        return ghost;
    }


    /**
     * 获取完整的鬼数据。
     */
    public PossessedGhostData data() {

        return PossessionHandler.getData(player, ghost);
    }


    /**
     * 获取当前通用状态。
     *
     * 这是对 data().state() 的快捷访问。
     */
    public PossessedGhostState state() {

        return data().state();
    }


    public LivingEntity target() {

        return target;
    }


    /**
     * 替换完整鬼数据。
     */
    public void setData(
            PossessedGhostData data
    ) {

        if (data == null) {
            return;
        }

        PossessionHandler.setData(player, ghost, data);
    }


    /**
     * 修改当前这只鬼的通用状态。
     *
     * 注意：
     * 不会丢失 PossessedGhostData 未来增加的其他字段。
     */
    public void setState(
            PossessedGhostState state
    ) {

        if (state == null) {
            return;
        }

        PossessionHandler.setState(player, ghost, state);
    }
}