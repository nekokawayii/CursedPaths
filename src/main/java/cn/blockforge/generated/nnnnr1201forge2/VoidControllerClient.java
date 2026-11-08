package cn.blockforge.generated.nnnnr1201forge2;

/** 只在客户端被反射加载，避免服务端在注册物品时解析到 Screen 类。 */
public final class VoidControllerClient {
    public static void open() {
        net.minecraft.client.Minecraft.getInstance().setScreen(new VoidControllerScreen());
    }
    private VoidControllerClient() { }
}
