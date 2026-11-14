package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 附拳用的贴图粒子（用户要求：**用贴图加特效，不要纯特效**）。
 * 贴图 `textures/particle/fist_flame_0.png` 由 ComfyUI 生成后去掉黑底转成 alpha。
 */
public final class FistParticles {
    public static final DeferredRegister<ParticleType<?>> TYPES =
        DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, "jujutsu_kaisen_cursed_paths");
    public static final RegistryObject<SimpleParticleType> FIST_FLAME =
        TYPES.register("fist_flame", () -> new SimpleParticleType(false));

    public static void init() {
        TYPES.register(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
    }

    private FistParticles() { }
}
