package com.qidate.qisplan2.block;

import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.GhostModuleRuntime;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.event.TouchEvent;
import com.qidate.qisplan2.ghost.module.implementation.GhostGrassModule;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 静态鬼草丛模块宿主。
 * 方块只提供模块配置、物理接触事实和原有挖掘限制，不保存冷却或执行袭击。
 * 同种静态方块共享定义，目标进度由关系状态适配器按目标隔离。
 */
public class GhostGrassBlock extends Block implements GhostModuleHost {
    private static final List<GhostModuleData.Entry> MODULES =
            List.of(new GhostModuleData.Entry(GhostGrassModule.ID, new GhostGrassModule().getInitialData().intensity()));

    /** 创建草丛方块，物理属性继续由注册配置提供。 */
    public GhostGrassBlock(Properties properties) { super(properties); }

    /** 声明自带的鬼草丛模块，无需新增方块实体。 */
    @Override
    public List<GhostModuleData.Entry> getModules() { return MODULES; }

    /** 静态模块定义不接受复苏写入；目标冷却存储由独立关系状态接口处理。 */
    @Override
    public boolean allowsAbilityRevivalStimulation() { return false; }

    /**
     * 将服务端实际碰撞转译为接触事实。
     * 此回调可能由多个相邻草丛重复调用，因此不把回调次数声明为经过的时间；
     * 鬼草丛规则通过目标冷却限制袭击频率。
     */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || !(entity instanceof LivingEntity target) || !target.isAlive()) return;
        GhostModuleRuntime.dispatch(this, new TouchEvent(null, target));
    }

    /** 非创造模式不能正常挖掘，创造模式沿用原版处理。 */
    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return player.isCreative() ? super.getDestroyProgress(state, player, level, pos) : 0.0F;
    }
}