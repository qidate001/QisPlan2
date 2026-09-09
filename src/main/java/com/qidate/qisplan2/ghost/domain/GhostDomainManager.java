package com.qidate.qisplan2.ghost.domain;

import net.minecraft.server.level.ServerLevel;

import java.util.*;

public final class GhostDomainManager {

    private static final Map<ServerLevel, GhostDomainManager>
            MANAGERS = new WeakHashMap<>();


    private final ServerLevel level;

    private final Map<UUID, GhostDomain> domains =
            new LinkedHashMap<>();


    private GhostDomainManager(
            ServerLevel level
    ) {

        this.level = level;
    }


    /*
     * ============================================================
     * 获取管理器
     * ============================================================
     */

    public static GhostDomainManager get(
            ServerLevel level
    ) {

        return MANAGERS.computeIfAbsent(
                level,
                GhostDomainManager::new
        );
    }


    /*
     * ============================================================
     * 添加鬼域
     * ============================================================
     */

    public void add(
            GhostDomain domain
    ) {

        domains.put(
                domain.getId(),
                domain
        );
    }


    /*
     * ============================================================
     * 删除鬼域
     * ============================================================
     */

    public void remove(
            UUID id
    ) {

        domains.remove(id);
    }


    /*
     * ============================================================
     * 获取
     * ============================================================
     */

    public GhostDomain get(
            UUID id
    ) {

        return domains.get(id);
    }


    /*
     * ============================================================
     * 全部鬼域
     * ============================================================
     */

    public Collection<GhostDomain> getDomains() {

        return Collections.unmodifiableCollection(
                domains.values()
        );
    }


    /*
     * ============================================================
     * 判断实体是否在鬼域中
     * ============================================================
     */

    public boolean isInside(
            net.minecraft.world.entity.Entity entity
    ) {

        for (GhostDomain domain : domains.values()) {

            if (domain.contains(
                    level,
                    entity
            )) {

                return true;
            }
        }

        return false;
    }


    /*
     * ============================================================
     * 获取实体所在的全部鬼域
     * ============================================================
     */

    public List<GhostDomain> getDomainsAt(
            net.minecraft.world.entity.Entity entity
    ) {

        List<GhostDomain> result =
                new ArrayList<>();

        for (GhostDomain domain : domains.values()) {

            if (domain.contains(
                    level,
                    entity
            )) {

                result.add(domain);
            }
        }

        return result;
    }


    /*
     * ============================================================
     * Tick
     * ============================================================
     */

    public void tick() {

        /*
         * 现在先留空。
         *
         * 后面这里负责：
         *
         * 1. 更新移动中的鬼域
         * 2. 清理失效鬼域
         * 3. 同步客户端
         * 4. 触发进入/离开事件
         */
    }
}