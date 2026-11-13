package com.qidate.qisplan2.entity;

import com.qidate.qisplan2.ghost.bearing.HostComposition;
import com.qidate.qisplan2.ghost.module.host.entity.GhostEntityBindings;

import com.qidate.qisplan2.death.SupernaturalCombatHandler;
import com.qidate.qisplan2.entity.ai.GhostCandleAttractionGoal;
import com.qidate.qisplan2.death.SupernaturalEntity;
import com.qidate.qisplan2.entity.ai.ModuleScheduledGoal;
import com.qidate.qisplan2.ghost.GhostAvoidanceSystem;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleContainer;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.event.ModuleTickEvent;
import com.qidate.qisplan2.ghost.module.event.TouchEvent;
import com.qidate.qisplan2.ghost.module.sound.GhostModuleSoundManager;
import com.qidate.qisplan2.ghost.possession.GhostPossessionInteractionSystem;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import static com.qidate.qisplan2.QisPlan2.MODID;

/**
 * 通用模块化厉鬼实体。
 *
 * 实体的具体身份和灵异规则由后续安装的实体模块决定。
 */
public class ModularGhostEntity extends PathfinderMob
        implements GhostModuleDataModifier, GhostModuleCustomDataModifier, SupernaturalEntity {

    private static final double BASE_SUPERNATURAL_DEFENSE = 4.0D;

    private static final String NBT_GHOST_MODULES = "QisPlan2GhostModules";
    private static final String NBT_MODULE_REVIVAL = "revival";

    private static final String NBT_MODULE_ID = "id";
    private static final String NBT_MODULE_INTENSITY = "intensity";
    private static final String NBT_MODULE_SLEEPING = "sleeping";
    private static final String NBT_MODULE_CUSTOM_DATA = "customData";
    private static final String NBT_MODULE_DEADLOCKED = "deadlocked";

    /**
     * 为模块化厉鬼刷怪蛋创建实体数据。
     *
     * 刷怪蛋会通过 ENTITY_DATA 将此数据交给新实体；
     * 字段格式与实体存档一致，因此新宿主读取数据时
     * 可以直接还原完整的模块实例状态。
     */
    public static CompoundTag createSpawnData(
            List<GhostModuleData.Entry> modules
    ) {
        ListTag modulesTag = new ListTag();

        for (GhostModuleData.Entry entry : modules) {
            CompoundTag moduleTag = new CompoundTag();
            moduleTag.putString(NBT_MODULE_ID, entry.id().toString());
            moduleTag.putDouble(NBT_MODULE_INTENSITY, entry.intensity());
            moduleTag.putDouble(NBT_MODULE_REVIVAL, entry.revival());
            moduleTag.putBoolean(NBT_MODULE_SLEEPING, entry.sleeping());
            moduleTag.putBoolean(NBT_MODULE_DEADLOCKED, entry.deadlocked());
            moduleTag.put(NBT_MODULE_CUSTOM_DATA, entry.customData().copy());
            modulesTag.add(moduleTag);
        }

        CompoundTag entityData = new CompoundTag();

        // ENTITY_DATA 在刷怪蛋上采用完整实体存档格式，必须包含宿主实体类型 ID；
        // 缺少该字段时，客户端读取刷怪蛋组件会因无法确定实体类型而崩溃。
        entityData.putString("id", MODID + ":modular_ghost");
        entityData.put(NBT_GHOST_MODULES, modulesTag);
        return entityData;
    }

    private final HostComposition composition;
    private final GhostModuleContainer ghostModules;

    private final Set<UUID> touchingEntities = new HashSet<>();

    /**
     * 创建通用厉鬼实体，并交给原版实体构造流程初始化。
     * 具体厉鬼身份、持久状态与行为由后续安装的模块提供。
     */
    public ModularGhostEntity(
            EntityType<? extends ModularGhostEntity> entityType,
            Level level
    ) {
        this(entityType, level, HostComposition.HUMANOID, List.of());
    }

    /**
     * 特殊模型实体通过此构造器声明自己的组成和默认模块，复用全部存档、行为与能力框架。
     * 默认模块只用于新实体初始化；读档中的空列表表示模块已移除，不能重新补回。
     */
    protected ModularGhostEntity(EntityType<? extends ModularGhostEntity> entityType, Level level,
            HostComposition composition, List<GhostModuleData.Entry> defaultModules) {
        super(entityType, level);
        this.composition = Objects.requireNonNull(composition, "composition");
        this.ghostModules = new GhostModuleContainer(() -> this.composition);
        if (!ghostModules.replaceEntries(defaultModules))
            throw new IllegalArgumentException("Default modules exceed entity bearing capacity");
    }

    /** 返回该实体类型在构造时声明的组成，基础实体默认采用二十一个人形部位。 */
    @Override public HostComposition composition() { return composition; }

    /**
     * 获取实体当前安装的模块。
     */
    @Override
    public List<GhostModuleData.Entry> getModules() {
        return ghostModules.getModules();
    }

    /**
     * 判断实体是否安装指定模块。
     */
    public boolean hasGhostModule(ResourceLocation moduleId) {
        Objects.requireNonNull(moduleId, "moduleId");
        return ghostModules.hasModule(moduleId);
    }

    /**
     * 安装模块。
     *
     * 已存在相同 ID 的模块时，更新强度并保留其他属性。
     */
    public void addGhostModule(
            ResourceLocation moduleId,
            double intensity
    ) {
        ghostModules.addModule(moduleId, intensity);
        refreshModuleBindings();
        syncModuleSounds();
    }

    /**
     * 跨宿主迁移时安装完整实例。
     * 先恢复全部专属数据，再创建行为，避免行为初始化读取到默认数据。
     * 同一宿主不允许覆盖已有的同名模块。
     */
    public boolean importGhostModule(GhostModuleData.Entry entry) {
        if (hasGhostModule(entry.id()) || !canInstallModule(entry.id())) return false;
        List<GhostModuleData.Entry> entries = new ArrayList<>(getModules());
        entries.add(entry);
        if (!ghostModules.replaceEntries(entries)) return false;
        refreshModuleBindings();
        syncModuleSounds();
        return true;
    }

    /**
     * 移除模块。
     */
    public void removeGhostModule(ResourceLocation moduleId) {
        ghostModules.removeModule(moduleId);
        refreshModuleBindings();
        syncModuleSounds();
    }

    /**
     * 当前宿主已挂载的模块行为实例。
     * 按模块 ID 管理运行时资源；模块移除时停止行为并释放对应外部资源。
     */
    private final Map<ResourceLocation, List<Goal>> moduleGoals =
            new HashMap<>();

    /** 同步模块提供的行为；重新读取存档和重复安装不会重复挂载同一组行为。 */
    private void refreshModuleGoals() {
        var iterator = moduleGoals.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (!carriesModule(entry.getKey())) {
                GhostModule removedModule = GhostModuleRegistry.get(entry.getKey());
                if (removedModule != null) GhostEntityBindings.detach(removedModule, this);
                entry.getValue().forEach(goalSelector::removeGoal);
                iterator.remove();
            }
        }
        for (var entry : getModules()) {
            if (!carriesModule(entry.id())) continue;
            GhostModule module = GhostModuleRegistry.get(entry.id());
            if (module == null || moduleGoals.containsKey(entry.id())) continue;
            var definitions = GhostEntityBindings.goals(module, this);
            List<Goal> goals = new ArrayList<>();
            for (int i = 0; i < definitions.size(); i++) {
                var scheduled = new ModuleScheduledGoal(
                        this, entry.id(), i, definitions.get(i),
                        () -> moduleGoals.values().stream().flatMap(List::stream)
                                .map(goal -> (ModuleScheduledGoal)goal).toList());
                goals.add(scheduled);
                // 跨模块优先级交给包装器按实时强度判断，避免原版固定数字优先级阻止强者抢占。
                goalSelector.addGoal(1, scheduled);
            }
            moduleGoals.put(entry.id(), goals);
        }
    }

    /**
     * 刷新当前模块对应的实体行为绑定，保持安装列表与运行时行为一致。
     */
    private void refreshModuleBindings() {
        refreshModuleGoals();
    }

    /**
     * 获取指定模块的完整实例数据。
     *
     * 不存在时返回 null。
     */
    private GhostModuleData.Entry getModuleEntry(
            ResourceLocation moduleId
    ) {
        return ghostModules.getModule(moduleId);
    }

    /**
     * 同步实体模块声明的声音。
     */
    private void syncModuleSounds() {
        if (!level().isClientSide) {
            GhostModuleSoundManager.syncEntityHost(this);
        }
    }

    /**
     * 获取指定模块实例的专属数据副本。
     */
    @Override
    public CompoundTag getModuleCustomData(ResourceLocation moduleId) {
        return ghostModules.getCustomData(moduleId);
    }

    /**
     * 替换指定模块实例的专属数据。
     *
     * 保留该模块的全部通用属性。
     */
    @Override
    public boolean setModuleCustomData(
            ResourceLocation moduleId,
            CompoundTag customData
    ) {
        return ghostModules.setCustomData(moduleId, customData);
    }

    /**
     * 修改模块复苏值，保留其他属性。
     */
    @Override
    public boolean setRevival(
            ResourceLocation moduleId,
            double revival
    ) {
        return ghostModules.setRevival(moduleId, revival);
    }

    /**
     * 修改模块沉睡状态，保留其他属性。
     */
    @Override
    public boolean setSleeping(
            ResourceLocation moduleId,
            boolean sleeping
    ) {
        return ghostModules.setSleeping(moduleId, sleeping);
    }

    /**
     * 修改模块死机状态，保留其他属性。
     */
    @Override
    public boolean setDeadlocked(
            ResourceLocation moduleId,
            boolean deadlocked
    ) {
        return ghostModules.setDeadlocked(moduleId, deadlocked);
    }

    /**
     * 校验灵异属性数值。
     *
     * 当前阶段统一要求数值有限且非负。
     */
    private static boolean isValidValue(double value) {
        return Double.isFinite(value) && value >= 0.0D;
    }

    /**
     * 安装模块或更新强度。
     */
    @Override
    public boolean addModule(
            ResourceLocation moduleId,
            double intensity
    ) {
        boolean changed = ghostModules.addModule(moduleId, intensity);

        if (changed) {
            refreshModuleBindings();
            syncModuleSounds();
        }

        return changed;
    }

    /**
     * 移除指定模块。
     */
    @Override
    public boolean removeModule(ResourceLocation moduleId) {
        boolean changed = ghostModules.removeModule(moduleId);

        if (changed) {
            refreshModuleBindings();
            syncModuleSounds();
        }

        return changed;
    }

    /**
     * 实体移除前注销声音并释放各模块的运行时资源，随后交给原版移除流程。
     */
    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            GhostModuleSoundManager.unregisterEntityHost(this);
            for (var entry : List.copyOf(getModules())) {
                GhostModule module = GhostModuleRegistry.get(entry.id());
                if (module != null) GhostEntityBindings.detach(module, this);
            }
        }

        super.remove(reason);
    }

    /**
     * 修改模块强度。
     */
    @Override
    public boolean setIntensity(
            ResourceLocation moduleId,
            double intensity
    ) {
        return ghostModules.setIntensity(moduleId, intensity);
    }

    /**
     * 检查强度是否为有限的非负数，拒绝 NaN、无穷大和负值。
     */
    private static boolean isValidIntensity(double intensity) {
        return Double.isFinite(intensity) && intensity >= 0.0D;
    }

    /**
     * 当前模块系统尚未确定具体身份时，返回通用 ID。
     */
    public ResourceLocation getGhostId() {
        // 一个模块定义代表一只完整厉鬼，因此宿主身份取首个已安装模块的 ID。
        if (!ghostModules.getModules().isEmpty()) {
            return ghostModules.getModules().getFirst().id();
        }

        return ResourceLocation.fromNamespaceAndPath(
                MODID,
                "modular_ghost"
        );
    }

    /** 空宿主不自带游荡行为，所有实体 AI 由已安装的完整厉鬼模块提供。 */
    @Override protected void registerGoals() {
        goalSelector.addGoal(2, new GhostCandleAttractionGoal(this));
    }

    /**
     * 取当前全部模块本质强度的最大值作为实体的对外灵异强度。
     * 空模块列表返回零，不按安装顺序选择某个模块。
     */
    @Override public double getSupernaturalStrength() {
        return getModules().stream().mapToDouble(GhostModuleData.Entry::intensity).max().orElse(0);
    }

    /**
     * 返回灵异防御值，供伤害结算使用。
     */
    @Override public double getSupernaturalDefense() {
        return getModules().stream().map(entry -> GhostModuleRegistry.get(entry.id()))
                .filter(Objects::nonNull).mapToDouble(GhostModule::getSupernaturalDefense)
                .filter(value -> Double.isFinite(value) && value >= 0)
                .max().orElse(BASE_SUPERNATURAL_DEFENSE);
    }

    /** 棺材钉属于实体上插入的物品；它压制宿主，但不覆盖模块原有休眠或永久死机状态。 */
    public boolean isCoffinNailed() { return getPersistentData().getBoolean("QisPlan2CoffinNailed"); }

    /**
     * 更新棺材钉约束状态，供实体行动控制检查。
     */
    public void setCoffinNailed(boolean nailed) {
        getPersistentData().putBoolean("QisPlan2CoffinNailed", nailed);
    }

    /**
     * 判断实体是否应整体停止自主行为。
     * 棺材钉约束直接阻止行动；有模块但没有任何可用模块时，也视为整体受限。
     */
    @Override public boolean isSupernaturallyStunned() {
        return isCoffinNailed() || (!getModules().isEmpty()
                && getModules().stream().noneMatch(GhostModuleCombatState::available));
    }

    /**
     * 只有模块列表非空且每个模块都被标记为永久死机时，返回 true。
     */
    @Override public boolean isPermanentlySupernaturallyStunned() {
        return !getModules().isEmpty() && getModules().stream().allMatch(GhostModuleCombatState::permanent);
    }

    /** 旧交互的全体死机操作适配为逐模块写入，宿主不再保存另一份倒计时。 */
    public void setSupernaturalStunTicks(int ticks) {
        for (var entry : List.copyOf(getModules()))
            GhostModuleCombatState.set(this, entry, ticks, GhostModuleCombatState.permanent(entry));
    }

    /**
     * 把灵异压制作用写入模块死机状态，供后续行为与规则检查。
     */
    @Override public void onSupernaturalAttack(int ticks) {
        for (var entry : List.copyOf(getModules())) {
            int total = Math.clamp((long)GhostModuleCombatState.ticks(entry) + ticks, 0, Integer.MAX_VALUE);
            GhostModuleCombatState.set(this, entry, total, GhostModuleCombatState.permanent(entry));
        }
        stopGhostMovement();
    }

    /**
     * 应用永久灵异死机，并停止当前实体行动。
     */
    @Override public void onPermanentSupernaturalAttack() {
        for (var entry : List.copyOf(getModules())) GhostModuleCombatState.set(this, entry, 0, true);
        stopGhostMovement();
    }

    /**
     * 清零各模块的临时死机倒计时。
     * 沿用状态写入接口，保留永久死机标记；不能用此方法解除永久死机。
     */
    @Override public void clearSupernaturalStun() { setSupernaturalStunTicks(0); }

    /**
     * 停止当前导航，清除攻击目标和攻击姿态。
     * 用于受限或死机后的行动收尾，不直接改写实体速度。
     */
    private void stopGhostMovement() {
        getNavigation().stop();
        setTarget(null);
        setAggressive(false);
    }

    /**
     * 推进实体 AI 生命周期，并在受限状态下维护行动停止与相关状态。
     */
    @Override public void aiStep() {
        super.aiStep();
        if (!level().isClientSide) {
            GhostModuleCombatState.tick(this);
            if (isSupernaturallyStunned()) {
                stopGhostMovement();
                return;
            }
            tickGhostAI();
        }
    }

    /**
     * 查询统一目标策略，判断当前厉鬼能否探测指定玩家。
     */
    public boolean canDetectPlayer(Player player) {
        return GhostAvoidanceSystem.canDetect(this, player);
    }

    /**
     * 查询统一目标策略，判断当前厉鬼是否允许袭击指定玩家。
     */
    public boolean canAttackPlayer(Player player) {
        return GhostAvoidanceSystem.canAttack(this, player);
    }

    /**
     * 读取玩家在统一目标策略中的优先级，供目标选择比较。
     */
    public int getPlayerTargetPriority(Player player) {
        return GhostAvoidanceSystem.getTargetPriority(this, player);
    }

    /**
     * 声明实体的远距离自动清除策略。
     */
    @Override public boolean removeWhenFarAway(double distance) { return false; }

    /**
     * 声明实体是否需要自定义持久保留，供原版清除逻辑使用。
     */
    @Override public boolean requiresCustomPersistence() { return true; }

    /**
     * 声明和平难度下的自动消失策略。
     */
    @Override protected boolean shouldDespawnInPeaceful() { return false; }

    /**
     * 检查当前伤害来源是否应被实体免疫策略拦截。
     */
    @Override public boolean isInvulnerableTo(DamageSource source) {
        return SupernaturalCombatHandler.isInvulnerableTo(this, source);
    }

    /**
     * 处理玩家与模块化实体的交互，将符合条件的交互接入驾驭流程。
     */
    @Override public InteractionResult mobInteract(
            Player player, InteractionHand hand) {
        return GhostPossessionInteractionSystem.handle(this, player, hand);
    }

    /**
     * 创建通用模块化实体的基础属性表，供实体类型注册使用。
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    /**
     * 保存实体附加状态和模块数据，保证模块专属状态随存档持久化。
     */
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        ListTag modulesTag = new ListTag();

        for (GhostModuleData.Entry entry : ghostModules.getModules()) {
            CompoundTag moduleTag = new CompoundTag();

            moduleTag.putString(
                    NBT_MODULE_ID,
                    entry.id().toString()
            );

            moduleTag.putDouble(
                    NBT_MODULE_INTENSITY,
                    entry.intensity()
            );

            moduleTag.putDouble(
                    NBT_MODULE_REVIVAL,
                    entry.revival()
            );

            moduleTag.putBoolean(
                    NBT_MODULE_SLEEPING,
                    entry.sleeping()
            );

            moduleTag.putBoolean(
                    NBT_MODULE_DEADLOCKED,
                    entry.deadlocked()
            );

            moduleTag.put(
                    NBT_MODULE_CUSTOM_DATA,
                    entry.customData()
            );

            modulesTag.add(moduleTag);
        }

        tag.put(NBT_GHOST_MODULES, modulesTag);
    }

    /**
     * 读取实体附加状态与模块数据，并恢复所需的运行时绑定。
     */
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (!tag.contains(NBT_GHOST_MODULES, Tag.TAG_LIST)) {
            refreshModuleBindings();
            return;
        }
        ghostModules.clear();

        ListTag modulesTag = tag.getList(
                NBT_GHOST_MODULES,
                Tag.TAG_COMPOUND
        );

        List<GhostModuleData.Entry> loadedModules = new ArrayList<>();

        for (int i = 0; i < modulesTag.size(); i++) {
            CompoundTag moduleTag = modulesTag.getCompound(i);

            ResourceLocation moduleId = ResourceLocation.tryParse(
                    moduleTag.getString(NBT_MODULE_ID)
            );

            if (moduleId == null) {
                continue;
            }

            double intensity = moduleTag.contains(
                    NBT_MODULE_INTENSITY,
                    Tag.TAG_DOUBLE
            )
                    ? moduleTag.getDouble(NBT_MODULE_INTENSITY)
                    : 1.0D;

            double revival = moduleTag.contains(
                    NBT_MODULE_REVIVAL,
                    Tag.TAG_DOUBLE
            )
                    ? moduleTag.getDouble(NBT_MODULE_REVIVAL)
                    : 0.0D;

            boolean sleeping = moduleTag.getBoolean(
                    NBT_MODULE_SLEEPING
            );

            boolean deadlocked = moduleTag.getBoolean(
                    NBT_MODULE_DEADLOCKED
            );

            CompoundTag customData = moduleTag.contains(
                    NBT_MODULE_CUSTOM_DATA,
                    Tag.TAG_COMPOUND
            )
                    ? moduleTag.getCompound(NBT_MODULE_CUSTOM_DATA)
                    : new CompoundTag();

            if (!Double.isFinite(intensity) || intensity < 0.0D
                    || !Double.isFinite(revival) || revival < 0.0D) {
                continue;
            }

            loadedModules.add(
                    new GhostModuleData.Entry(
                            moduleId,
                            intensity,
                            revival,
                            sleeping,
                            deadlocked,
                            customData
                    )
            );
        }

        ghostModules.loadEntries(loadedModules);
        // 兼容旧统一宿主存档：只迁移遗留死机字段，不覆盖模块现有强度、复苏和专属数据。
        if (tag.contains("QisPlan2SupernaturalStunTicks") || tag.contains("QisPlan2PermanentSupernaturalStun")) {
            int legacyTicks = Math.max(0, tag.getInt("QisPlan2SupernaturalStunTicks"));
            boolean legacyPermanent = tag.getBoolean("QisPlan2PermanentSupernaturalStun");
            for (var entry : List.copyOf(getModules())) {
                GhostModuleCombatState.set(this, entry, Math.max(legacyTicks, GhostModuleCombatState.ticks(entry)),
                        legacyPermanent || GhostModuleCombatState.permanent(entry));
            }
        }
        refreshModuleBindings();

    }

    /** 新生成的子类实体也需要激活默认模块行为；此时子类字段已完成初始化。 */
    @Override public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!level().isClientSide) {
            refreshModuleBindings();
            syncModuleSounds();
        }
    }

    /** 模块资源维护独立于规则和移动 AI，确保死机时也能撤销鬼域。 */
    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            for (var entry : List.copyOf(getModules())) {
                GhostModule module = GhostModuleRegistry.get(entry.id());
                if (module != null && hasGhostModule(entry.id())) GhostEntityBindings.tick(module, this);
            }
        }
    }

    /**
     * 推进模块化实体的灵异 AI，具体行为由已安装模块的适配器提供。
     */
    private void tickGhostAI() {
        // 保留现有的统一模块 Tick 事件
        GhostModuleRuntime.dispatch(this, new ModuleTickEvent());

        // 实体触碰检测只在服务端执行
        if (level().isClientSide) {
            return;
        }

        // 更新实体声音源的位置。
        syncModuleSounds();

        Set<UUID> currentTouchingEntities = new HashSet<>();

        for (LivingEntity target : level().getEntitiesOfClass(
                LivingEntity.class,
                getBoundingBox(),
                entity -> entity != this
                        && entity.isAlive()
                        && !entity.isSpectator()
        )) {
            UUID targetId = target.getUUID();
            currentTouchingEntities.add(targetId);

            // 每 tick 报告真实接触时长；事件阶段保留进入语义，避免不可触规则重复袭击。
            boolean beginning = touchingEntities.add(targetId);
            GhostModuleRuntime.dispatch(this, TouchEvent.sample(this, target, beginning));
        }

        // 目标离开碰撞范围后，允许下一次接触重新触发
        touchingEntities.retainAll(currentTouchingEntities);
    }
}
