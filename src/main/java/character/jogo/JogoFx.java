package character.jogo;

/** 漏壶专用特效号（服务端发 Network.Effect，客户端 {@link JogoFxClient} 用 PhotonFx 绘制）。 */
public final class JogoFx {
    public static final int BUG=258, BUG_BURST=259, METEOR_TRAIL=260, METEOR_IMPACT=261, PILLAR=262, DOMAIN=263, DOMAIN_END=264,
            JIYAN_RIDE=265, JIYAN_IMPACT=266, JIYAN_SEA=267;
    public static final int FIRST=BUG, LAST=JIYAN_SEA;
    private JogoFx() {}
}
