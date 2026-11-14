package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * NPC / 首领版的「咒力附拳」：不像玩家那样手动开关，而是**有目标就自动开 15 秒、关掉后冷却 30 秒**。
 * 开启期间每次造成伤害都有 1% 概率打出黑闪（伤害 ×10），与虎杖 NPC 自己的「黑闪的才能」可以叠加。
 */
@Mod.EventBusSubscriber(modid = "jujutsu_kaisen_cursed_paths")
public final class NpcFist {
    /** 每次维持 15 秒。 */
    public static final int DURATION = 15 * 20;
    /** 关闭后冷却 30 秒。 */
    public static final int COOLDOWN = 30 * 20;
    /** 开启期间的每次伤害触发黑闪的概率。 */
    public static final double BLACK_FLASH_CHANCE = 0.01;
    /** 黑闪伤害倍数（用户口径：×10，不再用 2.5 次方）。 */
    public static final double BLACK_FLASH_SCALE = 10.0;
    /** 没有角色 ID 的实体（戴天/大佛/魔虚罗这类首领）靠血量判定。 */
    private static final double BOSS_HEALTH = 2000.0;

    private NpcFist() { }

    /** 谁能用：有角色 ID 的 NPC，或血量达到首领档的怪。 */
    public static boolean user(Mob mob) {
        return mob != null && (NpcBridge.supports(mob) || mob.getMaxHealth() >= BOSS_HEALTH);
    }

    public static boolean active(Mob mob) {
        return mob != null && mob.getPersistentData().getBoolean("Fist");
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent e) {
        if (!(e.getEntity() instanceof Mob mob) || mob.level().isClientSide || !mob.isAlive() || mob.isNoAi()) return;
        if (!user(mob)) return;
        var data = mob.getPersistentData();
        long now = mob.level().getGameTime();
        if (data.getBoolean("Fist")) {
            if (now >= data.getLong("NpcFistEnd")) {
                data.putBoolean("Fist", false);
                data.putLong("NpcFistReadyAt", now + COOLDOWN);
            }
            return;
        }
        if (now < data.getLong("NpcFistReadyAt")) return;
        var target = mob.getTarget();
        if (target == null || !target.isAlive() || mob.distanceToSqr(target) > 48 * 48) return;
        data.putBoolean("Fist", true);
        data.putLong("NpcFistEnd", now + DURATION);
        if (mob.level() instanceof ServerLevel level) {
            Network.send(level, new Network.Effect(YujiFx.BLACK_FLASH, mob.getId(),
                mob.getEyePosition(), mob.getLookAngle(), 2.4f, 18, 0));
        }
    }

    @SubscribeEvent
    public static void hurt(LivingHurtEvent e) {
        if (e.getEntity().level().isClientSide) return;
        if (FateBlackFlash.applied) return; // 宿命黑闪已经乘过 ×10，不再叠
        if (!(e.getSource().getDirectEntity() instanceof Mob mob) || mob != e.getSource().getEntity()) return;
        if (!active(mob)) return;
        if (mob.getRandom().nextDouble() >= BLACK_FLASH_CHANCE) return;
        double amount = Math.max(1.0, e.getAmount()) * BLACK_FLASH_SCALE;
        e.setAmount((float) Math.min(1.0e7, amount));
        if (e.getEntity().level() instanceof ServerLevel level) {
            Network.send(level, new Network.Effect(YujiFx.BLACK_FLASH, mob.getId(),
                e.getEntity().position(), mob.getLookAngle(), 3f, 20, 0));
        }
    }
}
