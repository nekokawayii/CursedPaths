package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 宿命黑闪：**虎杖打真人必定黑闪**（用户要求）——近战、术式、领域，任何来源的伤害都算，伤害 ×10。
 * 覆盖全部组合：玩家↔玩家（PvP）、玩家↔NPC（PvE）、NPC↔NPC 都成立。
 *
 * <p>NPC 施法走的是 {@link NpcBridge.NpcActor} 假玩家，所以这里用 {@link NpcBridge#body(ServerPlayer)}
 * 反查回真正的 Mob 再判身份，否则"NPC 的技能伤害"会被漏掉。
 *
 * <p>{@link #applied} 是**同一次伤害**的标记：本类在 {@link EventPriority#HIGHEST} 先跑并置位，
 * 后面 {@code YujiRebuild}（LOW）与 {@code NpcFist}（默认）里的黑闪通道会跳过，避免 ×10 被叠成 ×100。
 */
@Mod.EventBusSubscriber(modid = "jujutsu_kaisen_cursed_paths")
public final class FateBlackFlash {
    /** 黑闪伤害倍数（与用户口径一致：×10，不是 2.5 次方）。 */
    public static final double SCALE = 10.0;
    /** 本次伤害已经被宿命黑闪处理过。 */
    public static boolean applied;

    private FateBlackFlash() { }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hurt(LivingHurtEvent e) {
        applied = false;
        if (e.getEntity().level().isClientSide) return;
        Entity source = e.getSource().getEntity();
        if (!(source instanceof LivingEntity raw)) return;
        LivingEntity attacker = body(raw);
        if (!yuji(attacker)) return;
        if (!mahito(e.getEntity())) return;
        applied = true;
        e.setAmount((float) Math.min(1.0e7, Math.max(1.0, e.getAmount()) * SCALE));
        if (e.getEntity().level() instanceof ServerLevel level) {
            Network.send(level, new Network.Effect(YujiFx.BLACK_FLASH, attacker.getId(),
                e.getEntity().position(), attacker.getLookAngle(), 3f, 20, 0));
        }
    }

    /** 假玩家（NPC 施法）反查回它操纵的实体。 */
    private static LivingEntity body(LivingEntity e) {
        return e instanceof ServerPlayer p ? NpcBridge.body(p) : e;
    }

    private static String character(LivingEntity e) {
        if (e instanceof Player p) {
            var definition = character.CharacterService.selected(p);
            return definition == null ? "" : definition.id();
        }
        return NpcBridge.character(e);
    }

    public static boolean yuji(LivingEntity e) {
        return e != null && "yuji".equals(character(e));
    }

    public static boolean mahito(LivingEntity e) {
        if (e == null) return false;
        if ("mahito".equals(character(e))) return true;
        // 真人的其它形态：受肉失败体、分身
        String path = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(e.getType()).getPath();
        return path.equals("broken_mahito") || path.equals("mahito_clone");
    }
}
