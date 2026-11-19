package com.qidate.qisplan2.ghost.module.host.attachment;

import com.qidate.qisplan2.core.ModItems;
import com.qidate.qisplan2.ghost.module.GhostModuleContext;
import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemModuleHost;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import com.qidate.qisplan2.ghost.module.environment.GhostAttachmentAccess;
import com.qidate.qisplan2.ghost.module.suppression.GhostSuppression;
import com.qidate.qisplan2.ghost.module.suppression.GhostSuppressionState;
import com.qidate.qisplan2.network.GhostAttachmentNetwork;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 独立附着来源的持久化与关系索引。没有 tick 扫描；插拔、载入及数据接收时重建。
 * 缓存值不引用实体，弱键可释放卸载目标。压制结果按模块强度缓存，稳定后只读。
 */
public final class GhostAttachments {
    public static final String KEY = "QisPlan2Attachments";
    private static final Map<LivingEntity, Snapshot> CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    private record Cached(double strength, GhostSuppressionState state) {}
    private static final class Snapshot {
        List<ItemStack> items = List.of();
        final List<GhostSuppression> relations = new ArrayList<>();
        final Map<ResourceLocation, Cached> states = new HashMap<>();
        boolean absolute;
    }
    private GhostAttachments() {}

    /** 仅查询锚定实体本身承载的模块，不把装备或附着物误算为驾驭模块。 */
    private static List<GhostModuleData.Entry> modules(LivingEntity entity) {
        if (entity instanceof Player player) return new PlayerGhostModuleHost(player).getModules();
        return entity instanceof GhostModuleHost host ? host.getModules() : List.of();
    }
    /** 插入媒介要求目标确有厉鬼模块，并且物品仍在使用者手中、目标可接近。 */
    public static GhostAttachmentAccess bind(GhostModuleHost source) {
        if (source instanceof AttachedGhostHost) return new GhostAttachmentAccess() {
            /** 此来源仅在附着记录存在时被构造，插入媒介由容器生命周期保证。 */
            @Override public boolean embedded() { return true; }
        };
        return new GhostAttachmentAccess() {
            @Override public boolean canEmbed(LivingEntity target) {
                if (!(source instanceof GhostItemModuleHost item) || item.carrier() == null || target == null) return false;
                var user = item.carrier();
                return user.isAlive() && target.isAlive() && user != target && user.level() == target.level()
                        && user.distanceToSqr(target) <= 9 && user.hasLineOfSight(target)
                        && !(target instanceof Player player && (player.isCreative() || player.isSpectator()))
                        && (user.getMainHandItem() == item.stack() || user.getOffhandItem() == item.stack())
                        && !item.stack().isEmpty() && !modules(target).isEmpty() && !hasAttachments(target);
            }
            @Override public boolean embed(LivingEntity target) {
                if (!canEmbed(target) || target.level().isClientSide()) return false;
                var item = (GhostItemModuleHost) source;
                // 插入是原物品的转移，创造模式也一样；拔出时无条件返还保存的原物品。
                attach(target, item.stack().copyWithCount(1), true);
                item.stack().shrink(1);
                return true;
            }
        };
    }
    /** 建立唯一附着实例标识，保存完整物品组件。初版交互仅允许一个插入槽位。 */
    private static void attach(LivingEntity target, ItemStack stack, boolean recoverable) {
        var list = target.getPersistentData().getList(KEY, Tag.TAG_COMPOUND).copy();
        var entry = new GhostAttachmentRecord(UUID.randomUUID(),
                GhostAttachedItemSnapshot.save(stack, target.registryAccess()), recoverable);
        list.add(entry.save());
        target.getPersistentData().put(KEY, list);
        changed(target);
        if (target instanceof Mob mob) { mob.getNavigation().stop(); mob.setTarget(null); mob.setAggressive(false); }
    }
    /** 判断的是附着容器，而不是旧棺材钉布尔量。 */
    public static boolean hasAttachments(LivingEntity target) {
        return !target.getPersistentData().getList(KEY, Tag.TAG_COMPOUND).isEmpty();
    }
    /** 返回仅供渲染读取的缓存快照；渲染器不得修改物品栈。 */
    public static List<ItemStack> visualItems(LivingEntity target) {
        return CACHE.computeIfAbsent(target, GhostAttachments::build).items;
    }
    /** 只在关系缓存失效时解码一次，返回的渲染快照仅供读取。 */
    private static List<ItemStack> decodeItems(LivingEntity target) {
        var result = new ArrayList<ItemStack>();
        var list = target.getPersistentData().getList(KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            var record = GhostAttachmentRecord.load(list.getCompound(i));
            if (record.isEmpty()) continue;
            var stack = GhostAttachedItemSnapshot.restore(record.get().item(), target.registryAccess());
            if (!stack.isEmpty()) result.add(stack);
        }
        return result;
    }
    /** 拔出只移除附着关系，不清除沉睡、死机、其他来源状态，也不制造全新的物品。 */
    public static List<ItemStack> detachAll(LivingEntity target) {
        if (!hasAttachments(target)) return List.of();
        var result = new ArrayList<ItemStack>();
        var list = target.getPersistentData().getList(KEY, Tag.TAG_COMPOUND);
        var unreadable = new ListTag();
        for (int i = 0; i < list.size(); i++) {
            var record = GhostAttachmentRecord.load(list.getCompound(i));
            var stack = record.isPresent()
                    ? GhostAttachedItemSnapshot.restore(record.get().item(), target.registryAccess()) : ItemStack.EMPTY;
            if (!stack.isEmpty()) result.add(stack);
            else unreadable.add(list.getCompound(i).copy()); // 不能因为解析失败而删除原物品数据。
        }
        if (unreadable.isEmpty()) target.getPersistentData().remove(KEY);
        else target.getPersistentData().put(KEY, unreadable);
        changed(target);
        return result;
    }
    /** 将拔出的原物品交给操作者；背包满时落地，物品种类和组件均来自快照。 */
    public static void recover(LivingEntity target, Player player) {
        for (var stack : detachAll(target)) if (!player.getInventory().add(stack)) player.drop(stack, false);
    }
    /** 查询固定关系的缓存结果；无解压制不需要读取目标强度或遍历关系。 */
    public static GhostSuppressionState suppression(LivingEntity target, ResourceLocation module) {
        var snapshot = CACHE.computeIfAbsent(target, GhostAttachments::build);
        if (snapshot.absolute) return GhostSuppressionState.EXTERNAL_COMPLETE;
        if (snapshot.relations.isEmpty()) return GhostSuppressionState.NONE;
        double strength = modules(target).stream().filter(e -> e.id().equals(module)).mapToDouble(GhostModuleData.Entry::intensity).findFirst().orElse(0);
        var cached = snapshot.states.get(module);
        if (cached != null && cached.strength() == strength) return cached.state();
        var level = GhostSuppression.Level.NONE;
        for (var relation : snapshot.relations) if (relation.level(strength).ordinal() > level.ordinal()) level = relation.level(strength);
        var state = new GhostSuppressionState(level, GhostSuppression.Level.NONE,
                snapshot.relations.stream().mapToDouble(GhostSuppression::strength).max().orElse(0));
        snapshot.states.put(module, new Cached(strength, state));
        return state;
    }
    /** 附着媒介已成立，重建持续声明，不重复释放能力或扣除代价。 */
    private static Snapshot build(LivingEntity target) {
        var snapshot = new Snapshot();
        snapshot.items = List.copyOf(decodeItems(target));
        for (var stack : snapshot.items) {
            var host = new AttachedGhostHost(stack);
            for (var entry : host.getModules()) {
                var definition = GhostModuleRegistry.get(entry.id());
                if (definition != null && host.carriesModule(entry.id()))
                    snapshot.relations.addAll(definition.getSuppressions(new GhostModuleContext(host, entry.id())));
            }
        }
        snapshot.absolute = snapshot.relations.stream().anyMatch(r -> r.kind() == GhostSuppression.Kind.ABSOLUTE);
        return snapshot;
    }
    /** 状态变化时重建一次，并向正在跟踪目标的客户端同步。 */
    public static void changed(LivingEntity target) {
        CACHE.remove(target);
        if (!target.level().isClientSide()) GhostAttachmentNetwork.sync(target);
    }
    /** 客户端接收服务端快照；只刷新缓存，不允许客户端反向创建压制。 */
    public static void receive(LivingEntity target, CompoundTag tag) {
        target.getPersistentData().put(KEY, tag.getList(KEY, Tag.TAG_COMPOUND).copy());
        CACHE.remove(target);
    }
    /** 仅同步独立附着容器，不暴露目标其他持久数据。 */
    public static CompoundTag snapshot(LivingEntity target) {
        var tag = new CompoundTag();
        tag.put(KEY, target.getPersistentData().getList(KEY, Tag.TAG_COMPOUND).copy());
        return tag;
    }
    /** 旧存档布尔量迁移一次；原模块状态完全不动，新记录随后按正常物品持久化。 */
    public static void loaded(LivingEntity target) {
        CACHE.remove(target);
        if (!target.level().isClientSide()) {
            var list = target.getPersistentData().getList(KEY, Tag.TAG_COMPOUND);
            boolean updated = false;
            for (int i = 0; i < list.size(); i++) {
                var record = GhostAttachmentRecord.load(list.getCompound(i));
                if (record.isEmpty()) continue;
                var old = record.get();
                if (GhostAttachedItemSnapshot.isComplete(old.item()) && list.getCompound(i).getBoolean("recoverable")) continue;
                var stack = GhostAttachedItemSnapshot.restore(old.item(), target.registryAccess());
                if (stack.isEmpty()) continue;
                list.set(i, new GhostAttachmentRecord(old.instance(),
                        GhostAttachedItemSnapshot.save(stack, target.registryAccess()), true).save());
                updated = true;
            }
            if (updated) target.getPersistentData().put(KEY, list);
        }
        if (!target.level().isClientSide() && target.getPersistentData().getBoolean("QisPlan2CoffinNailed")) {
            target.getPersistentData().remove("QisPlan2CoffinNailed");
            if (!hasAttachments(target)) attach(target, new ItemStack(ModItems.COFFIN_NAIL.get()), true);
        }
    }
    /** 卸载只释放运行时索引，不拔钉、不强制加载目标。 */
    public static void unloaded(LivingEntity target) { CACHE.remove(target); }
}
