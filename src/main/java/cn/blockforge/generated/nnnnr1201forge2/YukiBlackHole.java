package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 九十九由基 · 「质量坍缩·黑洞」的世界侧状态（r390）。
 * <p>只负责「黑洞存在多久」以及把位置/半径广播给客户端做后期渲染；
 * 伤害、吸扯与自损仍由 {@link character.yuki.YukiCombat#blackHole} 结算，两边不互相依赖。
 * <p>取景与透镜在 {@link YukiBlackHoleClient}：屏幕空间采样主 framebuffer 的彩色/深度副本，
 * 不改世界几何、不写方块、不影响游戏逻辑。
 */
@Mod.EventBusSubscriber(modid = "jujutsu_kaisen_cursed_paths")
public final class YukiBlackHole {
    /** 客户端后期通道号；799 紧接 YujiFx.LAST(798)，未与任何角色号段冲突。 */
    public static final int FX = 799;
    /** 默认演出时长：5 秒。 */
    public static final int LIFETIME = 100;
    /** 默认世界半径（格），只影响视觉与镜头尺度。r395：1.8 → 5.4（用户要求"再大 3 倍"）。 */
    public static final float RADIUS = 5.4f;
    /** 视觉中心放在施术者眼前多远（格）；太近会糊满整屏。 */
    public static final double DISTANCE = 7.0;

    private record State(ServerLevel level, long end) { }
    private static final Map<UUID, State> ACTIVE = new HashMap<>();

    /** 在指定位置亮起一个黑洞；重复调用会刷新同一施术者的黑洞。 */
    public static void open(ServerLevel level, ServerPlayer owner, Vec3 center, float radius, int ticks) {
        int life = Math.max(20, ticks);
        ACTIVE.put(owner.getUUID(), new State(level, level.getGameTime() + life));
        Network.send(level, new Network.Effect(FX, owner.getId(), center, Vec3.ZERO, radius, life, 0));
        com.mojang.logging.LogUtils.getLogger().info("[黑洞] 触发 {} 半径={} 时长={} @{}", owner.getGameProfile().getName(), radius, life, center);
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) return;
        ACTIVE.values().removeIf(s -> s.level().getGameTime() >= s.end());
    }

    @SubscribeEvent public static void stop(ServerStoppedEvent e) { ACTIVE.clear(); }

    private YukiBlackHole() { }
}
