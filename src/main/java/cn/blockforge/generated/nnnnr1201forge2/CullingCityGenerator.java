package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.serialization.Codec;
import java.util.concurrent.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

/** 768×768「新宿｜摩天楼与霓虹十字街」按 16×16 区块生成；边界为 ±384 的黑色结界墙，天花板 511。 */
public final class CullingCityGenerator extends FlatLevelSource {
    public static final Codec<CullingCityGenerator> CODEC=FlatLevelGeneratorSettings.CODEC.fieldOf("settings").xmap(CullingCityGenerator::new,CullingCityGenerator::settings).codec();
    public CullingCityGenerator(FlatLevelGeneratorSettings s){super(s);}
    @Override protected Codec<? extends ChunkGenerator> codec(){return CODEC;}
    @Override public int getGenDepth(){return 512;}
    @Override public int getSpawnHeight(net.minecraft.world.level.LevelHeightAccessor l){return 65;}
    @Override public int getBaseHeight(int x,int z,Heightmap.Types t,net.minecraft.world.level.LevelHeightAccessor l,RandomState r){return 65;}
    @Override public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor,Blender blender,RandomState random,StructureManager manager,ChunkAccess c){return super.fillFromNoise(executor,blender,random,manager,c).thenApply(chunk->{generate(chunk);return chunk;});}
    private static void generate(ChunkAccess c){
        var barrier=ExpeditionContent.block("culling_barrier");
        int minX=c.getPos().getMinBlockX(),minZ=c.getPos().getMinBlockZ();
        // ① 天花板 + 结界墙：墙在地图边界 ±384 那一圈，从地面一直封到天花板
        for(int x=minX;x<minX+16;x++)for(int z=minZ;z<minZ+16;z++){
            put(c,x,CullingMap.CEILING,z,barrier);
            if(Math.abs(x)==CullingMap.HALF||Math.abs(z)==CullingMap.HALF){
                for(int y=1;y<CullingMap.CEILING;y++)put(c,x,y,z,barrier);
            }
        }
        // ② 地图本体（只铺本区块，约 2500 块）
        CullingMap.place(c);
        // ③ 出生点设施：兑换台与离开点（地图中心地块）
        for(int x=minX;x<minX+16;x++)for(int z=minZ;z<minZ+16;z++){
            if(x==0&&z==-8)put(c,x,65,z,ExpeditionContent.block("culling_exchange"));
            if(x==2&&z==-8)put(c,x,65,z,ExpeditionContent.block("culling_exit"));
            if(x==0&&z==0)put(c,x,105,z,Blocks.BEACON);
        }
    }
    private static void put(ChunkAccess c,int x,int y,int z,Block b){c.setBlockState(new BlockPos(x,y,z),b.defaultBlockState(),false);}
    @Override public void buildSurface(net.minecraft.server.level.WorldGenRegion r,StructureManager m,RandomState s,ChunkAccess c){}
}
