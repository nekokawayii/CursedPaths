package cn.blockforge.generated.nnnnr1201forge2;

/** 虚空控制器界面：只有客户端会加载，避免专用服务端在物品注册时解析 Screen。 */
public final class VoidControllerClient {
    public static void open(){
        net.minecraft.client.Minecraft.getInstance().setScreen(new VoidControllerScreen());
    }
    private VoidControllerClient(){}
}
