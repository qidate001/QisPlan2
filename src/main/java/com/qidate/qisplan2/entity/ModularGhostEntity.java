
package com.qidate.qisplan2.entity;

import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.GhostModuleDataModifier;
import com.qidate.qisplan2.ghost.module.GhostModuleRuntime;
import com.qidate.qisplan2.ghost.module.event.ModuleTickEvent;
import com.qidate.qisplan2.ghost.module.event.TouchEvent;
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
        implements GhostModuleDataModifier {

    private static final double BASE_SUPERNATURAL_STRENGTH = 5.0D;
    private static final double BASE_SUPERNATURAL_DEFENSE = 4.0D;

    private static final String NBT_GHOST_MODULES =
            "QisPlan2GhostModules";

    private static final String NBT_MODULE_ID = "id";
    private static final String NBT_MODULE_INTENSITY = "intensity";

    private final List<GhostModuleData.Entry> ghostModules =
            new ArrayList<>();

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
        return List.copyOf(ghostModules);
    }

    /**
     * 判断实体是否安装指定模块。
     */
    public boolean hasGhostModule(ResourceLocation moduleId) {
        Objects.requireNonNull(moduleId, "moduleId");

        return hasModule(moduleId);
    }

    /**
     * 安装模块。
     *
     * 同一个模块 ID 再次添加时，会更新其灵异强度。
     */
    public void addGhostModule(
            ResourceLocation moduleId,
            double intensity
    ) {
        Objects.requireNonNull(moduleId, "moduleId");

        if (!isValidIntensity(intensity)) {
            throw new IllegalArgumentException(
                    "Invalid ghost module intensity: " + intensity
            );
        }

        ghostModules.removeIf(
                existing -> existing.id().equals(moduleId)
        );

        ghostModules.add(
                new GhostModuleData.Entry(moduleId, intensity)
        );
    }

    /**
     * 移除模块。
     */
    public void removeGhostModule(ResourceLocation moduleId) {
        Objects.requireNonNull(moduleId, "moduleId");

        ghostModules.removeIf(
                entry -> entry.id().equals(moduleId)
        );
    }

    @Override
    public boolean addModule(ResourceLocation moduleId, double intensity) {
        Objects.requireNonNull(moduleId, "moduleId");

        if (!isValidIntensity(intensity)) {
            return false;
        }

        boolean unchanged = ghostModules.stream()
                .anyMatch(entry ->
                        entry.id().equals(moduleId)
                                && Double.compare(
                                entry.intensity(),
                                intensity
                        ) == 0
                );

        if (unchanged) {
            return false;
        }

        addGhostModule(moduleId, intensity);
        return true;
    }

    @Override
    public boolean removeModule(ResourceLocation moduleId) {
        Objects.requireNonNull(moduleId, "moduleId");

        if (!hasModule(moduleId)) {
            return false;
        }

        removeGhostModule(moduleId);
        return true;
    }

    @Override
    public boolean setIntensity(
            ResourceLocation moduleId,
            double intensity
    ) {
        Objects.requireNonNull(moduleId, "moduleId");

        if (!isValidIntensity(intensity) || !hasModule(moduleId)) {
            return false;
        }

        boolean unchanged = ghostModules.stream()
                .anyMatch(entry ->
                        entry.id().equals(moduleId)
                                && Double.compare(
                                entry.intensity(),
                                intensity
                        ) == 0
                );

        if (unchanged) {
            return false;
        }

        addGhostModule(moduleId, intensity);
        return true;
    }

    private static boolean isValidIntensity(double intensity) {
        return Double.isFinite(intensity) && intensity >= 0.0D;
    }

    /**
     * 当前模块系统尚未确定具体身份时，返回通用 ID。
     */
    @Override
    public ResourceLocation getGhostId() {
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

        for (GhostModuleData.Entry entry : ghostModules) {
            CompoundTag moduleTag = new CompoundTag();

            moduleTag.putString(
                    NBT_MODULE_ID,
                    entry.id().toString()
            );

            moduleTag.putDouble(
                    NBT_MODULE_INTENSITY,
                    entry.intensity()
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

            if (!isValidIntensity(intensity)) {
                continue;
            }

            addGhostModule(moduleId, intensity);
        }
    }

    @Override
    protected void tickGhostAI() {
        // 保留现有的统一模块 Tick 事件
        GhostModuleRuntime.dispatch(this, new ModuleTickEvent());

        // 实体触碰检测只在服务端执行
        if (level().isClientSide) {
            return;
        }

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
