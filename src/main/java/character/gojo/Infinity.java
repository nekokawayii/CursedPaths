package character.gojo;

import cn.blockforge.generated.nnnnr1201forge2.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/**
 * 五条悟 · 无下限（玩家版）。
 *
 * <p>开启后每秒消耗 250 咒力；1.0 格内「无限减速」一切东西，速度倍率 = (距离/1.0)^6，
 * 距离 ≤ 0.2 判绝对静止。同时积累「混乱的思绪」：每 25 秒 +1 级（无上限），
 * 每级降低伤害/移速、拉长冷却。反转术式可直接解除并免疫 15 秒。
 */
@Mod.EventBusSubscriber(modid = "jujutsu_kaisen_cursed_paths")
public final class Infinity {
    public static final int SKILL = 9;
    /** 服务端 → 客户端同步无下限状态（size = 等级，0 = 关闭）。 */
    public static final int SYNC_KIND = 996;
    public static final double RADIUS = 1.0, STOP = 0.2, COST_PER_SECOND = 250;
    /** 抄 JJK-HeianEra 的几何：贴身处 0.5 格硬定身，2.0 格内算「身体距离」拦近战。 */
    public static final double EDGE = 0.5, BODY_BAND = 2.0;
    public static final int STACK_TICKS = 500, IMMUNE_TICKS = 300, MAX_LEVEL = 200;

    private static final String K_ON = "GojoInfinity", K_LEVEL = "GojoInfinityLevel",
        K_NEXT = "GojoInfinityNext", K_IMMUNE = "GojoInfinityImmune", K_SEVERED = "GojoInfinitySevered";
    private static final UUID SPEED_ID = UUID.fromString("8c11a000-0000-4a11-9c00-00000000b001");
    private static final UUID DAMAGE_ID = UUID.fromString("8c11a000-0000-4a11-9c00-00000000b002");

    /** 被定住的东西：记录原速度/重力/引信，离开范围时还原。 */
    private static final class Held {
        Vec3 speed;
        boolean noGravity;
        int fuse = -1;
        boolean hadModifier;
    }
    private static final Map<UUID, Map<UUID, Held>> HELD = new HashMap<>();
    private static final Map<UUID, Integer> LEVEL_CACHE = new HashMap<>();
    /** 被硬定身的实体 → 锁死的位置（离开 0.5 格后清除）。 */
    private static final Map<UUID, Vec3> LOCKED = new LinkedHashMap<>();

    public static boolean active(Player p) {
        return p != null && Curse.data(p).getBoolean(K_ON);
    }

    public static int level(Player p) {
        return p == null ? 0 : Math.max(0, Math.min(MAX_LEVEL, Curse.data(p).getInt(K_LEVEL)));
    }

    public static double cooldownMul(Player p) {
        int level = level(p);
        return 1 + 0.06 * level;
    }

    private static boolean immune(Player p) {
        return Curse.data(p).getLong(K_IMMUNE) > p.level().getGameTime();
    }

