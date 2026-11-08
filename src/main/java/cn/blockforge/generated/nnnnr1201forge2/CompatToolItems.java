package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.world.item.*;

/** 1.21 兼容：旧工具构造 (Tier,int damage,float speed,Properties) 的可继承基类（供匿名覆写使用）。 */
public final class CompatToolItems {
    public static class Axe extends AxeItem {
        public Axe(Tier t, int d, float s, Item.Properties p) { super(t, p.attributes(AxeItem.createAttributes(t, d, s))); }
    }
    public static class Pickaxe extends PickaxeItem {
        public Pickaxe(Tier t, int d, float s, Item.Properties p) { super(t, p.attributes(PickaxeItem.createAttributes(t, d, s))); }
    }
    public static class Hoe extends HoeItem {
        public Hoe(Tier t, int d, float s, Item.Properties p) { super(t, p.attributes(HoeItem.createAttributes(t, d, s))); }
    }
    public static class Shovel extends ShovelItem {
        public Shovel(Tier t, int d, float s, Item.Properties p) { super(t, p.attributes(ShovelItem.createAttributes(t, d, s))); }
    }
    private CompatToolItems() { }
}
