package com.qidate.qisplan2.mixin;

import com.qidate.qisplan2.ghost.module.GhostModuleRuntime;
import com.qidate.qisplan2.ghost.module.event.ItemDestructionAttemptEvent;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemModuleHost;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackDestructionAttemptMixin {

    @Unique
    private static final ThreadLocal<Deque<DamageContext>>
            QISPLAN2_DAMAGE_CONTEXT =
            ThreadLocal.withInitial(ArrayDeque::new);

    @Unique
    private record DamageContext(ItemStack stack, int originalDamage) {}

    @Inject(
            method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
            at = @At("HEAD")
    )
    private void qisplan2$beginDamage(
            int damage,
            ServerLevel level,
            @Nullable LivingEntity holder,
            Consumer<Item> onBreak,
            CallbackInfo ci
    ) {
        ItemStack stack = (ItemStack) (Object) this;

        QISPLAN2_DAMAGE_CONTEXT.get().push(
                new DamageContext(stack, stack.getDamageValue())
        );
    }

    @Inject(
            method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"
            ),
            cancellable = true
    )
    private void qisplan2$beforeShrink(
            int damage,
            ServerLevel level,
            @Nullable LivingEntity holder,
            Consumer<Item> onBreak,
            CallbackInfo ci
    ) {
        ItemStack stack = (ItemStack) (Object) this;
        Deque<DamageContext> contexts = QISPLAN2_DAMAGE_CONTEXT.get();

        DamageContext context = contexts.peek();
        if (context == null || context.stack() != stack) {
            return;
        }

        int originalDamage = context.originalDamage();
        int resultingDamage = stack.getDamageValue();
        int maxDamage = stack.getMaxDamage();

        if (originalDamage >= maxDamage
                || resultingDamage < maxDamage
                || resultingDamage <= originalDamage) {
            return;
        }

        GhostItemModuleHost host = new GhostItemModuleHost(stack);
        if (host.getModules().isEmpty()) {
            return;
        }

        ItemDestructionAttemptEvent event =
                new ItemDestructionAttemptEvent(
                        stack,
                        holder,
                        originalDamage,
                        resultingDamage - originalDamage,
                        maxDamage
                );

        GhostModuleRuntime.dispatch(host, event);

        if (event.isCanceled()) {
            stack.setDamageValue(originalDamage);

            // ci.cancel() 会提前退出原方法，因此这里主动清理上下文。
            contexts.pop();

            if (contexts.isEmpty()) {
                QISPLAN2_DAMAGE_CONTEXT.remove();
            }

            ci.cancel();
        }
    }

    @Inject(
            method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
            at = @At("RETURN")
    )
    private void qisplan2$endDamage(
            int damage,
            ServerLevel level,
            @Nullable LivingEntity holder,
            Consumer<Item> onBreak,
            CallbackInfo ci
    ) {
        Deque<DamageContext> contexts = QISPLAN2_DAMAGE_CONTEXT.get();

        if (!contexts.isEmpty()) {
            contexts.pop();
        }

        if (contexts.isEmpty()) {
            QISPLAN2_DAMAGE_CONTEXT.remove();
        }
    }
}