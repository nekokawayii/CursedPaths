package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** 被宿傩夺舍期间：强制第三人称（r326，事件号 798）。 */
@Mod.EventBusSubscriber(modid = "jujutsu_kaisen_cursed_paths", value = Dist.CLIENT)
public final class PossessionClient {
    private static long until;
    private static boolean wasForced;

    public static void receive(Network.Effect e) {
        if (e.life() <= 0) { until = 0; return; }   // 取消锁视角
        until = Math.max(until, System.currentTimeMillis() + e.life() * 50L);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        if (false) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.options == null) return;
        boolean active = System.currentTimeMillis() < until;
        if (active) {
            if (mc.options.getCameraType() == CameraType.FIRST_PERSON) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            wasForced = true;
        } else if (wasForced) {
            wasForced = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
    }

    private PossessionClient() { }
}
