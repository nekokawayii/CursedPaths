package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Inflater;

/**
 * 死灭洄游地图：把「新宿｜摩天楼与霓虹十字街」（768 × 189 × 768，约 590 万非空气方块）
 * 按 16×16 区块烘焙成 {@code data/jujutsu_kaisen_cursed_paths/culling/shinjuku.bin}，
 * 区块生成时只铺自己那一格，因此不会在主线程一次性铺几百万方块。
 *
 * <p>文件格式：{@code CJSU1} + int32 chunksX + int32 chunksZ，
 * 然后按 (cz * chunksX + cx) 顺序每块写 int32 记录数 + 每条 4 字节记录：
 * {@code (x<<21)|(z<<17)|(y<<9)|paletteIndex}，x/z 是区块内 0..15，y 是地图内 0..188。
 */
public final class CullingMap {
    /** 768 / 16。 */
    public static final int CHUNKS_X = 48, CHUNKS_Z = 48;
    /** 结界半径：地图 768 宽，中心在 0，所以边界是 ±384。 */
    public static final int HALF = 384;
    /** 地图的 y=0 放到世界 y=61：地图中心地面（schem 里 y=3）正好落在 y=64，和旧城地面一致。 */
    public static final int BASE_Y = 61;
    /** 地图最高点 188 + BASE_Y = 249，天花板屏障放 511。 */
    public static final int CEILING = 511;

    private static final String BIN = "/data/jujutsu_kaisen_cursed_paths/culling/shinjuku.bin";
    private static final String PALETTE = "/data/jujutsu_kaisen_cursed_paths/culling/shinjuku_palette.txt";
    private static final int[] OFFSET = new int[CHUNKS_X * CHUNKS_Z];
    private static final int[] COUNT = new int[CHUNKS_X * CHUNKS_Z];
    private static byte[] data;
    private static BlockState[] states;
    private static boolean failed;

    /** 懒加载：第一次生成区块时读包内资源并建索引。 */
    private static synchronized boolean ensure() {
        if (data != null) return true;
        if (failed) return false;
        try {
            byte[] compressed = read(BIN);
            byte[] raw = inflate(compressed);
            if (raw.length < 13 || raw[0] != 'C' || raw[1] != 'J' || raw[2] != 'S' || raw[3] != 'U' || raw[4] != '1') {
                failed = true;
                return false;
            }
            int cx = readInt(raw, 5), cz = readInt(raw, 9);
            if (cx != CHUNKS_X || cz != CHUNKS_Z) { failed = true; return false; }
            int p = 13;
            for (int i = 0; i < OFFSET.length; i++) {
                int count = readInt(raw, p);
                p += 4;
                OFFSET[i] = p;
                COUNT[i] = count;
                p += count * 4;
            }
            List<String> lines = new ArrayList<>();
            for (String line : new String(read(PALETTE), StandardCharsets.UTF_8).split("\n")) {
                lines.add(line.trim());
            }
            states = new BlockState[lines.size()];
            for (int i = 0; i < lines.size(); i++) {
                states[i] = parse(lines.get(i));
            }
            data = raw;
            return true;
        } catch (Throwable t) {
            failed = true;
            return false;
        }
    }

    /** 区块是否在地图范围内。 */
    public static boolean covers(int chunkX, int chunkZ) {
        return chunkX >= -CHUNKS_X / 2 && chunkX < CHUNKS_X / 2
            && chunkZ >= -CHUNKS_Z / 2 && chunkZ < CHUNKS_Z / 2;
    }

    /** 只铺这一格区块里的方块（世界坐标由区块坐标推出）。 */
    public static void place(ChunkAccess chunk) {
        int cx = chunk.getPos().x, cz = chunk.getPos().z;
        if (!covers(cx, cz) || !ensure()) return;
        int index = (cz + CHUNKS_Z / 2) * CHUNKS_X + (cx + CHUNKS_X / 2);
        int count = COUNT[index];
        if (count <= 0) return;
        int p = OFFSET[index];
        for (int i = 0; i < count; i++, p += 4) {
            int v = readInt(data, p);
            int lx = (v >>> 21) & 15;
            int lz = (v >>> 17) & 15;
            int ly = (v >>> 9) & 0xFF;
            int pi = v & 0x1FF;
            BlockState state = pi < states.length ? states[pi] : null;
            if (state == null || state.isAir()) continue;
            chunk.setBlockState(new BlockPos(cx * 16 + lx, BASE_Y + ly, cz * 16 + lz), state, false);
        }
    }

    private static BlockState parse(String text) {
        if (text == null || text.isBlank()) return Blocks.AIR.defaultBlockState();
        try {
            return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), text, false).blockState();
        } catch (Throwable t) {
            String name = text.contains("[") ? text.substring(0, text.indexOf('[')) : text;
            Block block = BuiltInRegistries.BLOCK.get(new ResourceLocation(name));
            return block == null ? Blocks.AIR.defaultBlockState() : block.defaultBlockState();
        }
    }

    private static byte[] read(String path) throws Exception {
        try (InputStream in = CullingMap.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("缺少资源 " + path);
            ByteArrayOutputStream out = new ByteArrayOutputStream(1 << 20);
            byte[] buf = new byte[1 << 16];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toByteArray();
        }
    }

    private static byte[] inflate(byte[] compressed) throws Exception {
        Inflater inflater = new Inflater();
        inflater.setInput(compressed);
        ByteArrayOutputStream out = new ByteArrayOutputStream(1 << 24);
        byte[] buf = new byte[1 << 16];
        try {
            while (!inflater.finished()) {
                int n = inflater.inflate(buf);
                if (n == 0 && inflater.needsInput()) break;
                out.write(buf, 0, n);
            }
        } finally {
            inflater.end();
        }
        return out.toByteArray();
    }

    private static int readInt(byte[] b, int p) {
        return ((b[p] & 255) << 24) | ((b[p + 1] & 255) << 16) | ((b[p + 2] & 255) << 8) | (b[p + 3] & 255);
    }

    private CullingMap() {}
}
