package com.qidate.qisplan2.event;

import com.qidate.qisplan2.core.ModEntities;
import com.qidate.qisplan2.entity.ClosingGhost;
import com.qidate.qisplan2.entity.KnockingGhost;
import com.qidate.qisplan2.entity.OpeningGhost;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

@EventBusSubscriber
public class VillageGhostSpawner {

    private record PendingSpawn(
            ResourceKey<Level> dimension,
            ChunkPos chunkPos
    ) {}

    private static final ConcurrentLinkedQueue<PendingSpawn> QUEUE =
            new ConcurrentLinkedQueue<>();

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event){

        if(event.getLevel().isClientSide()) return;

        ServerLevel level = (ServerLevel) event.getLevel();

        if(level.dimension() != Level.OVERWORLD) return;

        QUEUE.add(new PendingSpawn(
                level.dimension(),
                event.getChunk().getPos()
        ));
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event){

        if(!(event.getLevel() instanceof ServerLevel level)) return;

        if(level.dimension() != Level.OVERWORLD) return;

        int maxPerTick = 2;

        while(maxPerTick-- > 0){

            PendingSpawn task = QUEUE.poll();

            if(task == null) return;

            if(task.dimension() != level.dimension()) continue;

            ChunkPos chunkPos = task.chunkPos();

            VillageGhostData data =
                    VillageGhostData.get(level);

            String key =
                    chunkPos.x + "," + chunkPos.z;

            if (data.isProcessed(key)) {
                continue;
            }

            data.markProcessed(key);

            BlockPos center = chunkPos.getMiddleBlockPosition(64);

            if (isVillage(level, center)) {
                if (level.random.nextFloat() < 0.20F || true) {
                    spawnGhost(level, chunkPos);
                }
            }
        }
    }

    private static void spawnGhost(ServerLevel level, ChunkPos chunkPos){

        BlockPos pos = level.getHeightmapPos(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                chunkPos.getMiddleBlockPosition(0)
        );

        switch(level.random.nextInt(3)){

            case 0 -> {
                OpeningGhost ghost = ModEntities.OPENING_GHOST.get().create(level);
                if(ghost != null){
                    ghost.moveTo(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5,0,0);
                    level.addFreshEntity(ghost);
                }
            }

            case 1 -> {
                ClosingGhost ghost = ModEntities.CLOSING_GHOST.get().create(level);
                if(ghost != null){
                    ghost.moveTo(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5,0,0);
                    level.addFreshEntity(ghost);
                }
            }

            case 2 -> {
                KnockingGhost ghost = ModEntities.KNOCKING_GHOST.get().create(level);
                if(ghost != null){
                    ghost.moveTo(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5,0,0);
                    level.addFreshEntity(ghost);
                }
            }
        }
    }

    private static boolean isVillage(ServerLevel level, BlockPos pos) {
        return level.structureManager()
                .getStructureWithPieceAt(pos, StructureTags.VILLAGE)
                .isValid();
    }
}