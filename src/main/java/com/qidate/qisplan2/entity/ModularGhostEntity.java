package com.qidate.qisplan2.entity;

import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleContainer;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.event.ModuleTickEvent;
import com.qidate.qisplan2.ghost.module.event.TouchEvent;
import com.qidate.qisplan2.ghost.module.sound.GhostModuleSoundManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.qidate.qisplan2.QisPlan2.MODID;

/**
 * 通用模块化厉鬼实体。
 *
 * 实体的具体身份和灵异规则由后续安装的实体模块决定。
 */
public class ModularGhostEntity extends AbstractGhostEntity
        implements GhostModuleDataModifier, GhostModuleCustomDataModifier {

    private static final double BASE_SUPERNATURAL_STRENGTH = 5.0D;
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

    private final GhostModuleContainer ghostModules =
            new GhostModuleContainer();

    private final Set<UUID> touchingEntities = new HashSet<>();

    public ModularGhostEntity(
            EntityType<? extends ModularGhostEntity> entityType,
            Level level
    ) {
        super(entityType, level);

        GhostAttributeSystem.setSupernaturalStrength(
                this,
                BASE_SUPERNATURAL_STRENGTH
        );

        GhostAttributeSystem.setSupernaturalDefense(
                this,
                BASE_SUPERNATURAL_DEFENSE
        );
    }

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
        refreshSupernaturalDefense();
        syncModuleSounds();
    }

    /**
     * 移除模块。
     */
    public void removeGhostModule(ResourceLocation moduleId) {
        ghostModules.removeModule(moduleId);
        refreshSupernaturalDefense();
        syncModuleSounds();
    }

    /**
     * 根据当前模块定义刷新宿主的灵异防御。
     *
     * 模块负责声明完整厉鬼的防御基础值；宿主只负责汇总，
     * 因此安装或移除模块时不会依赖专用实体类型来设置属性。
     */
    private final java.util.Map<ResourceLocation, List<net.minecraft.world.entity.ai.goal.Goal>> moduleGoals =
            new java.util.HashMap<>();

    /** 同步模块提供的行为；重新读取存档和重复安装不会重复挂载同一组行为。 */
    private void refreshModuleGoals() {
        var iterator = moduleGoals.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (!hasGhostModule(entry.getKey())) {
                entry.getValue().forEach(goalSelector::removeGoal);
                iterator.remove();
            }
        }
        for (var entry : getModules()) {
            GhostModule module = GhostModuleRegistry.get(entry.id());
            if (module == null || moduleGoals.containsKey(entry.id())) continue;
            var goals = module.createEntityGoals(this);
            for (int i = 0; i < goals.size(); i++) goalSelector.addGoal(i + 1, goals.get(i));
            moduleGoals.put(entry.id(), goals);
        }
    }

    private void refreshSupernaturalDefense() {
        refreshModuleGoals();
        double defense = BASE_SUPERNATURAL_DEFENSE;

        for (GhostModuleData.Entry entry : ghostModules.getModules()) {
            GhostModule module = GhostModuleRegistry.get(entry.id());
            if (module == null) {
                continue;
            }

            double moduleDefense = module.getSupernaturalDefense();
            if (Double.isFinite(moduleDefense) && moduleDefense >= 0.0D) {
                defense = Math.max(defense, moduleDefense);
            }
        }

        GhostAttributeSystem.setSupernaturalDefense(this, defense);
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
            refreshSupernaturalDefense();
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
            refreshSupernaturalDefense();
            syncModuleSounds();
        }

        return changed;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            GhostModuleSoundManager.unregisterEntityHost(this);
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

    private static boolean isValidIntensity(double intensity) {
        return Double.isFinite(intensity) && intensity >= 0.0D;
    }

    /**
     * 当前模块系统尚未确定具体身份时，返回通用 ID。
     */
    @Override
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

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

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

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

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
        refreshSupernaturalDefense();

    }

    @Override
    protected void tickGhostAI() {
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

            // 仅在刚刚接触目标时派发一次事件
            if (touchingEntities.add(targetId)) {
                GhostModuleRuntime.dispatch(
                        this,
                        new TouchEvent(this, target)
                );
            }
        }

        // 目标离开碰撞范围后，允许下一次接触重新触发
        touchingEntities.retainAll(currentTouchingEntities);
    }
}
