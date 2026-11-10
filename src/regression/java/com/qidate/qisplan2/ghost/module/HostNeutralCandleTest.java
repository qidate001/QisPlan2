package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.environment.*;
import com.qidate.qisplan2.ghost.module.event.ModuleTickEvent;
import com.qidate.qisplan2.ghost.module.implementation.candle.WhiteGhostCandleModule;
import java.util.List;
import java.lang.reflect.Proxy;
import com.qidate.qisplan2.ghost.module.implementation.nightwanderer.NightWandererModule;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** 执行真实规则和媒介，证明相同环境下不依赖载体类，且未暴露不会产生能力效果。 */
public final class HostNeutralCandleTest {
    private static final ResourceLocation ID = WhiteGhostCandleModule.ID;
    private static final class Environment implements GhostEnvironment {
        boolean exposed = true;
        int emissions;
        double consumed;

        /**
         * 判断当前模块是否满足暴露在世界中的条件。
         * 具体的手持、存活或方块有效性由载体适配器解释。
         */
        public boolean exposed() { return exposed; }

        /**
         * 获取当前环境造成的基础燃烧压力，单位为每 tick 的消耗量。
         * 此值不包含声音屏蔽额外消耗。
         */
        public double burnPressure() { return 0.05; }

        /**
         * 读取被屏蔽的灵异声音强度，供模块计算额外燃烧代价。
         * 没有可计入的声音压力时返回零。
         */
        public double blockedSoundIntensity() { return 50; }

        /**
         * 扣除本次能力消耗的燃料。
         * 适配器负责把连续消耗量转换为耐久或持久燃料，模块不处理物品存储细节。
         */
        public void consume(double amount) { consumed += amount; }

        /**
         * 发布本次能力产生的环境效果。
         * 世界系统读取效果时仍需检查模块状态和暴露条件。
         */
        public void emit(GhostEnvironmentalEffect effect) {
            if (effect != GhostEnvironmentalEffect.CONCEALMENT) throw new AssertionError("白烛效果错误");
            emissions++;
        }
    }
    private interface Storage extends GhostModuleHost, GhostModuleCustomDataModifier {
        Environment environment();
        CompoundTag data();

        /**
         * 读取当前载体保存的模块实例列表。
         */
        default List<GhostModuleData.Entry> getModules() {
            return List.of(new GhostModuleData.Entry(ID, 5, 0, false, false, data()));
        }

        /**
         * 返回指定模块的专属数据副本，避免调用方直接修改存储。
         */
        default CompoundTag getModuleCustomData(ResourceLocation id) { return data().copy(); }

        /**
         * 写回指定模块的专属数据，并返回是否成功。
         */
        default boolean setModuleCustomData(ResourceLocation id, CompoundTag next) {
            data().merge(next); return true;
        }

        /**
         * 为指定模块提供当前载体解释后的环境接口。
         */
        default GhostEnvironment moduleEnvironment(ResourceLocation id) { return environment(); }
    }
    private record First(Environment environment, CompoundTag data) implements Storage {}
    private record Second(Environment environment, CompoundTag data) implements Storage {}
    private static final class CooldownHost implements GhostModuleHost, GhostModuleCustomDataModifier {
        private CompoundTag data = new CompoundTag();

        /**
         * 读取当前载体保存的模块实例列表。
         */
        public List<GhostModuleData.Entry> getModules() {
            return List.of(new GhostModuleData.Entry(NightWandererModule.ID, 5, 0, false, false, data));
        }

        /**
         * 返回指定模块的专属数据副本，避免调用方直接修改存储。
         */
        public CompoundTag getModuleCustomData(ResourceLocation id) { return data.copy(); }

        /**
         * 写回指定模块的专属数据，并返回是否成功。
         */
        public boolean setModuleCustomData(ResourceLocation id, CompoundTag next) { data = next.copy(); return true; }

        /**
         * 为指定模块提供世界操作接口，使模块不依赖载体实现。
         */
        public GhostWorldAccess moduleWorld(ResourceLocation id) {
            return (GhostWorldAccess) Proxy.newProxyInstance(GhostWorldAccess.class.getClassLoader(),
                    new Class<?>[]{GhostWorldAccess.class}, (proxy, method, args) -> {
                        if (method.getName().equals("present")) return true;
                        throw new AssertionError("冷却推进不应请求其他世界操作: " + method.getName());
                    });
        }
    }

    /**
     * 运行跨载体规则与环境接口回归检查，验证相同环境的结果一致。
     */
    public static void run() {
        GhostModuleRegistry.register(new NightWandererModule());
        var cooldown = new CooldownHost();
        cooldown.data.putInt("attackCooldown", 2);
        GhostModuleRuntime.dispatch(cooldown, new ModuleTickEvent());
        if (cooldown.data.getInt("attackCooldown") != 1)
            throw new AssertionError("非实体载体也必须推进夜游鬼冷却");
        GhostModuleRegistry.register(new WhiteGhostCandleModule());
        var first = new First(new Environment(), new CompoundTag());
        var second = new Second(new Environment(), new CompoundTag());
        for (Storage host : List.of(first, second)) {
            GhostModuleRuntime.dispatch(host, new ModuleTickEvent());
            if (host.environment().emissions != 0) throw new AssertionError("未点燃不应触发规则");
            host.data().putBoolean("lit", true);
            host.environment().exposed = false;
            GhostModuleRuntime.dispatch(host, new ModuleTickEvent());
            if (host.environment().emissions != 0 || host.environment().consumed != 0)
                throw new AssertionError("未暴露不能执行屏蔽和消耗");
            host.environment().exposed = true;
            GhostModuleRuntime.dispatch(host, new ModuleTickEvent());
            if (host.environment().emissions != 1 || Math.abs(host.environment().consumed - 0.30) > 1e-9)
                throw new AssertionError("屏蔽应执行一次并按声音强度额外燃烧");
        }
        if (first.environment().consumed != second.environment().consumed)
            throw new AssertionError("相同环境下不同载体必须一致");
    }
}
