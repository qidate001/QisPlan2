package com.qidate.qisplan2.ghost.isolation;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.network.ghostdomain.GhostDomainNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

/**
 * 灵异隔绝系统。
 *
 * <p>
 * 这是整个灵异隔绝机制的统一入口。
 *
 * <p>
 * 其他灵异系统、鬼以及相关机制，
 * 不应该直接调用 {@link GhostIsolationDetector}，
 * 而应该通过本类查询指定位置是否处于
 * 灵异隔绝空间。
 *
 * <p>
 * 当前版本负责：
 *
 * <ul>
 *     <li>查询已经缓存的灵异隔绝区域</li>
 *     <li>命中有效缓存时直接返回结果</li>
 *     <li>缓存失效时重新进行空间检测</li>
 *     <li>检测成功后建立新的 Region</li>
 *     <li>将确认过的 Region 保存到世界存档</li>
 * </ul>
 *
 * <p>
 * 当前版本暂不包含 Chunk Index。
 * Region 数量较少时直接遍历 SavedData 中的 Region。
 * 后续再加入 Chunk → Region 索引进行优化。
 */
public final class GhostIsolationSystem {

    private GhostIsolationSystem() {
    }

    /**
     * 判断指定位置是否处于灵异隔绝空间。
     *
     * <p>
     * 这是其他灵异系统应该使用的主要 API。
     *
     * @param level 世界
     * @param pos   要检测的位置
     * @return 如果当前位置已经确认处于灵异隔绝空间，则返回 true
     */
    public static boolean isIsolated(
            ServerLevel level,
            BlockPos pos
    ) {

        return query(
                level,
                pos
        ).isIsolated();
    }

    /**
     * 查询指定位置的灵异隔绝状态。
     *
     * <p>
     * 与 {@link #isIsolated(ServerLevel, BlockPos)} 不同，
     * 本方法还会返回本次查询是否命中了缓存，
     * 方便调试系统运行状态。
     */
    public static QueryResult query(
            ServerLevel level,
            BlockPos pos
    ) {

        GhostIsolationSavedData data =
                GhostIsolationSavedData.get(level);

        /*
         * ========================================================
         * 第一步：查询已经缓存的 Region
         * ========================================================
         */
        for (GhostIsolationRegion region :
                data.getRegions()) {

            if (!region.containsExact(
                    level.dimension(),
                    pos
            )) {
                continue;
            }

            /*
             * ====================================================
             * 已经确认隔绝。
             *
             * 直接命中缓存。
             * ====================================================
             */
            if (region.getState() ==
                    IsolationState.ISOLATED) {

                return new QueryResult(
                        IsolationState.ISOLATED,
                        true,
                        false,
                        false
                );
            }

            /*
             * ====================================================
             * Region 已经失效。
             *
             * 重新检测。
             * ====================================================
             */
            if (region.getState() ==
                    IsolationState.DIRTY) {

                boolean isolated =
                        recheckRegion(
                                level,
                                region,
                                data
                        );

                return new QueryResult(
                        isolated
                                ? IsolationState.ISOLATED
                                : IsolationState.UNKNOWN,
                        false,
                        true,
                        isolated
                );
            }

            /*
             * ====================================================
             * OPEN / UNKNOWN
             *
             * 当前版本通常不会把这两种状态保存下来。
             * ====================================================
             */
            return new QueryResult(
                    region.getState(),
                    true,
                    false,
                    false
            );
        }

        /*
         * ========================================================
         * 第二步：没有命中任何缓存 Region。
         *
         * 进行一次新的空间检测。
         * ========================================================
         */
        GhostIsolationDetector.DetectionResult result =
                GhostIsolationDetector.detect(
                        level,
                        pos
                );

        /*
         * UNKNOWN 不进行缓存。
         */
        if (!result.isIsolated()) {

            return new QueryResult(
                    result.state(),
                    false,
                    false,
                    false
            );
        }

        /*
         * ========================================================
         * 第三步：确认隔绝。
         *
         * 建立新的 Region。
         * ========================================================
         */
        GhostIsolationRegion region =
                new GhostIsolationRegion(
                        UUID.randomUUID(),
                        level.dimension(),
                        result.min(),
                        result.max(),
                        result.seed(),
                        result.cuboids(),
                        IsolationState.ISOLATED
                );

        /*
         * ========================================================
         * 保存新的 Region。
         * ========================================================
         */
        data.addRegion(
                region
        );

        /*
         * ========================================================
         * 将新建立的 Region 同步到客户端。
         * ========================================================
         */
        GhostDomainNetwork.sendIsolationAdd(
                level,
                region
        );

        return new QueryResult(
                IsolationState.ISOLATED,
                false,
                false,
                true
        );
    }

