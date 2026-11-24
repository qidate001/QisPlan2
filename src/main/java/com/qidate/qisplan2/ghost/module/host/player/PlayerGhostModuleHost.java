package com.qidate.qisplan2.ghost.module.host.player;

import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import com.qidate.qisplan2.ghost.module.GhostModuleRuntime;
import com.qidate.qisplan2.ghost.module.event.ModuleTickEvent;
import com.qidate.qisplan2.ghost.module.host.ModuleGhostDomains;
import com.qidate.qisplan2.ghost.possession.manager.GhostSuppressionAllocationHandler;
import com.qidate.qisplan2.event.GhostBreakoutHandler;
import com.qidate.qisplan2.death.QisDeathHandler;
import net.minecraft.server.level.ServerPlayer;
import com.qidate.qisplan2.ghost.module.GhostModuleCombatState;
import com.qidate.qisplan2.ghost.module.host.GhostSoundSuppression;
import com.qidate.qisplan2.ghost.module.host.attachment.GhostAttachments;
import com.qidate.qisplan2.ghost.module.suppression.GhostSuppressionState;
import com.qidate.qisplan2.ghost.bearing.HostComposition;

import com.qidate.qisplan2.ghost.module.GhostModuleCustomDataModifier;
import com.qidate.qisplan2.ghost.module.GhostModuleDataModifier;
import com.qidate.qisplan2.ghost.module.data.GhostModuleContainer;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Player 的模块宿主适配器。
 *
 * 通过 PlayerGhostData 读写玩家上的模块实例数据。
 * 仅在当前操作期间使用，不长期保存模块容器。
 *
 * 修改模块数据时，必须保留其他通用属性与专属数据。
 */
