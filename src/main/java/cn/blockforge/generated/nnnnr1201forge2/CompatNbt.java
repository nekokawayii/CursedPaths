package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** 1.21 兼容：旧 CompatNbt.read(ItemStack) 的只读替代（1.20 的 NBT 现在放在 CUSTOM_DATA 组件里）。 */
public final class CompatNbt {
    public static CompoundTag read(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.isEmpty() ? null : tag;
    }
    /** 读路径：返回空 tag 而不是 null，方便链式 getXxx。 */
    public static CompoundTag readOrEmpty(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
    /** 写路径：旧 getOrCreateTag().putXxx(...) 的替代，读-改-写回组件。 */
    public static void update(ItemStack stack, java.util.function.Consumer<CompoundTag> action) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, action);
    }
    private CompatNbt() { }
}
