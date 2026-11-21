package character.gojo;

import cn.blockforge.generated.nnnnr1201forge2.Network;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 客户端侧的无下限「定格墙」：所有角色的特效包最终都会走 {@code Client.receive}，
 * 在这里按本地玩家是否开着无下限再夹一次坐标 —— 覆盖服务端 {@code sendTo}、单人特效等
 * 不经过 {@code Network.send} 的路径。
 */
public final class InfinityClient {
    private static int level;

    private InfinityClient() {}

    /** 服务端同步：size = 等级（0 = 关闭）。 */
    public static void set(int value) {
        level = Math.max(0, value);
    }

    public static boolean active() {
        return level > 0;
    }

    public static Network.Effect deflect(Network.Effect e) {
        if (level <= 0 || e == null) return e;
        var mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null) return e;
        if (e.owner() == p.getId()) return e;
        int kind = e.kind();
        if (kind == 137 || kind == 144) return e;
        AABB zone = p.getBoundingBox();
        if (edge(zone, point(e.position())) > Infinity.RADIUS) return e;
        Vec3 center = zone.getCenter();
        Vec3 stuck = rim(center, e.position());
        Vec3 end = e.direction();
        if (end.lengthSqr() > 1e-4 && edge(zone, point(end)) <= Infinity.RADIUS) end = rim(center, end);
        return new Network.Effect(kind, e.owner(), stuck, end, e.size(), e.life(), e.seed(),
            e.wallpaperOwner(), e.desktopSlashes());
    }

    private static AABB point(Vec3 v) { return new AABB(v, v).inflate(.05); }

    private static Vec3 rim(Vec3 center, Vec3 at) {
        Vec3 dir = at.subtract(center);
        if (dir.lengthSqr() < 1e-6) dir = new Vec3(0, 1, 0);
        return center.add(dir.normalize().scale(Infinity.RADIUS + 0.02));
    }

    private static double edge(AABB a, AABB b) {
        double dx = Math.max(0, Math.max(a.minX - b.maxX, b.minX - a.maxX));
        double dy = Math.max(0, Math.max(a.minY - b.maxY, b.minY - a.maxY));
        double dz = Math.max(0, Math.max(a.minZ - b.maxZ, b.minZ - a.maxZ));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
