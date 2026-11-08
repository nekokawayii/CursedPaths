package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.Mod;

/** 仅为创造测试符放行右键，熔断或动作锁定不影响恢复入口。 */
@Mod.EventBusSubscriber(modid="jujutsu_kaisen_cursed_paths",value=Dist.CLIENT)
public final class CreativeRestorerClient {
    @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true)
    public static void use(InputEvent.InteractionKeyMappingTriggered e){
        var p=Minecraft.getInstance().player;
        if(e.isUseItem()&&p!=null&&p.isCreative()&&p.getItemInHand(e.getHand()).getItem() instanceof CreativeRestorer){e.setCanceled(false);e.setSwingHand(false);}
    }
    private CreativeRestorerClient(){}
}