    // ------------------------------------------------------------ 开关
    public static void toggle(ServerPlayer p) {
        if (!GojoPlayer.unlocked(p)) { p.displayClientMessage(Component.literal("尚未觉醒六眼"), true); return; }
        var data = Curse.data(p);
        boolean on = !data.getBoolean(K_ON);
        data.putBoolean(K_ON, on);
        data.putLong(K_IMMUNE, 0);
        if (on) {
            if (!immune(p)) data.putInt(K_LEVEL, Math.max(1, data.getInt(K_LEVEL)));
            data.putLong(K_NEXT, p.level().getGameTime() + STACK_TICKS);
            p.displayClientMessage(Component.literal("无下限：展开（每秒 250 咒力）"), true);
            p.level().playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 1.9f);
            fx(p, 2.4f, 30);
        } else {
            shutdown(p, "无下限：收起");
        }
        applyDebuff(p);
        sync(p);
    }

    private static void shutdown(ServerPlayer p, String message) {
        Curse.data(p).putBoolean(K_ON, false);
        restoreAll(p);
        applyDebuff(p);
        sync(p);
        if (message != null) p.displayClientMessage(Component.literal(message), true);
    }

    /** 反转术式解除：清空等级并给 15 秒免疫。 */
    public static void onReverse(ServerPlayer p) {
        var data = Curse.data(p);
        data.putInt(K_LEVEL, 0);
        data.putLong(K_IMMUNE, p.level().getGameTime() + IMMUNE_TICKS);
        data.putLong(K_NEXT, p.level().getGameTime() + STACK_TICKS);
        applyDebuff(p);
        sync(p);
        p.displayClientMessage(Component.literal("反转术式：混乱的思绪已清除，15 秒内不再积累"), true);
    }

    /** 服务端 → 客户端同步无下限状态：客户端用它决定是否把特效夹在 1.0 格球面上。 */
    private static void sync(ServerPlayer p) {
        Network.sendTo(p, new Network.Effect(SYNC_KIND, p.getId(), p.position(), Vec3.ZERO,
            active(p) ? Math.max(1, level(p)) : 0f, 0, 0));
    }

    /** 天逆鉾：无下限强制关闭 3 秒。 */
    public static void sever(ServerPlayer p) {
        Curse.data(p).putLong(K_SEVERED, p.level().getGameTime() + 60);
        if (active(p)) shutdown(p, "天逆鉾：无下限被斩断（3 秒）");
    }

    // ------------------------------------------------------------ 主循环
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.getServer() == null) return;
        long now = e.getServer().overworld().getGameTime();
        for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) {
            boolean on = active(p);
            if (!on) { restoreAll(p); continue; }
            if (Curse.data(p).getLong(K_SEVERED) > now) { shutdown(p, null); continue; }
            if (!Curse.spend(p, COST_PER_SECOND / 20.0)) { shutdown(p, "咒力耗尽：无下限解除"); continue; }
            // 混乱的思绪：每 25 秒 +1 级
            if (!immune(p)) {
                if (Curse.data(p).getLong(K_NEXT) <= now) {
                    int level = Math.min(MAX_LEVEL, level(p) + 1);
                    Curse.data(p).putInt(K_LEVEL, level);
                    Curse.data(p).putLong(K_NEXT, now + STACK_TICKS);
                    applyDebuff(p);
                    p.displayClientMessage(Component.literal("混乱的思绪 " + level + " 级：伤害/移速下降，冷却变长"), true);
                    fx(p, 1.6f, 12);
                }
            }
            zone(p, now);
            if (now % 20 == 0) { sync(p); fx(p, (float) (RADIUS * 2.0), 4); }
        }
        // 玩家下线/切维度清理
        HELD.keySet().removeIf(id -> e.getServer().getPlayerList().getPlayer(id) == null);
    }

    /** 1.0 格内的一切：弹道、生物、载具、掉落物、TNT、苦力怕。 */
    private static void zone(ServerPlayer p, long now) {
        ServerLevel level = p.serverLevel();
        AABB reach = p.getBoundingBox().inflate(RADIUS + 1.0);
        Map<UUID, Held> held = HELD.computeIfAbsent(p.getUUID(), k -> new HashMap<>());
        Set<UUID> seen = new HashSet<>();
        for (Entity entity : level.getEntities(p, reach, t -> t.isAlive() && t != p)) {
            double dist = edge(p.getBoundingBox(), entity.getBoundingBox());
            if (dist > RADIUS) continue;
            seen.add(entity.getUUID());
            double decay = dist <= STOP ? 0 : Math.pow(Math.min(1, dist / RADIUS), 6);
            Held state = held.computeIfAbsent(entity.getUUID(), k -> {
                Held h = new Held();
                h.speed = entity.getDeltaMovement();
                h.noGravity = entity.isNoGravity();
                if (entity instanceof PrimedTnt tnt) h.fuse = tnt.getFuse();
                return h;
            });
            // 贴身处（<= EDGE 0.5 格）直接硬定身：位置锁死 + 速度清零 + 关重力（抄 JJK-HeianEra 的 freeze）
            boolean holds = !(entity instanceof Player) && dist <= EDGE;
            if (holds) {
                Vec3 lock = LOCKED.computeIfAbsent(entity.getUUID(), k -> entity.position());
                entity.setNoGravity(true);
                entity.setDeltaMovement(Vec3.ZERO);
                if (entity.position().distanceToSqr(lock) > 1e-8) entity.setPos(lock.x, lock.y, lock.z);
                entity.fallDistance = 0;
                if (entity instanceof LivingEntity le) {
                    AttributeInstance speed = le.getAttribute(Attributes.MOVEMENT_SPEED);
                    if (speed != null) {
                        if (speed.getModifier(SPEED_ID) != null) speed.removeModifier(SPEED_ID);
                        speed.addTransientModifier(new AttributeModifier(SPEED_ID, "无下限定身", -1, AttributeModifier.Operation.MULTIPLY_TOTAL));
                    }
                    le.hurtMarked = true;
                }
                continue;
            }
            if (entity instanceof ExperienceOrb) continue; // 经验球让五条悟主动走过去吸收
            // 生物/载具/玩家：属性减速 + 到 0.2 格锁位移
            if (entity instanceof LivingEntity living) {
                AttributeInstance speed = living.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed != null) {
                    double amount = decay <= 0 ? -1 : decay - 1;
                    var old = speed.getModifier(SPEED_ID);
                    if (old != null) speed.removeModifier(SPEED_ID);
                    speed.addTransientModifier(new AttributeModifier(SPEED_ID, "无下限减速", amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
                if (decay <= 0) {
                    living.setDeltaMovement(0, Math.min(0, living.getDeltaMovement().y), 0);
                    living.hurtMarked = true;
                } else {
                    living.setDeltaMovement(living.getDeltaMovement().scale(decay));
                    living.hurtMarked = true;
                }
            } else if (entity instanceof PrimedTnt tnt) {
                if (state.fuse > 0) tnt.setFuse(state.fuse);
            } else if (entity instanceof Creeper creeper) {
                creeper.setSwellDir(-1);
            } else {
                // 投射物 / 掉落物 / 其它实体：直接改速度
                if (decay <= 0) {
                    entity.setDeltaMovement(Vec3.ZERO);
                    entity.setNoGravity(true);
                } else {
                    entity.setDeltaMovement(entity.getDeltaMovement().scale(decay));
                }
            }
            if (entity instanceof ItemEntity item && decay <= 0 && item.tickCount > 100) {
                ItemStack stack = item.getItem().copy();
                if (!p.getInventory().add(stack)) p.drop(stack, false);
                item.discard();
            }
            if (entity instanceof Projectile projectile && decay <= 0) projectile.setDeltaMovement(Vec3.ZERO);
        }
        // 离开 1.0 格：还原
        held.entrySet().removeIf(entry -> {
            if (seen.contains(entry.getKey())) return false;
            Entity entity = level.getEntity(entry.getKey());
            if (entity != null) restore(entity, entry.getValue());
            return true;
        });
    }

    private static void restore(Entity entity, Held state) {
        LOCKED.remove(entity.getUUID());
        if (entity instanceof LivingEntity living) {
            AttributeInstance speed = living.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null && speed.getModifier(SPEED_ID) != null) speed.removeModifier(SPEED_ID);
        }
        entity.setNoGravity(state.noGravity);
        if (state.speed != null) entity.setDeltaMovement(state.speed);
        if (entity instanceof PrimedTnt tnt && state.fuse > 0) tnt.setFuse(state.fuse);
        entity.level().playSound(null, entity.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, .5f, 1.6f);
    }

    private static void restoreAll(ServerPlayer p) {
        Map<UUID, Held> held = HELD.remove(p.getUUID());
        if (held == null) return;
        ServerLevel level = p.serverLevel();
        for (var entry : held.entrySet()) {
            Entity entity = level.getEntity(entry.getKey());
            if (entity != null) restore(entity, entry.getValue());
        }
    }

    // ------------------------------------------------------------ 伤害拦截
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void hurt(LivingAttackEvent e) {
        if (e.getEntity().level().isClientSide || !(e.getEntity() instanceof ServerPlayer p)) return;
        if (blocked(p, e.getSource())) e.setCanceled(true);
    }

    /** 兜底：万一有别的模组/通道绕过 LivingAttackEvent，在扣血前再拦一次。 */
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void hurtLate(net.minecraftforge.event.entity.living.LivingHurtEvent e) {
        if (e.getEntity().level().isClientSide || !(e.getEntity() instanceof ServerPlayer p)) return;
        if (e.getAmount() <= 0) return;
        if (blocked(p, e.getSource())) e.setCanceled(true);
    }

    /** true = 这一下该被无下限挡掉。 */
    private static boolean blocked(ServerPlayer p, net.minecraft.world.damagesource.DamageSource source) {
        if (!active(p)) return false;
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        // 环境伤害（火/岩浆/冻结）直接免疫：粒子都被减速了
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) || source.is(net.minecraft.tags.DamageTypeTags.IS_FREEZING)) {
            return shieldFeedback(p, null);
        }
        if (p.getPersistentData().getBoolean("SukunaWorldSlashHit")) return false;
        LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
        // 天逆鉾：刺中即斩断无下限 3 秒
        if (attacker != null && isTenka(attacker)) {
            sever(p);
            p.level().playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.2f, .6f);
            return false;
        }
        // 领域展延：术式穿透无下限
        if (attacker != null && DomainAmplification.active(attacker)) return false;
        // 距离：按碰撞箱边缘算；近战怪有攻击距离，放宽 0.6 格
        double reach = EDGE + BODY_BAND;   // 2.5 格：覆盖各种近战攻击距离
        if (attacker != null) {
            if (bodyDist(attacker, p.getBoundingBox().getCenter()) > reach) return false;
        } else {
            Vec3 at = source.getSourcePosition();
            if (at != null) {
                if (edge(p.getBoundingBox(), point(at)) > reach) return false;
            } else if (source.is(net.minecraft.world.damagesource.DamageTypes.STARVE)
                || source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)) {
                return false; // 机制性伤害不挡
            }
            // 没有来源实体也没有坐标的远程/领域伤害：无下限直接吃掉
        }
        return shieldFeedback(p, attacker);
    }

    private static boolean shieldFeedback(ServerPlayer p, LivingEntity attacker) {
        p.level().playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, .6f);
        fx(p, 1.4f, 6);
        if (attacker instanceof ServerPlayer other) {
            other.displayClientMessage(Component.literal("无下限：伤害被无限减速挡下（需要领域展延）"), true);
        }
        return true;
    }

    // ------------------------------------------------------------ 混乱的思绪
    private static void applyDebuff(ServerPlayer p) {
        int level = active(p) ? level(p) : 0;
        AttributeInstance move = p.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance damage = p.getAttribute(Attributes.ATTACK_DAMAGE);
        if (move != null) {
            if (move.getModifier(SPEED_ID) != null) move.removeModifier(SPEED_ID);
            if (level > 0) move.addTransientModifier(new AttributeModifier(SPEED_ID, "混乱的思绪", 1.0 / (1 + .06 * level) - 1, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        if (damage != null) {
            if (damage.getModifier(DAMAGE_ID) != null) damage.removeModifier(DAMAGE_ID);
            if (level > 0) damage.addTransientModifier(new AttributeModifier(DAMAGE_ID, "混乱的思绪", 1.0 / (1 + .08 * level) - 1, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static boolean isTenka(LivingEntity entity) {
        for (var hand : net.minecraft.world.InteractionHand.values()) {
            var stack = entity.getItemInHand(hand);
            if (stack.isEmpty()) continue;
            String id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).getPath();
            if (id.equals("tenka_spear") || id.equals("tenka")) return true;
        }
        return false;
    }

    /**
     * 所有角色的「术式特效」被无下限**定格在 1.0 格边界上**：
     * 在 {@link Network#send(ServerLevel, Network.Effect)} 广播前把落在圈内的特效位置/终点夹到边界球面上，
     * 于是火山、弹幕、斩击这些每刻刷新位置的特效会一直停在五条悟面前那层「看不见的墙」上。
     */
    public static Network.Effect deflect(ServerLevel level, Network.Effect effect) {
        if (effect == null) return null;
        int kind = effect.kind();
        if (kind == 137 || kind == 144) return effect; // 界面/状态类不算术式
        for (ServerPlayer p : level.players()) {
            if (!active(p) || effect.owner() == p.getId()) continue;
            AABB zone = p.getBoundingBox();
            if (edge(zone, point(effect.position())) > RADIUS) continue;
            Vec3 center = zone.getCenter();
            Vec3 stuck = rim(center, effect.position());
            Vec3 end = effect.direction();
            if (end.lengthSqr() > 1e-4 && edge(zone, point(end)) <= RADIUS) end = rim(center, end);
            if (level.getGameTime() % 4 == 0) fx(p, 1.5f, 6);
            return new Network.Effect(kind, effect.owner(), stuck, end, effect.size(), effect.life(),
                effect.seed(), effect.wallpaperOwner(), effect.desktopSlashes());
        }
        return effect;
    }

    private static AABB point(Vec3 v) { return new AABB(v, v).inflate(.05); }

    /** 把一个点拉到以 center 为球心、半径 1.0 的球面上。 */
    private static Vec3 rim(Vec3 center, Vec3 at) {
        Vec3 dir = at.subtract(center);
        if (dir.lengthSqr() < 1e-6) dir = new Vec3(0, 1, 0);
        return center.add(dir.normalize().scale(RADIUS + 0.02));
    }

    /** 实体「身体」到某点的距离：按宽高胶囊算（JJK-HeianEra 的 bodyDist 同款思路）。 */
    public static double bodyDist(LivingEntity e, Vec3 at) {
        double half = e.getBbWidth() * 0.5;
        double bottom = e.getY() + half;
        double top = e.getY() + e.getBbHeight() - half;
        if (top < bottom) top = bottom;
        double y = Math.max(bottom, Math.min(top, at.y));
        double dx = at.x - e.getX(), dy = at.y - y, dz = at.z - e.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz) - half;
    }

    /** 两个碰撞箱之间的最近距离（Boss 大碰撞箱按边缘算，不按中心算）。 */
    private static double edge(AABB a, AABB b) {
        double dx = Math.max(0, Math.max(a.minX - b.maxX, b.minX - a.maxX));
        double dy = Math.max(0, Math.max(a.minY - b.maxY, b.minY - a.maxY));
        double dz = Math.max(0, Math.max(a.minZ - b.maxZ, b.minZ - a.maxZ));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static void fx(ServerPlayer p, float size, int life) {
        Network.send(p.serverLevel(), new Network.Effect(GojoFx.BLOCK, p.getId(), p.getEyePosition(), Vec3.ZERO, size, life, 0));
    }

    public static void clear(ServerPlayer p) {
        Curse.data(p).putBoolean(K_ON, false);
        Curse.data(p).putInt(K_LEVEL, 0);
        restoreAll(p);
        applyDebuff(p);
    }

    private Infinity() {}
}
