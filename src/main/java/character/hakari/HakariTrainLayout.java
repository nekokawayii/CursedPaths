package character.hakari;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Arrays;
import java.util.Base64;
import java.util.zip.Inflater;

/**
 * 「银灰穿梭 · 八节悬空地铁」结构（96 长 × 14 高 × 9 宽）的烘焙数据。
 * 由用户提供的 zip 里的结构 NBT 生成：palette 保留原始字符串，运行期用 BlockStateParser 解析一次并缓存。
 */
public final class HakariTrainLayout {
    public static final int SX=96, SY=14, SZ=9;
    private static final String[] PALETTE={
        "minecraft:gray_concrete",
        "minecraft:stone",
        "minecraft:iron_block",
        "minecraft:yellow_terracotta",
        "minecraft:smooth_stone",
        "minecraft:light_gray_concrete",
        "minecraft:cyan_concrete",
        "minecraft:sea_lantern",
        "minecraft:stone_button[face=wall,facing=south,powered=false]",
        "minecraft:stone_button[face=wall,facing=north,powered=false]",
        "minecraft:glass",
        "minecraft:stone_brick_stairs[facing=south,half=bottom,shape=straight]",
        "minecraft:stone_brick_stairs[facing=north,half=bottom,shape=straight]",
        "minecraft:smooth_stone_slab[type=bottom]",
        "minecraft:tinted_glass",
        "minecraft:iron_door[facing=south,half=lower,hinge=left,open=false]",
        "minecraft:iron_door[facing=south,half=upper,hinge=left,open=false]",
        "minecraft:smooth_stone_slab[type=top]",
        "minecraft:iron_door[facing=north,half=lower,hinge=left,open=false]",
        "minecraft:iron_door[facing=north,half=upper,hinge=left,open=false]",
        "minecraft:dark_oak_stairs[facing=north,half=bottom,shape=straight]",
        "minecraft:iron_bars[east=true,north=false,south=false,west=false]",
        "minecraft:dark_oak_stairs[facing=south,half=bottom,shape=straight]",
        "minecraft:iron_trapdoor[facing=north,half=top,open=false]",
        "minecraft:polished_deepslate_slab[type=bottom]",
        "minecraft:polished_deepslate",
        "minecraft:iron_bars[east=false,north=false,south=false,west=false]",
        "minecraft:polished_deepslate_slab[type=top]",
        "minecraft:lever[face=floor,facing=east,powered=false]"
    };
    /** 每块 3 字节：(x<<13)|(y<<9)|(z<<5)|state，zlib 压缩后 base64。 */
    private static final String DATA=
        "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"
        ;
    private static final BlockState[] CACHE=new BlockState[PALETTE.length];
    private static byte[] RAW;

    public interface Sink{ void put(int x,int y,int z,int state); }

    public static BlockState state(int index){
        BlockState cached=CACHE[index];
        if(cached!=null)return cached;
        String raw=PALETTE[index];
        BlockState resolved;
        try{
            resolved=BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(),raw,false).blockState();
        }catch(Exception ex){
            String name=raw.contains("[")?raw.substring(0,raw.indexOf(91)):raw;
            Block block=BuiltInRegistries.BLOCK.get(new ResourceLocation(name));
            resolved=block.defaultBlockState();
        }
        CACHE[index]=resolved;
        return resolved;
    }

    public static void forEach(Sink sink){
        byte[] raw=RAW;
        if(raw==null){
            String flat=DATA.replace(" ","").replace("\n","").replace("\r","");
            byte[] compressed=Base64.getDecoder().decode(flat);
            Inflater inflater=new Inflater();
            inflater.setInput(compressed);
            byte[] out=new byte[SX*SY*SZ*4];
            try{
                int n=inflater.inflate(out);
                raw=Arrays.copyOf(out,n);
            }catch(Exception ex){
                throw new IllegalStateException("地铁结构数据解压失败",ex);
            }finally{
                inflater.end();
            }
            RAW=raw;
        }
        for(int i=0;i+2<raw.length;i+=3){
            int v=((raw[i]&255)<<16)|((raw[i+1]&255)<<8)|(raw[i+2]&255);
            sink.put(v>>13,(v>>9)&15,(v>>5)&15,v&31);
        }
    }

    private HakariTrainLayout(){}
}