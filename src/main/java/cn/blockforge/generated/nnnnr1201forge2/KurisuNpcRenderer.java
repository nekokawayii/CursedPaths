package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 来栖华 NPC：玩家模型 + 金白配色皮肤。 */
public final class KurisuNpcRenderer extends MobRenderer<KurisuNpcEntity, PlayerModel<KurisuNpcEntity>> {
    private static final ResourceLocation SKIN =
        new ResourceLocation("jujutsu_kaisen_cursed_paths", "textures/entity/kurisu_npc.png");
    public KurisuNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }
    @Override public ResourceLocation getTextureLocation(KurisuNpcEntity entity) { return SKIN; }
}
