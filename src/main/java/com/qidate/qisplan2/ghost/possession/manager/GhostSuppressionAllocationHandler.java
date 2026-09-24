package com.qidate.qisplan2.ghost.possession.manager;

import com.qidate.qisplan2.core.ModAttachments;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GhostSuppressionAllocationHandler {

    private GhostSuppressionAllocationHandler() {
    }

    /**
     * 获取玩家当前所有灵异压制分配。
     */
    public static Map<
            ResourceLocation,
            Map<ResourceLocation, List<SuppressionAllocation>>
            > getAllocations(Player player) {

        return player.getData(
                ModAttachments.GHOST_SUPPRESSION_ALLOCATION
        );
    }

    /**
     * 获取某个源鬼对某个目标鬼的所有压制分配。
     */
    public static List<SuppressionAllocation> getAllocations(
            Player player,
            ResourceLocation sourceGhost,
            ResourceLocation targetGhost
    ) {

        Map<
                ResourceLocation,
                Map<ResourceLocation, List<SuppressionAllocation>>
                > allocations = getAllocations(player);

        Map<ResourceLocation, List<SuppressionAllocation>> targets =
                allocations.get(sourceGhost);

        if (targets == null) {
            return List.of();
        }

        List<SuppressionAllocation> result =
                targets.get(targetGhost);

        if (result == null) {
            return List.of();
        }

        return result;
    }

    /**
     * 判断某个源鬼的某一个压制额度是否已经被使用。
     */
    public static boolean isSlotAllocated(
            Player player,
            ResourceLocation sourceGhost,
            int slotIndex
    ) {

        Map<
                ResourceLocation,
                Map<ResourceLocation, List<SuppressionAllocation>>
                > allocations = getAllocations(player);

        Map<ResourceLocation, List<SuppressionAllocation>> targets =
                allocations.get(sourceGhost);

        if (targets == null) {
            return false;
        }

        for (List<SuppressionAllocation> list : targets.values()) {

            for (SuppressionAllocation allocation : list) {

                if (allocation.slotIndex() == slotIndex) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 将一个压制额度分配给目标鬼。
     *
     * x / y 为目标鬼卡片内部的相对坐标。
     */
    public static boolean allocate(
            Player player,
            ResourceLocation sourceGhost,
            ResourceLocation targetGhost,
            int slotIndex,
            double x,
            double y
    ) {

        if (sourceGhost.equals(targetGhost)) {
            return false;
        }

        if (isSlotAllocated(
                player,
                sourceGhost,
                slotIndex
        )) {
            return false;
        }

        Map<
                ResourceLocation,
                Map<ResourceLocation, List<SuppressionAllocation>>
                > allocations = getAllocations(player);

        Map<ResourceLocation, List<SuppressionAllocation>> targets =
                allocations.computeIfAbsent(
                        sourceGhost,
                        ignored -> new HashMap<>()
                );

        List<SuppressionAllocation> list =
                targets.computeIfAbsent(
                        targetGhost,
                        ignored -> new ArrayList<>()
                );

        list.add(
                new SuppressionAllocation(
                        slotIndex,
                        x,
                        y
                )
        );

        return true;
    }

    /**
     * 移除一个已经分配出去的压制额度。
     */
    public static boolean remove(
            Player player,
            ResourceLocation sourceGhost,
            ResourceLocation targetGhost,
            int slotIndex
    ) {

        Map<
                ResourceLocation,
                Map<ResourceLocation, List<SuppressionAllocation>>
                > allocations = getAllocations(player);

        Map<ResourceLocation, List<SuppressionAllocation>> targets =
                allocations.get(sourceGhost);

        if (targets == null) {
            return false;
        }

        List<SuppressionAllocation> list =
                targets.get(targetGhost);

        if (list == null) {
            return false;
        }

        boolean removed =
                list.removeIf(
                        allocation ->
                                allocation.slotIndex() == slotIndex
                );

        if (list.isEmpty()) {
            targets.remove(targetGhost);
        }

        if (targets.isEmpty()) {
            allocations.remove(sourceGhost);
        }

        return removed;
    }
}