package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/** 咏唱显示由服务端统一决定，所有在线玩家接收同一条词。 */
public final class TechniqueConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.ConfigValue<String> DISPLAY;
    private static final ForgeConfigSpec.IntValue CHANT_GAP,MAX_RATE;
    private static final ForgeConfigSpec.BooleanValue SLASH_PIERCE;
    static {
        var b=new ForgeConfigSpec.Builder();
        DISPLAY=b.comment("咏唱显示方式：title=屏幕标题（默认），chat=聊天栏；所有在线玩家可见。").define("chantDisplay","title",v->v instanceof String s&&(s.equals("title")||s.equals("chat")));
        CHANT_GAP=b.comment("每句咏唱的等待刻数，20刻=1秒。").defineInRange("chantGapTicks",20,10,100);
        MAX_RATE=b.comment("长按普通解的最高每秒斩击数：1到30；从每秒1斩逐渐加速。").defineInRange("dismantleMaxPerSecond",30,1,30);
        SLASH_PIERCE=b.comment("剑气是否穿墙：true=穿过方块继续飞（默认），false=撞墙整道停下并在墙面留下斩痕。").define("slashPierceWalls",true);
        SPEC=b.build();
    }
    public static boolean chat(){return (SPEC.isLoaded()?DISPLAY.get():DISPLAY.getDefault()).equals("chat");}
    public static int gap(){return SPEC.isLoaded()?CHANT_GAP.get():CHANT_GAP.getDefault();}
    public static int rate(){return SPEC.isLoaded()?MAX_RATE.get():MAX_RATE.getDefault();}
    /** 剑气穿墙开关（配置文件 techniques 里的 slashPierceWalls）。 */
    public static boolean slashPierce(){return SPEC.isLoaded()?SLASH_PIERCE.get():true;}
    public static void register(){ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON,SPEC,"jujutsu_kaisen_cursed_paths-techniques.toml");}
    private TechniqueConfig(){}
}
