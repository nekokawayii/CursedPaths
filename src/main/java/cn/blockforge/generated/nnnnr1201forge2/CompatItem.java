package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** 1.21 兼容：旧 CompatItem.save(ItemStack) 的替代（1.21 需要 HolderLookup.Provider）。 */
public final class CompatItem {
    public static CompoundTag save(ItemStack stack) {
        return stack.save(RegistryAccess.EMPTY);
    }
    /** 1.21：CompatItem.load(CompoundTag) 已移除，改用 parseOptional(Provider, tag)。 */
    public static ItemStack load(CompoundTag tag) {
        return ItemStack.parseOptional(RegistryAccess.EMPTY, tag);
    }
    /** 1.21：setHoverName 已移除，改用 DataComponents.CUSTOM_NAME。 */
    public static void setName(ItemStack stack, net.minecraft.network.chat.Component name) {
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, name);
    }
    /** 1.21：setTag 已移除，改用 CUSTOM_DATA 组件。 */
    public static void setTag(ItemStack stack, CompoundTag tag) {
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }
    private CompatItem() { }
}
