package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

/** 只替换探索护甲的穿戴贴图；耐久、防御、修理材料和物品注册名均不变。 */
public final class ExpeditionArmorItem extends ArmorItem {
    private final String texture;

    /** 1.21：ArmorItem 接收 Holder<ArmorMaterial>。 */
    public ExpeditionArmorItem(String id, Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
        this.texture = "jujutsu_kaisen_cursed_paths:textures/models/armor/" + id + "_layer_1.png";
    }

}
