package com.qidate.qisplan2.ghost.possession.manager;

import com.qidate.qisplan2.core.ModAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GhostSuppressionAllocationHandler {

    private GhostSuppressionAllocationHandler() {}

    /**
     * 获取玩家全部压制额度分配。
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
     * 获取指定“来源鬼 → 目标鬼”的全部分配。
     */
    public static List<SuppressionAllocation> getAllocations(
            Player player,
            ResourceLocation sourceGhost,
            ResourceLocation targetGhost
    ) {

        Map<
                ResourceLocation,
                Map<ResourceLocation, List<SuppressionAllocation>>
                > allocations =
                getAllocations(player);

        Map<ResourceLocation, List<SuppressionAllocation>>
                targets =
                allocations.get(sourceGhost);

        if (targets == null) {
            return List.of();
        }

        List<SuppressionAllocation> list =
                targets.get(targetGhost);

        if (list == null) {
            return List.of();
        }

        return list;
    }

    /**
     * 判断某个来源鬼的 slot 是否已经被分配。
     */
    public static boolean isSlotAllocated(
            Player player,
            ResourceLocation sourceGhost,
            int slotIndex
    ) {

        Map<
                ResourceLocation,
                Map<ResourceLocation, List<SuppressionAllocation>>
                > allocations =
                getAllocations(player);

        Map<ResourceLocation, List<SuppressionAllocation>>
                targets =
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
     * 分配一个新的压制额度。
     *
     * 一个 slot 只能存在一份分配。
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
                > oldAllocations =
                getAllocations(player);

        Map<
                ResourceLocation,
                Map<ResourceLocation, List<SuppressionAllocation>>
                > allocations =
                deepCopy(oldAllocations);

        Map<ResourceLocation, List<SuppressionAllocation>>
                targets =
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

        player.setData(
                ModAttachments.GHOST_SUPPRESSION_ALLOCATION,
                allocations
        );

        return true;
    }

    /**
     * 删除指定来源鬼的某一个 slot 分配。
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
                > oldAllocations =
                getAllocations(player);

        Map<
                ResourceLocation,
                Map<ResourceLocation, List<SuppressionAllocation>>
                > allocations =
                deepCopy(oldAllocations);

        Map<ResourceLocation, List<SuppressionAllocation>>
                targets =
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
                                allocation.slotIndex()
                                        == slotIndex
                );

        if (!removed) {
            return false;
        }

        /*
         * 清理空容器。
         */
        if (list.isEmpty()) {
            targets.remove(targetGhost);
        }

        if (targets.isEmpty()) {
            allocations.remove(sourceGhost);
        }

        player.setData(
                ModAttachments.GHOST_SUPPRESSION_ALLOCATION,
                allocations
        );

        return true;
    }

    /**
     * 移动一个已经存在的压制额度。
     *
     * oldTargetGhost：
     * 原来的目标鬼。
     *
     * newTargetGhost：
     * 新的目标鬼。
     *
     * x / y：
     * 新目标卡片中的相对位置。
     */
    public static boolean move(
            Player player,
            ResourceLocation sourceGhost,
            ResourceLocation oldTargetGhost,
            ResourceLocation newTargetGhost,
            int slotIndex,
            double x,
            double y
    ) {

        if (sourceGhost.equals(newTargetGhost)) {
            return false;
        }

        Map<
                ResourceLocation,
                Map<ResourceLocation, List<SuppressionAllocation>>
                > oldAllocations =
                getAllocations(player);

        Map<
                ResourceLocation,
                Map<ResourceLocation, List<SuppressionAllocation>>
                > allocations =
                deepCopy(oldAllocations);

        Map<ResourceLocation, List<SuppressionAllocation>>
                targets =
                allocations.get(sourceGhost);

        if (targets == null) {
            return false;
        }

        List<SuppressionAllocation> oldList =
                targets.get(oldTargetGhost);

        if (oldList == null) {
            return false;
        }

        boolean removed =
                oldList.removeIf(
                        allocation ->
                                allocation.slotIndex()
                                        == slotIndex
                );

        if (!removed) {
            return false;
        }

        /*
         * 原目标已经没有这个分配后，
         * 再加入新目标。
         */
        if (oldList.isEmpty()) {
            targets.remove(oldTargetGhost);
        }

        List<SuppressionAllocation> newList =
                targets.computeIfAbsent(
                        newTargetGhost,
                        ignored -> new ArrayList<>()
                );

        /*
         * 理论上 slot 不应该已经存在于其他目标。
         * 这里再检查一次，防止非法状态。
         */
        for (List<SuppressionAllocation> list
                : targets.values()) {

            for (SuppressionAllocation allocation : list) {

                if (allocation.slotIndex() == slotIndex) {
                    return false;
                }
            }
        }

        newList.add(
                new SuppressionAllocation(
                        slotIndex,
                        x,
                        y
                )
        );

        player.setData(
                ModAttachments.GHOST_SUPPRESSION_ALLOCATION,
                allocations
        );

        return true;
    }

    /**
     * 深拷贝整个压制分配结构。
     *
     * Attachment 中的默认数据可能是不可变 Map/List，
     * 所以所有修改都必须基于新的可变结构进行。
     */
    private static Map<
            ResourceLocation,
            Map<ResourceLocation, List<SuppressionAllocation>>
            > deepCopy(
            Map<
                    ResourceLocation,
                    Map<ResourceLocation, List<SuppressionAllocation>>
                    > source
    ) {

        Map<
                ResourceLocation,
                Map<ResourceLocation, List<SuppressionAllocation>>
                > result =
                new HashMap<>();

        for (var sourceEntry : source.entrySet()) {

            Map<ResourceLocation, List<SuppressionAllocation>>
                    targets =
                    new HashMap<>();

            for (var targetEntry
                    : sourceEntry.getValue().entrySet()) {

                targets.put(
                        targetEntry.getKey(),
                        new ArrayList<>(
                                targetEntry.getValue()
                        )
                );
            }

            result.put(
                    sourceEntry.getKey(),
                    targets
            );
        }

        return result;
    }
}