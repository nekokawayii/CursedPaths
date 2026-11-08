package cn.blockforge.generated.nnnnr1201forge2;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/** 1.21 兼容：DistExecutor 已移除，用运行期 dist 判断替代（lambda 延迟解析客户端类）。 */
public final class CompatClient {
    public static void runWhenClient(Runnable action) {
        if (FMLEnvironment.dist == Dist.CLIENT) action.run();
    }
    private CompatClient() { }
}
