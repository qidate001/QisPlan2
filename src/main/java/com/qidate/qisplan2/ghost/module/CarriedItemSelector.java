package com.qidate.qisplan2.ghost.module;

import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** 携带物品订阅条件；物品 ID、Tag 或组件条件在观察阶段匹配，不广播给无关规则。 */
@FunctionalInterface
public interface CarriedItemSelector {
    /** 检查非空物品；实体参数是被观察的携带者，并非模块宿主。 */
    boolean matches(ItemStack stack, LivingEntity carrier);
    /** 订阅指定物品 ID 的任意一种。 */
    static CarriedItemSelector items(ResourceLocation... ids) {
        var accepted = Set.of(ids);
        return (stack, carrier) -> !stack.isEmpty() && accepted.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }
    /** 每次使用当前 Tag 内容，数据包重载后无需重建订阅。 */
    static CarriedItemSelector tag(TagKey<Item> tag) {
        return (stack, carrier) -> !stack.isEmpty() && stack.is(tag);
    }
    /** 食物只是通用物品条件的一种，保留 NeoForge 按携带者计算食物属性的支持。 */
    static CarriedItemSelector food() {
        return (stack, carrier) -> !stack.isEmpty() && stack.getFoodProperties(carrier) != null;
    }
}
