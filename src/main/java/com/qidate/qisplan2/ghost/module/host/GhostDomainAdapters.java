package com.qidate.qisplan2.ghost.module.host;

import com.qidate.qisplan2.ghost.module.environment.GhostDomainAccess;
import com.qidate.qisplan2.ghost.domain.GhostDomain;
import com.qidate.qisplan2.ghost.domain.GhostDomainEntityTracker;
import com.qidate.qisplan2.ghost.domain.GhostDomainManager;
import com.qidate.qisplan2.ghost.domain.GhostDomainLayerHandler;
import com.qidate.qisplan2.ghost.domain.GhostDomainTeleportHandler;
import com.qidate.qisplan2.ghost.layer.GhostLayerHandler;
import com.qidate.qisplan2.ghost.reboot.GhostRebootManager;
import com.qidate.qisplan2.ghost.reboot.GhostRebootSources;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** 鬼域世界接口适配；不注册技能，也不按技能 ID 分支执行。 */
public final class GhostDomainAdapters {
    /** 工具类无需实例化。 */
    private GhostDomainAdapters() {}
    /** 将物理作用来源和模块范围封闭在接口内部。 */
    public static GhostDomainAccess bind(LivingEntity source, ResourceLocation moduleId) {
        if (source == null || !(source.level() instanceof ServerLevel level)) return GhostDomainAccess.NONE;
        return new GhostDomainAccess() {
            /** 有效覆盖集合考虑其他鬼域的竞争关系。 */
            public boolean contains(LivingEntity target) {
                var domain = current();
                return domain != null && target != null && target.level() == level
                        && GhostDomainEntityTracker.get(level).getEffectiveEntities(domain).contains(target);
            }
            /** 返回实际鬼域强度。 */
            public double intensity() { var domain = current(); return domain == null ? 0 : domain.getStrength(); }
            /** 每次重新取得运行时鬼域，避免保留已经移除的对象。 */
            private GhostDomain current() { return GhostDomainManager.get(level).getBySourceAndType(source.getUUID(), moduleId); }
            /** 仅查询当前模块的鬼域。 */
            public boolean exists() { return source.isAlive() && current() != null; }
            /** 返回当前总层数。 */
            public int layer() { var domain = current(); return domain == null ? 0 : domain.getLayer(); }
            /** 写入鬼域层数；接口不规定某个技能的增减步长。 */
            public boolean setLayer(int value) {
                var domain = current();
                if (domain == null || value < 1 || value > 10 || domain.getLayer() == value) return false;
                GhostDomainManager.get(level).updateLayer(domain.getId(), value);
                GhostLayerHandler.setLayer(source, value);
                return true;
            }
            /** 读取作用来源当前层数。 */
            public int occupiedLayer() { return GhostLayerHandler.getLayer(source); }
            /** 设置来源所在层数。 */
            public boolean setOccupiedLayer(int value) {
                if (!exists() || value < 1 || value > layer() || value == occupiedLayer()) return false;
                GhostLayerHandler.setLayer(source, value); return true;
            }
            /** 视线移动由具备输入的世界对象实现。 */
            public boolean teleportToAim() { return source instanceof ServerPlayer player && GhostDomainTeleportHandler.teleport(player, current()); }
            /** 调整当前鬼域实际覆盖的指向目标。 */
            public boolean shiftTargetLayer(int delta) { return source instanceof ServerPlayer player && GhostDomainLayerHandler.changeLayer(player, delta, current()); }
            /** 重启操作仍由统一重启系统实施，层数门槛归模块媒介定义。 */
            public boolean toggleReboot() {
                if (!(source instanceof ServerPlayer player) || !exists()) return false;
                GhostRebootManager.toggleContinuousReboot(player, GhostRebootSources.ghost(source.getUUID(), moduleId));
                return true;
            }
        };
    }
}
