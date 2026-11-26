package com.qidate.qisplan2.ghost.module.implementation.divinationslip;

import com.qidate.qisplan2.ghost.curse.*;
import com.qidate.qisplan2.ghost.curse.type.ghostdivination.GhostDivinationCurseSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import java.util.UUID;

/** 鬼签的持续复苏能力，定义归属完整鬼签模块，不另行注册。 */
public final class GhostRevivalCurseType implements CurseType {
    public static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath("qisplan2","ghost_divination_revival");
    /** 此 ID 用于存档和展示，绝不替代完整模块 ID。 */
    /** HUD 图标也随模块定义提供。 */
    @Override public ResourceLocation icon(){return ResourceLocation.fromNamespaceAndPath("qisplan2","textures/item/ghost_sign.png");}
    @Override public ResourceLocation id(){return ID;}
    /** 活跃期间复苏效率翻倍；倍率由共享复苏入口读取。 */
    @Override public double revivalMultiplier(){return 2;}
    /** 新实例拥有独立 UUID、强度和寿命。 */
    @Override public Curse create(UUID target,CurseSource source,CompoundTag state){return new Instance(UUID.randomUUID(),target,source,state.getInt("Strength"),state.contains("RemainingTicks")?state.getInt("RemainingTicks"):6000);}
    /** 完整还原实例；倍速不是向目标写一个永久属性，因此不会残留。 */
    @Override public Curse load(CompoundTag tag){return new Instance(tag.getUUID("Id"),tag.getUUID("Target"),GhostDivinationCurseSource.load(tag.getCompound("Source")),tag.getInt("Strength"),tag.getInt("RemainingTicks"));}
    /** 只保存诅咒状态，复苏倍率由定义提供。 */
    private static final class Instance extends AbstractCurse {
        private Instance(UUID id,UUID target,CurseSource source,int strength,int ticks){super(id,ID,target,source,strength,ticks);}
        /** 常态属性不需要每 tick 向目标追加增益，持续时间由管理器统一推进。 */
        @Override public void tick(MinecraftServer server){}
    }
}
