package com.qidate.qisplan2.client;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(
        modid = QisPlan2.MODID,
        value = Dist.CLIENT
)
public final class DoorGhostMarkClient {

    /**
     * 当前这个客户端需要显示高亮的实体 ID。
     */
    private static final Set<Integer> MARKED =
            new HashSet<>();

    private DoorGhostMarkClient() {
    }

    /**
     * 服务端告诉客户端：
     *
     * 某个实体开始 / 停止被开关门鬼标记。
     */
    public static void apply(
            int entityId,
            boolean marked
    ) {

        if (marked) {

            MARKED.add(
                    entityId
            );

            refreshEntity(
                    entityId
            );

        } else {

            remove(
                    entityId
            );
        }
    }

    /**
     * 客户端 Tick。
     *
     * 持续维持本地 Glowing。
     */
    @SubscribeEvent
    public static void onClientTick(
            ClientTickEvent.Post event
    ) {

        tick();
    }

    /**
     * 每 tick 重新维持本地 Glowing。
     */
    private static void tick() {

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        /*
         * 使用副本，避免遍历过程中发生修改。
         */
        for (Integer entityId :
                Set.copyOf(MARKED)) {

            refreshEntity(
                    entityId
            );
        }
    }

    /**
     * 刷新一个实体的本地高亮状态。
     */
    private static void refreshEntity(
            int entityId
    ) {

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        Entity entity =
                minecraft.level.getEntity(
                        entityId
                );

        if (entity == null) {
            return;
        }

        entity.setGlowingTag(
                true
        );
    }

    /**
     * 清除一个实体的标记。
     */
    public static void remove(
            int entityId
    ) {

        MARKED.remove(
                entityId
        );

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        Entity entity =
                minecraft.level.getEntity(
                        entityId
                );

        if (entity != null) {

            entity.setGlowingTag(
                    false
            );
        }
    }

    /**
     * 清空全部标记。
     */
    public static void clear() {

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level != null) {

            for (Integer entityId :
                    Set.copyOf(MARKED)) {

                Entity entity =
                        minecraft.level.getEntity(
                                entityId
                        );

                if (entity != null) {

                    entity.setGlowingTag(
                            false
                    );
                }
            }
        }

        MARKED.clear();
    }
}