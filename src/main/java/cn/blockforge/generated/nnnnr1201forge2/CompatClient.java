package cn.blockforge.generated.nnnnr1201forge2;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/** 1.21 兼容：DistExecutor 已移除，用运行期 dist 判断替代（lambda 延迟解析客户端类）。 */
public final class CompatClient {
    public static void runWhenClient(Runnable action) {
        if (FMLEnvironment.dist == Dist.CLIENT) action.run();
    }
    /** 反射打开客户端界面：调用方字节码里不能直接出现客户端类，否则服务端注册时会解析到 Screen。 */
    public static void openScreenReflect(String className) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        try {
            Class.forName(className).getMethod("open").invoke(null);
        } catch (Throwable t) {
            com.mojang.logging.LogUtils.getLogger().warn("客户端界面打开失败：{}", className, t);
        }
    }
    private CompatClient() { }
}
