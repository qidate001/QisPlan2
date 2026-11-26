package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.core.ModDataComponents;
import com.qidate.qisplan2.entity.ModularGhostEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.host.attachment.AttachedGhostHost;
import com.qidate.qisplan2.ghost.module.host.attachment.GhostAttachedItemSnapshot;
import com.qidate.qisplan2.ghost.module.host.attachment.GhostAttachmentRecord;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemData;
import com.qidate.qisplan2.ghost.module.implementation.CoffinNailModule;
import com.qidate.qisplan2.ghost.module.implementation.GhostBookModule;
import com.qidate.qisplan2.item.CoffinNailItem;
import com.qidate.qisplan2.item.GhostBlankSwordItem;
import java.util.List;
import java.util.Map;
import net.neoforged.fml.loading.LoadingModList;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** 独立进程使用真实物品、组件 Codec 和注册表验证附着快照，不以手写 NBT 冒充物品往返。 */
public final class AttachmentItemSnapshotTest {
    /** 失败直接终止 Gradle 检查。 */
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }

    /** 仅在独立回归进程中注册两个真实物品和模块组件，不启动世界或修改正式注册流程。 */
    public static void main(String[] args) {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ((MappedRegistry<DataComponentType<?>>) BuiltInRegistries.DATA_COMPONENT_TYPE).unfreeze();
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, ResourceLocation.parse("qisplan2:ghost_modules"),
                DataComponentType.<List<GhostModuleData.Entry>>builder().persistent(GhostModuleData.CODEC).build());
        ((MappedRegistry<Item>) BuiltInRegistries.ITEM).unfreeze();
        var nail = Registry.register(BuiltInRegistries.ITEM, ResourceLocation.parse("qisplan2:coffin_nail"),
                new CoffinNailItem(new Item.Properties().stacksTo(1)));
        var sword = Registry.register(BuiltInRegistries.ITEM, ResourceLocation.parse("qisplan2:ghost_blank_sword"),
                new GhostBlankSwordItem(new Item.Properties().durability(600)));
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var definition = new CoffinNailModule();
        GhostModuleRegistry.register(definition);
        GhostModuleRegistry.register(new GhostBookModule());

        var plain = new ItemStack(nail);
        var old = (CompoundTag) plain.save(registries);
        check(!old.getCompound("components").contains("qisplan2:ghost_modules"), "复现原版省略默认模块");
        var saved = GhostAttachedItemSnapshot.save(plain, registries);
        check(saved.getCompound("components").contains("qisplan2:ghost_modules"), "默认棺材钉也必须显式保存模块");
        check(ItemStack.isSameItemSameComponents(plain, GhostAttachedItemSnapshot.restore(saved, registries)), "默认棺材钉组件完整往返");
        check(ItemStack.isSameItemSameComponents(plain, GhostAttachedItemSnapshot.restore(old, registries)), "兼容旧版仅ID记录");

        var original = new ItemStack(sword);
        original.setDamageValue(137);
        original.set(DataComponents.CUSTOM_NAME, Component.literal("带棺材钉模块的白板剑"));
        var custom = new CompoundTag(); custom.putString("test", "模块私有数据"); custom.putInt("progress", 91);
        var modules = List.of(new GhostModuleData.Entry(CoffinNailModule.ID, 73, 26, true, true, custom),
                new GhostModuleData.Entry(GhostBookModule.ID, 19, 8, false, false, new CompoundTag()));
        var egg=new ItemStack(Items.PIG_SPAWN_EGG);
        var marker=new CompoundTag();marker.putString("GhostModule",CoffinNailModule.ID.toString());
        var spawnData=ModularGhostEntity.createSpawnData(modules);
        egg.set(DataComponents.CUSTOM_DATA,CustomData.of(marker));
        egg.set(DataComponents.ENTITY_DATA,CustomData.of(spawnData));
        egg.set(ModDataComponents.GHOST_MODULES.get(),modules);
        check(GhostItemData.getModules(egg).isEmpty(),"旧刷怪蛋模板不能成为活跃物品模块");
        check(egg.get(DataComponents.ENTITY_DATA).copyTag().equals(spawnData),"过滤旧模板不得丢失待生成实体的模块数据");
        egg.remove(ModDataComponents.GHOST_MODULES.get());
        check(GhostItemData.getModules(egg).isEmpty(),"新刷怪蛋仅保存实体数据");
        original.set(ModDataComponents.GHOST_MODULES.get(), modules);
        original.remove(DataComponents.ATTRIBUTE_MODIFIERS);
        var originId = UUID.randomUUID();
        var record = new GhostAttachmentRecord(originId, GhostAttachedItemSnapshot.save(original.copyWithCount(1), registries), true);
        var stored = record.save();
        original.setDamageValue(5); // 插入后的手中副本变化不能污染已保存来源。
        original.set(ModDataComponents.GHOST_MODULES.get(), List.of());
        var loaded = GhostAttachmentRecord.load(stored).orElseThrow();
        var returned = GhostAttachedItemSnapshot.restore(loaded.item(), registries);
        check(returned.getItem() == sword && returned.getCount() == 1, "取出必须仍为同类型白板剑，不能变成棺材钉");
        check(returned.getDamageValue() == 137 && returned.getHoverName().getString().equals("带棺材钉模块的白板剑"), "耐久与名称必须保留");
        check(GhostItemData.getModules(returned).equals(modules), "模块强度、复苏、沉睡、死机、customData及其他模块必须全部保留");
        check(!returned.has(DataComponents.ATTRIBUTE_MODIFIERS), "显式删除的默认组件不能复活");
        check(loaded.instance().equals(originId), "重载不更换附着来源身份");
        var host = new AttachedGhostHost(returned);
        check(!definition.getSuppressions(new GhostModuleContext(host, CoffinNailModule.ID)).isEmpty(),
                "白板剑上的棺材钉模块必须通过相同接口形成压制");
        var again = GhostAttachedItemSnapshot.restore(GhostAttachedItemSnapshot.save(returned, registries), registries);
        check(ItemStack.isSameItemSameComponents(returned, again), "多次保存重载不能改变组件");

        var empty = new ItemStack(nail);
        empty.set(ModDataComponents.GHOST_MODULES.get(), List.of());
        check(GhostItemData.getModules(GhostAttachedItemSnapshot.restore(
                GhostAttachedItemSnapshot.save(empty, registries), registries)).isEmpty(), "空模块列表不得恢复物品默认棺材钉模块");
        var absent = saved.copy(); absent.getCompound("components").remove("qisplan2:ghost_modules");
        check(!GhostAttachedItemSnapshot.restore(absent, registries).has(ModDataComponents.GHOST_MODULES.get()),
                "完整快照中不存在的组件不能被物品默认值重新注入");
        System.out.println("Real coffin nail and blank sword full-component snapshot round-trip checks passed");
    }
}
