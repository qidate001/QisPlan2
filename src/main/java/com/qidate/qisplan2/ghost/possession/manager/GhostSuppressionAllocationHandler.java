package com.qidate.qisplan2.ghost.possession.manager;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.core.ModAttachments;
import com.qidate.qisplan2.ghost.possession.ability.GhostAbilityRegistry;
import com.qidate.qisplan2.ghost.possession.ability.PossessedGhostAbility;
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

        /*
         * 服务端最终验证：
         *
         * 来源鬼的灵异必须能够压制目标鬼。
         */
        PossessedGhostAbility sourceAbility =
                GhostAbilityRegistry.get(
                        sourceGhost
                );

        PossessedGhostAbility targetAbility =
                GhostAbilityRegistry.get(
                        targetGhost
                );

        if (
                sourceAbility == null
                        || targetAbility == null
                        || !sourceAbility.canSuppress(
                        targetAbility
                )
        ) {

            QisPlan2.LOGGER.info(
                    "[灵异配平] 服务端拒绝分配：{} 无法压制 {}",
                    sourceGhost,
                    targetGhost
            );

            return false;
        }

        if (
                isSlotAllocated(
                        player,
                        sourceGhost,
                        slotIndex
                )
        ) {
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
     * 清空玩家全部压制分配。
     *
     * <p>
     * 用于玩家死亡、重置驾驭状态等
     * 会导致全部压制关系失效的情况。
     */
    public static void clear(
            Player player
    ) {

        player.setData(
                ModAttachments.GHOST_SUPPRESSION_ALLOCATION,
                Map.of()
        );
    }

    /**
     * 删除所有涉及指定鬼的压制分配。
     *
     * <p>
     * 如果指定鬼是来源鬼，
     * 则删除该鬼发出的全部压制。
     *
     * <p>
     * 如果指定鬼是目标鬼，
     * 则删除所有指向该鬼的压制。
     *
     * @param player 玩家
     * @param ghost 被取消驾驭的鬼
     */
    public static void removeByGhost(
            Player player,
            ResourceLocation ghost
    ) {

        Map<
                ResourceLocation,
                Map<ResourceLocation, List<SuppressionAllocation>>
                > oldAllocations =
                getAllocations(player);

        /*
         * 没有任何分配时无需处理。
         */
        if (oldAllocations.isEmpty()) {
            return;
        }

        Map<
                ResourceLocation,
                Map<ResourceLocation, List<SuppressionAllocation>>
                > allocations =
                deepCopy(oldAllocations);

        /*
         * ========================================================
         * 删除该鬼作为来源鬼的全部压制
         * ========================================================
         */
        allocations.remove(ghost);

        /*
         * ========================================================
         * 删除该鬼作为目标鬼的全部压制
         * ========================================================
         */

        for (
                Map<ResourceLocation, List<SuppressionAllocation>>
                        targets
                : allocations.values()
        ) {

            targets.remove(ghost);
        }

        /*
         * ========================================================
         * 清理空容器
         * ========================================================
         */

        allocations.entrySet().removeIf(
                entry -> entry.getValue().isEmpty()
        );

        player.setData(
                ModAttachments.GHOST_SUPPRESSION_ALLOCATION,
                allocations
        );
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

        /*
         * 服务端最终验证：
         *
         * 来源鬼必须能够压制新的目标鬼。
         */
        PossessedGhostAbility sourceAbility =
                GhostAbilityRegistry.get(
                        sourceGhost
                );

        PossessedGhostAbility targetAbility =
                GhostAbilityRegistry.get(
                        newTargetGhost
                );

        if (
                sourceAbility == null
                        || targetAbility == null
                        || !sourceAbility.canSuppress(
                        targetAbility
                )
        ) {

            QisPlan2.LOGGER.info(
                    "[灵异配平] 服务端拒绝移动：{} 无法压制 {}",
                    sourceGhost,
                    newTargetGhost
            );

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
}