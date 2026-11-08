package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.world.item.*;

/** 1.21 兼容：旧工具构造 (Tier,int damage,float speed,Properties) 的包装。 */
public final class CompatTools {
    public static AxeItem axe(Tier t, float d, float s, Item.Properties p) { return new AxeItem(t, p.attributes(AxeItem.createAttributes(t, d, s))); }
    public static PickaxeItem pickaxe(Tier t, float d, float s, Item.Properties p) { return new PickaxeItem(t, p.attributes(PickaxeItem.createAttributes(t, d, s))); }
    public static HoeItem hoe(Tier t, float d, float s, Item.Properties p) { return new HoeItem(t, p.attributes(HoeItem.createAttributes(t, d, s))); }
    public static ShovelItem shovel(Tier t, float d, float s, Item.Properties p) { return new ShovelItem(t, p.attributes(ShovelItem.createAttributes(t, d, s))); }
    private CompatTools() { }
}
