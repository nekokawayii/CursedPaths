package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 附拳火焰粒子的客户端提供者：走贴图（`fist_flame_0.png`），不是程序化烟雾。 */
@Mod.EventBusSubscriber(modid = "jujutsu_kaisen_cursed_paths", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FistParticleClient {
    @SubscribeEvent
    public static void register(RegisterParticleProvidersEvent e) {
        e.registerSpriteSet(FistParticles.FIST_FLAME.get(), Provider::new);
    }

    private static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        Provider(SpriteSet sprites) { this.sprites = sprites; }
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                                 double x, double y, double z,
                                                 double dx, double dy, double dz) {
            return new Flame(level, x, y, z, dx, dy, dz, sprites);
        }
    }

    private static final class Flame extends TextureSheetParticle {
        private final SpriteSet sprites;
        Flame(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz);
            this.sprites = sprites;
            this.lifetime = 14 + level.random.nextInt(9);
            this.quadSize = 0.34f + level.random.nextFloat() * 0.20f;
            this.xd = dx * 0.08 + (level.random.nextDouble() - 0.5) * 0.028;
            this.yd = 0.018 + level.random.nextDouble() * 0.030;
            this.zd = dz * 0.08 + (level.random.nextDouble() - 0.5) * 0.028;
            this.gravity = -0.015f;
            this.friction = 0.96f;
            this.hasPhysics = false;
            this.setSpriteFromAge(sprites);
        }
        @Override public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
        @Override public void tick() {
            super.tick();
            this.setSpriteFromAge(sprites);
            if (this.age > this.lifetime / 2) this.alpha *= 0.955f;
        }
    }

    private FistParticleClient() { }
}
