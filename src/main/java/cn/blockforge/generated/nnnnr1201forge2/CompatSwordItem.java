package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/** 1.21 兼容：旧版 SwordItem(Tier,int damage,float speed,Properties) 的写法保留。 */
public class CompatSwordItem extends SwordItem {
    public CompatSwordItem(Tier tier, int damage, float speed, Item.Properties props) {
        super(tier, props.attributes(SwordItem.createAttributes(tier, damage, speed)));
    }
}
