package com.qidate.qisplan2.ghost.possession.ability;

import com.qidate.qisplan2.ghost.possession.data.PossessedGhostData;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class GhostAbilityContext {

    private final ServerPlayer player;

    private final ResourceLocation ghost;

    /**
     * 当前这只鬼的完整持久数据。
     */
    private PossessedGhostData data;

    private final LivingEntity target;


    public GhostAbilityContext(
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


    public GhostAbilityContext(
            ServerPlayer player,
            ResourceLocation ghost,
            PossessedGhostData data,
            LivingEntity target
    ) {

        this.player = player;
        this.ghost = ghost;
        this.data = data;
        this.target = target;
    }


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

        return data;
    }


    /**
     * 获取当前通用状态。
     *
     * 这是对 data().state() 的快捷访问。
     */
    public PossessedGhostState state() {

        return data.state();
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

        this.data = data;
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

        this.data =
                this.data.withState(
                        state
                );
    }
}