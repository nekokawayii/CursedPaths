package character.kurisu;

/** 来栖华的特效号：PLAY/STOP 驱动 Photon 时间轴与自绘层，HUD 同步理智。 */
public final class KurisuFx {
    /** 粒子时间轴：size=技能号，seed=变体（{@link KurisuHorn#V_READY} 等）。 */
    public static final int PLAY=800, STOP=801, HUD=802;
    /** 长按咏唱：背后羽翼 + 金色包裹（自绘层，owner=玩家 id）。 */
    public static final int WINGS=803;
    /** 松手圣光柱：地面金色法阵 + 天空裂口（自绘层，position=落点）。 */
    public static final int PILLAR=804;
    public static final int FIRST=PLAY, LAST=PILLAR;
    private KurisuFx() {}
}
