package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** 配方由服务端加载并同步；关闭时九格泥土不产生控制器。 */
public final class ControllerCraftingCondition implements ICondition {
    /** 1.21：条件改为 MapCodec 注册（原来是 IConditionSerializer）。 */
    public static final MapCodec<ControllerCraftingCondition> CODEC = MapCodec.unit(new ControllerCraftingCondition());

    public static final DeferredRegister<MapCodec<? extends ICondition>> SERIALIZERS =
        DeferredRegister.create(NeoForgeRegistries.CONDITION_SERIALIZERS, GeneratedMod.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<ControllerCraftingCondition>> ENTRY =
        SERIALIZERS.register("controller_crafting_enabled", () -> CODEC);

    @Override public MapCodec<? extends ICondition> codec() { return CODEC; }

    @Override public boolean test(IContext context) {
        return GrowthConfig.SPEC.isLoaded() && GrowthConfig.CONTROLLER_CRAFTING.get();
    }

    public static void register() {
        SERIALIZERS.register(GeneratedMod.MOD_BUS);
    }

    private ControllerCraftingCondition() { }
}
