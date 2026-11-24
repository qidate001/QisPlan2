package com.qidate.qisplan2.ghost.module.host;

import com.qidate.qisplan2.core.ModRecipes;
import com.qidate.qisplan2.ghost.module.data.GhostWorkstationData;
import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.GhostModuleCustomDataModifier;
import com.qidate.qisplan2.ghost.module.host.item.GhostItemModuleHost;
import com.qidate.qisplan2.menu.GhostStoveMenu;
import com.qidate.qisplan2.recipe.GhostStoveInput;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import java.lang.ref.WeakReference;
import java.util.*;

/** 五输入一输出的配方工作台服务。实际库存和进度以模块 customData 为唯一持久数据源。 */
public final class FiveSlotCookingWorkstation extends SimpleContainer implements GhostWorkstationAdapters.Session {
    public static final String DATA = GhostWorkstationData.KEY;
    // 键和值都不强持有载体；打开的菜单强持有会话，关闭后允许回收并从模块数据重建。
    private static final Map<Object, Map<ResourceLocation, WeakReference<FiveSlotCookingWorkstation>>> CACHE = new WeakHashMap<>();
    private final GhostModuleHost host;
    private final ResourceLocation module;
    private final ServerLevel level;
    private final GhostModuleCustomDataModifier storage;
    private CompoundTag snapshot = new CompoundTag();
    private List<ItemStack> previousInputs = List.of();
    private int progress;
    private int duration = 200;
    private String recipeId = "";
    private long lastTick = Long.MIN_VALUE;
    private boolean loading;
    private final ContainerData data = new ContainerData() {
        /** 客户端进度条只同步两个整数，不携带服务端配方对象。 */
        public int get(int index) { return index == 0 ? progress : index == 1 ? duration : 0; }
        /** 服务端只读；客户端使用自己的 SimpleContainerData。 */
        public void set(int index,int value) {}
        /** 同步当前进度与总时长。 */
        public int getCount() { return 2; }
    };
    /** 读取存档初始化会话；创建期间禁止 setChanged 回写空库存。 */
    private FiveSlotCookingWorkstation(GhostModuleHost host, ResourceLocation module, ServerLevel level) {
        super(6); this.host = host; this.module = module; this.level = level;
        this.storage = (GhostModuleCustomDataModifier)host; refresh();
    }
    /** 在适配边界解析世界与会话身份，不依赖特定厉鬼实体。 */
    public static FiveSlotCookingWorkstation obtain(GhostModuleHost host, ResourceLocation module) {
        if (!(host instanceof GhostModuleCustomDataModifier)) return null;
        var entity = GhostEnvironmentAdapters.bearer(host);
        var world = entity != null ? entity.level() : host instanceof BlockEntity block ? block.getLevel() : null;
        if (!(world instanceof ServerLevel server)) return null;
        Object key = host instanceof GhostItemModuleHost item ? item.stack() : entity != null ? entity : host;
        var sessions = CACHE.computeIfAbsent(key, ignored -> new HashMap<>());
        var reference = sessions.get(module); var session = reference == null ? null : reference.get();
        if (session == null || session.level != server) {
            session = new FiveSlotCookingWorkstation(host,module,server); sessions.put(module,new WeakReference<>(session));
        } else session.refresh();
        return session;
    }
    /** 模块被移除、物品收起或来源离开当前维度后，旧菜单失效。 */
    private boolean available() {
        if (!host.carriesModule(module) || !GhostEnvironmentAdapters.bind(host,module).exposed()) return false;
        var bearer = GhostEnvironmentAdapters.bearer(host);
        return bearer != null ? bearer.isAlive() && bearer.level() == level
                : host instanceof BlockEntity block && !block.isRemoved() && block.getLevel() == level;
    }
    /** 距离约束对方块、实体及物品携带者一视同仁。 */
    public boolean canUse(ServerPlayer player) {
        if (!available() || !player.isAlive() || player.isSpectator() || player.level() != level) return false;
        var entity = GhostEnvironmentAdapters.bearer(host);
        Vec3 pos = entity != null ? entity.position() : ((BlockEntity)host).getBlockPos().getCenter();
        return player.distanceToSqr(pos) <= 64;
    }
    /** 菜单每次交互由原版重复调用有效性检查。 */
    public boolean stillValid(Player player) { return player instanceof ServerPlayer server && canUse(server); }
    /** 使用已有翻译键。 */
    public Component getDisplayName() { return Component.translatable("container.qisplan2.ghost_stove"); }
    /** 同一个菜单支持任意模块载体提供的 Container。 */
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new GhostStoveMenu(id,inventory,this,data);
    }
    /** 输入快照用于检查外部槽位编辑，不把输出取走误判为更换配方。 */
    private List<ItemStack> inputs() {
        var result = new ArrayList<ItemStack>(); for (int i=0;i<5;i++) result.add(getItem(i).copy()); return result;
    }
    /** 外部命令或模块转移改变了持久数据时刷新，避免缓存覆盖较新的数据。 */
    private void refresh() {
        var saved = storage.getModuleCustomData(module).getCompound(DATA);
        if (saved.equals(snapshot) && !previousInputs.isEmpty()) return;
        loading = true;
        try {
            var items = NonNullList.withSize(6,ItemStack.EMPTY); ContainerHelper.loadAllItems(saved,items,level.registryAccess());
            for (int i=0;i<6;i++) super.setItem(i,items.get(i));
            progress = Math.max(0,saved.getInt("CookTime")); duration = Math.max(1,saved.contains("CookTimeTotal") ? saved.getInt("CookTimeTotal") : 200);
            recipeId = saved.getString("Recipe"); previousInputs = inputs(); snapshot = saved.copy();
        } finally { loading = false; }
    }
    /** 每次库存变更立即落到模块，关闭菜单或转移载体都不会丢失物品。 */
    @Override public void setChanged() {
        super.setChanged();
        if (loading || storage == null || !host.hasModule(module)) return;
        var current = inputs();
        boolean changed = previousInputs.size() != current.size();
        for (int i=0;!changed && i<5;i++) changed = !ItemStack.matches(previousInputs.get(i),current.get(i));
        if (changed) { progress = 0; recipeId = ""; }
        previousInputs = current; save();
    }
    /** 不通知型槽位移除同样需要持久化，以免重新打开后物品复现。 */
    @Override public ItemStack removeItemNoUpdate(int slot) { var result = super.removeItemNoUpdate(slot); setChanged(); return result; }
    /** 保存时合并模块其他数据，避免覆盖死机或后续增加的能力状态。 */
    private void save() {
        var saved = new CompoundTag(); var items = NonNullList.withSize(6,ItemStack.EMPTY);
        for (int i=0;i<6;i++) items.set(i,getItem(i));
        ContainerHelper.saveAllItems(saved,items,level.registryAccess());
        saved.putInt("CookTime",progress); saved.putInt("CookTimeTotal",duration); saved.putString("Recipe",recipeId);
        if (saved.equals(snapshot)) return;
        var custom = storage.getModuleCustomData(module); custom.put(DATA,saved);
        if (storage.setModuleCustomData(module,custom)) snapshot = saved.copy();
    }
    /** 无配方或输出堵塞时复位，只有状态改变才写存档。 */
    private void reset() { if (progress != 0 || !recipeId.isEmpty()) { progress=0; recipeId=""; save(); } }
    /** 每个世界 tick 最多制作一次；配方、数量、组件与输出容量全部在服务端验证。 */
    public void tick() {
        if (!available() || lastTick == level.getGameTime()) return;
        lastTick = level.getGameTime(); refresh();
        var input = new GhostStoveInput(inputs());
        var match = level.getRecipeManager().getRecipeFor(ModRecipes.GHOST_STOVE_RECIPE_TYPE.get(),input,level);
        if (match.isEmpty()) { reset(); return; }
        var holder = match.get(); var recipe = holder.value(); var result = recipe.assemble(input,level.registryAccess());
        var output = getItem(5);
        if (result.isEmpty() || result.getCount() > Math.min(result.getMaxStackSize(),getMaxStackSize())
                || !output.isEmpty() && (!ItemStack.isSameItemSameComponents(output,result)
                || output.getCount()+result.getCount() > Math.min(output.getMaxStackSize(),getMaxStackSize()))) { reset(); return; }
        if (!recipeId.isEmpty() && !recipeId.equals(holder.id().toString())) progress=0;
        recipeId=holder.id().toString(); duration=Math.max(1,recipe.getCookingTime()); progress++;
        if (progress >= duration) {
            loading=true;
            try {
                for (int i=0;i<5;i++) {
                    var ingredient=recipe.getSlotIngredients().get(i);
                    if (!ingredient.isEmpty()) getItem(i).shrink(ingredient.count());
                }
                if (output.isEmpty()) super.setItem(5,result.copy()); else output.grow(result.getCount());
            } finally { loading=false; }
            progress=0; previousInputs=inputs();
        }
        save();
    }
}