    /**
     * 重新检测一个已经失效的 Region。
     *
     * <p>
     * 如果重新检测成功，则更新 Region 的范围和状态，
     * 并同步新的空间数据到客户端。
     * </p>
     *
     * <p>
     * 如果检测失败，则移除这个已经失效的 Region，
     * 同时通知客户端删除对应缓存。
     * </p>
     *
     * @param level 当前服务端维度
     * @param region 需要重新检测的 Region
     * @param data 灵异隔绝 SavedData
     * @return 如果重新确认空间仍然隔绝则返回 true
     */
    private static boolean recheckRegion(
            ServerLevel level,
            GhostIsolationRegion region,
            GhostIsolationSavedData data
    ) {

        GhostIsolationDetector.DetectionResult result =
                GhostIsolationDetector.detect(
                        level,
                        region.getSeed()
                );

        /*
         * ========================================================
         * 重新确认隔绝
         * ========================================================
         */
        if (result.isIsolated()) {

            /*
             * 目前 GhostIsolationRegion 的边界字段是 final，
             * 因此重新检测后直接使用新的 Region 替换旧 Region。
             *
             * UUID 保持不变。
             */
            GhostIsolationRegion updatedRegion =
                    new GhostIsolationRegion(
                            region.getId(),
                            level.dimension(),
                            result.min(),
                            result.max(),
                            result.seed(),
                            result.cuboids(),
                            IsolationState.ISOLATED
                    );

            /*
             * ====================================================
             * 客户端先删除旧的空间数据。
             *
             * 旧 Region 的 Cuboid 可能已经发生变化。
             * ====================================================
             */
            GhostDomainNetwork.sendIsolationRemove(
                    level,
                    region.getId()
            );

            /*
             * ====================================================
             * 保存重新检测后的 Region。
             * ====================================================
             */
            data.addRegion(
                    updatedRegion
            );

            /*
             * ====================================================
             * 将新的空间数据同步给客户端。
             * ====================================================
             */
            GhostDomainNetwork.sendIsolationAdd(
                    level,
                    updatedRegion
            );

            return true;
        }

        /*
         * ========================================================
         * 已经无法确认原来的空间仍然成立。
         *
         * 删除旧缓存。
         * ========================================================
         */
        data.removeRegion(
                region.getId()
        );

        /*
         * ========================================================
         * 同时通知客户端移除旧的 Region。
         * ========================================================
         */
        GhostDomainNetwork.sendIsolationRemove(
                level,
                region.getId()
        );

        /*
         * 下一次查询时，
         * 如果当前位置仍然处于封闭空间，
         * 系统会重新建立 Region。
         */
        return false;
    }

    /**
     * 通知灵异隔绝系统：
     * 某个位置的方块状态发生了变化。
     *
     * <p>
     * 本方法只负责处理已经被
     * GhostIsolationBlockRegistry 判定为
     * “可能影响灵异隔绝”的方块变化。
     */
    public static void onBlockChanged(
            ServerLevel level,
            BlockPos pos
    ) {

        GhostIsolationSavedData data =
                GhostIsolationSavedData.get(level);

        for (GhostIsolationRegion region :
                data.getRegions()) {

            /*
             * 只有已经确认的 Region
             * 才需要进行失效处理。
             */
            if (region.getState() !=
                    IsolationState.ISOLATED) {
                continue;
            }

            /*
             * 当前变化位置如果不靠近
             * Region 的边界，
             * 那么它不会影响这个空间
             * 是否与外界连通。
             */
            if (!isNearRegionBoundary(
                    region,
                    pos
            )) {
                continue;
            }

            /*
             * 标记为失效。
             *
             * 这里不立即重新检测。
             */
            region.setState(
                    IsolationState.DIRTY
            );

            data.setDirty();

            QisPlan2.LOGGER.info(
                    "[灵异隔绝检测] Region {} 因方块变化而标记为 DIRTY，位置：{}",
                    region.getId(),
                    pos
            );
        }
    }

    private static boolean isNearRegionBoundary(
            GhostIsolationRegion region,
            BlockPos pos
    ) {

        return pos.getX() >=
                region.getMin().getX() - 1
                && pos.getX() <=
                region.getMax().getX() + 1

                && pos.getY() >=
                region.getMin().getY() - 1
                && pos.getY() <=
                region.getMax().getY() + 1

                && pos.getZ() >=
                region.getMin().getZ() - 1
                && pos.getZ() <=
                region.getMax().getZ() + 1;
    }

    /**
     * 一次性处理大型结构生成造成的空间变化。
     */
    public static void onBatchChanged(
            ServerLevel level,
            int minChunkX,
            int maxChunkX,
            int minChunkZ,
            int maxChunkZ
    ) {

        GhostIsolationSavedData data =
                GhostIsolationSavedData.get(level);

        int minBlockX =
                minChunkX << 4;

        int maxBlockX =
                (maxChunkX << 4) + 15;

        int minBlockZ =
                minChunkZ << 4;

        int maxBlockZ =
                (maxChunkZ << 4) + 15;

        for (GhostIsolationRegion region :
                data.getRegions()) {

            if (region.getState()
                    != IsolationState.ISOLATED) {

                continue;
            }

            /*
             * Region 的粗略 AABB 与 Batch 范围没有交集。
             */
            if (region.getMax().getX() < minBlockX
                    || region.getMin().getX() > maxBlockX
                    || region.getMax().getZ() < minBlockZ
                    || region.getMin().getZ() > maxBlockZ) {

                continue;
            }

            region.setState(
                    IsolationState.DIRTY
            );

            QisPlan2.LOGGER.info(
                    "[灵异隔绝检测] Region {} 因批量方块变化而标记为 DIRTY，Batch Chunk：X {}~{}，Z {}~{}",
                    region.getId(),
                    minChunkX,
                    maxChunkX,
                    minChunkZ,
                    maxChunkZ
            );
        }

        data.setDirty();
    }

    /**
     * 灵异隔绝系统的一次查询结果。
     *
     * @param state      最终得到的隔绝状态
     * @param cacheHit   是否直接命中了已有缓存
     * @param rechecked  是否因为缓存失效而重新检测
     * @param regionCreated 是否因为本次查询新建了 Region
     */
    public record QueryResult(
            IsolationState state,
            boolean cacheHit,
            boolean rechecked,
            boolean regionCreated
    ) {

        /**
         * 判断最终是否处于灵异隔绝空间。
         */
        public boolean isIsolated() {

            return state ==
                    IsolationState.ISOLATED;
        }
    }
}