package com.qidate.qisplan2.ghost.module;

import net.minecraft.world.entity.LivingEntity;

/**
 * 灵异刻字能力。
 *
 * 宿主可以提供此能力，让灵异模块请求执行刻字操作。
 * 接口不依赖具体的方块、物品或实体类型。
 */
public interface GhostInscriptionCapability {

    /**
     * 请求为指定使用者打开刻字界面。
     *
     * @param user 发起刻字操作的实体
     * @return 是否接受了此次请求
     */
    boolean requestInscription(LivingEntity user);
}