package cn.blockforge.generated.nnnnr1201forge2;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 甚尔 QTE 的 HUD 注册。
 *
 * <p>⚠️ `RegisterGuiLayersEvent` 是**模组总线（MOD bus）**事件，必须单独用
 * `bus = EventBusSubscriber.Bus.MOD` 的订阅者注册；挂在 Forge 总线上永远不会触发
 * （r350 提示面板不显示的根因就是这个——红光和提示一起哑掉）。
 */
@EventBusSubscriber(modid = "jujutsu_kaisen_cursed_paths", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class TojiOverlay {
    @SubscribeEvent public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID, "sukuna_toji_qte"),
            (graphics, delta) -> TojiQteVisuals.drawOverlay(graphics, graphics.guiWidth(), graphics.guiHeight()));
    }

    private TojiOverlay() {}
}
