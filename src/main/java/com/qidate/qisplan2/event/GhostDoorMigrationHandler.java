package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.block.GhostDoorBlock;
import com.qidate.qisplan2.block.entity.GhostDoorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/** 旧版本鬼门没有方块实体；区块加载时补建下半部宿主，使已开启的旧门也能继续运行。 */
@EventBusSubscriber(modid = QisPlan2.MODID)
public final class GhostDoorMigrationHandler {
    /** 仅通过事件总线调用。 */
    private GhostDoorMigrationHandler() {}
    /** 延后到服务端主线程处理，并跳过已经卸载的区块。 */
    @SubscribeEvent public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var chunkPos = event.getChunk().getPos();
        level.getServer().execute(() -> {
            if (!level.hasChunk(chunkPos.x, chunkPos.z)) return;
            var chunk = level.getChunk(chunkPos.x, chunkPos.z);
            for (int sectionIndex = 0; sectionIndex < chunk.getSections().length; sectionIndex++) {
                var section = chunk.getSection(sectionIndex);
                if (!section.maybeHas(state -> state.getBlock() instanceof GhostDoorBlock)) continue;
                int baseY = chunk.getMinBuildHeight() + sectionIndex * 16;
                for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                    var state = section.getBlockState(x,y,z);
                    if (!(state.getBlock() instanceof GhostDoorBlock) || state.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER) continue;
                    var pos = new BlockPos(chunkPos.getMinBlockX()+x,baseY+y,chunkPos.getMinBlockZ()+z);
                    if (!(level.getBlockEntity(pos) instanceof GhostDoorBlockEntity))
                        level.setBlockEntity(new GhostDoorBlockEntity(pos,state));
                }
            }
        });
    }
}
