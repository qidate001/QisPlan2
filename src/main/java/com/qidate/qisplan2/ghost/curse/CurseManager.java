package com.qidate.qisplan2.ghost.curse;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * 诅咒实例管理器。
 *
 * 负责：
 *
 * 1. 注册诅咒
 * 2. 注销诅咒
 * 3. 查询诅咒
 * 4. 每 Tick 执行诅咒
 * 5. 自动清理失效诅咒
 */
public final class CurseManager {

    /*
     * 所有正在运行中的诅咒。
     *
     * Key：
     *     Curse 实例 UUID
     *
     * Value：
     *     Curse 实例
     */
    private static final java.util.Map<UUID, Curse> CURSES =
            new java.util.HashMap<>();

    private CurseManager() {
    }

    /**
     * 注册一个诅咒。
     *
     * 如果已经存在相同 ID，
     * 则覆盖原来的实例。
     */
    public static void add(
            Curse curse
    ) {
        CURSES.put(
                curse.getId(),
                curse
        );

        QisPlan2.LOGGER.info(
                "[诅咒系统] 注册诅咒：{}，目标：{}",
                curse.getId(),
                curse.getTarget()
        );
    }

    /**
     * 注销一个诅咒。
     */
    public static void remove(
            UUID curseId
    ) {
        CURSES.remove(curseId);

        QisPlan2.LOGGER.info(
                "[诅咒系统] 注销诅咒：{}",
                curseId
        );
    }

    /**
     * 根据 ID 获取诅咒。
     */
    public static Curse get(
            UUID curseId
    ) {

        return CURSES.get(curseId);
    }

    /**
     * 获取当前所有诅咒。
     *
     * 返回副本，
     * 防止外部直接修改 Manager 内部数据。
     */
    public static Collection<Curse> getAll() {

        return List.copyOf(
                CURSES.values()
        );
    }

    /**
     * 获取某个玩家当前受到的所有诅咒。
     */
    public static List<Curse> getByTarget(
            UUID target
    ) {

        List<Curse> result =
                new ArrayList<>();

        for (Curse curse : CURSES.values()) {

            if (curse.getTarget().equals(target)) {

                result.add(curse);
            }
        }

        return result;
    }

    /**
     * 每个服务器 Tick 调用一次。
     */
    public static void tick(
            MinecraftServer server
    ) {

        /*
         * 使用副本遍历。
         *
         * 因为 tick() 或清理过程中，
         * 可能会修改 CURSES。
         */
        List<Curse> curses =
                new ArrayList<>(
                        CURSES.values()
                );

        for (Curse curse : curses) {

            /*
             * 诅咒已经失效。
             */
            if (!curse.isValid(server)) {

                remove(
                        curse.getId()
                );

                continue;
            }

            /*
             * 执行诅咒逻辑。
             */
            curse.tick(server);
        }
    }

    /**
     * 清空所有诅咒。
     *
     * 主要用于服务器关闭或重新初始化时。
     */
    public static void clear() {

        CURSES.clear();
    }
}