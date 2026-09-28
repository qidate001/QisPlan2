package com.qidate.qisplan2.ghost.curse.type;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.curse.Curse;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/**
 * 诅咒系统测试用诅咒。
 *
 * 仅用于验证：
 *
 * 注册
 *  ↓
 * Tick
 *  ↓
 * 自动失效
 *  ↓
 * 注销
 */
public class TestCurse implements Curse {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "ghost_tombstone"
            );

    /*
     * 这个诅咒实例自己的 ID。
     */
    private final UUID id;

    /*
     * 被诅咒的玩家。
     */
    private final UUID target;

    /*
     * 已经运行了多少 Tick。
     */
    private int ticks;

    /*
     * 测试持续时间。
     *
     * 这里先设置 100 Tick，也就是 5 秒。
     */
    private static final int MAX_TICKS = 100;

    public TestCurse(
            UUID target
    ) {

        this.id =
                UUID.randomUUID();

        this.target =
                target;
    }

    @Override
    public UUID getId() {

        return id;
    }

    @Override
    public UUID getTarget() {

        return target;
    }

    @Override
    public void tick(
            MinecraftServer server
    ) {

        ticks++;

        /*
         * 每 20 Tick 打印一次，
         * 避免刷屏。
         */
        if (ticks % 20 == 0) {

            com.qidate.qisplan2.QisPlan2.LOGGER.info(
                    "[诅咒测试] Curse {} 已运行 {} Tick，目标 {}",
                    id,
                    ticks,
                    target
            );
        }
    }

    @Override
    public boolean isValid(
            MinecraftServer server
    ) {

        return ticks < MAX_TICKS;
    }
}