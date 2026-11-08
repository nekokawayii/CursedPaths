package cn.blockforge.generated.nnnnr1201forge2;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;

/** 只探测可选模组，不链接第三方渲染类或依赖其混淆名。 */
@OnlyIn(Dist.CLIENT)
public final class MotionRenderCompat {
    public static boolean externalPlayerModels(){return ModList.get().isLoaded("yes_steve_model");}
    private MotionRenderCompat(){}
}