public final class PlayerGhostModuleHost
        implements GhostModuleDataModifier, GhostModuleCustomDataModifier {
    private final Player player;

    /** 玩家输入只是请求，宿主负责接受完整模块实例及其承载校验。 */
    public boolean possess(GhostModuleData.Entry entry) {
        return player.isAlive() && GhostModuleRegistry.get(entry.id())!=null && importModule(entry);
    }

    /** 从模块声明创建新实例，命令与调试入口也使用同一个安装路径。 */
    public boolean possess(ResourceLocation id) {
        var module=GhostModuleRegistry.get(id);if(module==null)return false;
        var initial=module.getInitialData();return possess(new GhostModuleData.Entry(id,initial.intensity(),initial.revival()));
    }

    /** 释放承载关系；只关闭此宿主上的运行时资源，不触碰其他宿主的同名模块。 */
    public boolean release(ResourceLocation id) {
        if(!hasModule(id))return false;
        ModuleGhostDomains.closeRuntime(this,id);
        if(!removeModule(id))return false;
        if(player instanceof ServerPlayer server)GhostSuppressionAllocationHandler.removeByGhost(server,id);
        return true;
    }

    /** 宿主推进自己的输入与生命周期，然后进入所有载体共用的模块运行时。 */
    public void tick() {
        if(!(player instanceof ServerPlayer server))return;
        GhostModuleRuntime.dispatch(this,new ModuleTickEvent());
        PlayerModuleEvents.tickTouch(server,this);
    }

    /** 复苏判断由运行时统一完成；此适配器只落实玩家身体的破体与死亡。 */
    @Override public void onControlLost(ResourceLocation module) {
        if (player instanceof ServerPlayer server && server.isAlive()) {
            GhostBreakoutHandler.breakout(server);
            QisDeathHandler.forceKillAndCleanup(server);
        }
    }

    /** 已驾驭模块由主动输入决定持续表现，不自动开启音乐。 */
    @Override public boolean allowsAutomaticAbilities() { return false; }

    /** 驭鬼模块才属于体内来源，插入的物品不会加入本适配器的数据。 */
    @Override public boolean controlsIntrinsicModules() { return true; }

    /** 只读附着关系计算出的状态，不从背包或世界扫描压制来源。 */
    @Override public GhostSuppressionState suppression(ResourceLocation module) {
        return GhostAttachments.suppression(player, module).merge(
                GhostSoundSuppression.state(player, module)).merge(GhostModuleCombatState.attackSuppression(this, module));
    }

    /** 玩家固定采用二十一个人形承载部位。 */
    @Override public HostComposition composition() { return HostComposition.HUMANOID; }

    public PlayerGhostModuleHost(Player player) {
        this.player = Objects.requireNonNull(player, "player");
    }

    public Player player() { return player; }

    /** 完整实例导入，保留强度、复苏、沉睡、死机和 customData；重复模块拒绝覆盖。 */
    public boolean importModule(GhostModuleData.Entry entry) {
        if (hasModule(entry.id()) || !canInstallModule(entry.id())) return false;
        var entries = new ArrayList<>(getModules());
        entries.add(entry);
        PlayerGhostData.setModules(player, entries);
        return true;
    }

    @Override public boolean equals(Object other) {
        return other instanceof PlayerGhostModuleHost host && host.player == player;
    }
    @Override public int hashCode() { return System.identityHashCode(player); }

    /**
     * 获取玩家当前保存的模块实例数据。
     */
    @Override
    public List<GhostModuleData.Entry> getModules() {
        return PlayerGhostData.getModules(player);
    }

    /**
     * 添加模块或更新已有模块的强度。
     */
    @Override
    public boolean addModule(
            ResourceLocation moduleId,
            double intensity
    ) {
        Objects.requireNonNull(moduleId, "moduleId");

        GhostModuleContainer container = createContainer();

        if (!container.addModule(moduleId, intensity)) {
            return false;
        }

        saveContainer(container);
        return true;
    }

    /**
     * 移除指定模块及其全部实例数据。
     */
    @Override
    public boolean removeModule(ResourceLocation moduleId) {
        Objects.requireNonNull(moduleId, "moduleId");

        GhostModuleContainer container = createContainer();

        if (!container.removeModule(moduleId)) {
            return false;
        }

        saveContainer(container);
        return true;
    }

    /**
     * 修改模块强度，保留其他数据。
     */
    @Override
    public boolean setIntensity(
            ResourceLocation moduleId,
            double intensity
    ) {
        Objects.requireNonNull(moduleId, "moduleId");

        GhostModuleContainer container = createContainer();

        if (!container.setIntensity(moduleId, intensity)) {
            return false;
        }

        saveContainer(container);
        return true;
    }

    /**
     * 修改模块复苏值，保留其他数据。
     */
    @Override
    public boolean setRevival(
            ResourceLocation moduleId,
            double revival
    ) {
        Objects.requireNonNull(moduleId, "moduleId");

        GhostModuleContainer container = createContainer();

        if (!container.setRevival(moduleId, revival)) {
            return false;
        }

        saveContainer(container);
        return true;
    }

    /**
     * 修改模块沉睡状态，保留其他属性。
     */
    @Override
    public boolean setSleeping(
            ResourceLocation moduleId,
            boolean sleeping
    ) {
        Objects.requireNonNull(moduleId, "moduleId");

        GhostModuleContainer container = createContainer();

        if (!container.setSleeping(moduleId, sleeping)) {
            return false;
        }

        saveContainer(container);
        return true;
    }

    /**
     * 修改模块死机状态，保留其他属性。
     */
    @Override
    public boolean setDeadlocked(
            ResourceLocation moduleId,
            boolean deadlocked
    ) {
        Objects.requireNonNull(moduleId, "moduleId");

        GhostModuleContainer container = createContainer();

        if (!container.setDeadlocked(moduleId, deadlocked)) {
            return false;
        }

        saveContainer(container);
        return true;
    }

    /**
     * 获取指定模块的专属数据副本。
     *
     * 模块不存在时返回空容器，不创建模块。
     */
    @Override
    public CompoundTag getModuleCustomData(ResourceLocation moduleId) {
        Objects.requireNonNull(moduleId, "moduleId");

        GhostModuleContainer container = createContainer();

        return container.getCustomData(moduleId);
    }

    /**
     * 替换指定模块的专属数据。
     *
     * 保留模块的强度、复苏值、沉睡和死机状态。
     */
    @Override
    public boolean setModuleCustomData(
            ResourceLocation moduleId,
            CompoundTag customData
    ) {
        Objects.requireNonNull(moduleId, "moduleId");
        Objects.requireNonNull(customData, "customData");

        GhostModuleContainer container = createContainer();

        if (!container.setCustomData(moduleId, customData)) {
            return false;
        }

        saveContainer(container);
        return true;
    }

    /**
     * 从 Player 当前的数据组件创建临时容器。
     */
    private GhostModuleContainer createContainer() {
        GhostModuleContainer container = new GhostModuleContainer(this::composition);
        container.loadEntries(PlayerGhostData.getModules(player));
        return container;
    }

    /**
     * 将容器中的全部模块数据一次性写回 Player。
     */
    private void saveContainer(GhostModuleContainer container) {
        PlayerGhostData.setModules(player, container.getModules());
    }
}
