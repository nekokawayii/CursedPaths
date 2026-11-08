package cn.blockforge.generated.nnnnr1201forge2;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/** 「法轮」的防具材质：**头盔**，戴在头上才有模型。
 *
 *  <p>1.21 起 {@link ArmorMaterial} 是 record，不能匿名继承；这里是数据构造。
 *  只占头部槽位，防御力很低但带 4 点盔甲韧性——它不是防具，而是召唤物/进度显示器。
 *  耐久仿照下界合金（不可损坏），修理用金锭。</p> */
public final class MahoragaArmor {
    private static final Map<ArmorItem.Type,Integer> DEFENSE=new EnumMap<>(ArmorItem.Type.class);
    static{
        DEFENSE.put(ArmorItem.Type.BOOTS,1);
        DEFENSE.put(ArmorItem.Type.LEGGINGS,2);
        DEFENSE.put(ArmorItem.Type.CHESTPLATE,3);
        DEFENSE.put(ArmorItem.Type.HELMET,2);
    }

    private static final ArmorMaterial MATERIAL = new ArmorMaterial(
        DEFENSE,
        22,
        SoundEvents.ARMOR_EQUIP_GOLD,
        () -> Ingredient.of(Items.GOLD_INGOT),
        List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID, "mahoraga_wheel"))),
        4f,
        0.1f);

    public static ArmorMaterial material(){return MATERIAL;}
    /** 1.21：ArmorItem 需要 Holder<ArmorMaterial>。 */
    public static net.minecraft.core.Holder<ArmorMaterial> materialHolder(){return net.minecraft.core.Holder.direct(MATERIAL);}
    private MahoragaArmor(){}
}
