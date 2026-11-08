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
    private CompatItem() { }
}
