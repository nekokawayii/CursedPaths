package character.kurisu;

import character.CharacterSkill;
import java.util.List;

/** 来栖华 · 邪去悔天梯（雅各天梯）：11 式，编号 2800~2810。 */
public final class KurisuSkills {
    public static final int BAPTISM=2800, FLAME=2801, SPEARS=2802, GATE=2803, SWORD=2804,
        THUNDER=2805, STORM=2806, EXECUTE=2807, HORN=2808, SUPPER=2809, DOMAIN=2810;

    public static List<CharacterSkill> all() {
        return List.of(
            new CharacterSkill(BAPTISM, "雅各天梯·剥夺洗礼", "剥夺",
                "锁定前方单体，降下极细金色激光；命中强制解除目标全部增益（无下限/领域展延/无敌帧）并造成目标最大生命 15% 的真实伤害。",
                0xFFFFD77A, 600, 200, CharacterSkill.Shape.RAY,
                28, .6, 10, 0, 0, 1, 0, 0, 0),
            new CharacterSkill(FLAME, "赎罪之炎", "赎罪",
                "拍地燃起金色火海：范围内敌人持续掉血，且期间所有治疗归零。",
                0xFFFFE9A8, 500, 260, CharacterSkill.Shape.BURST,
                8, 6, 180, 0, 0, 8, 0, 0, 0),
            new CharacterSkill(SPEARS, "圣光穿刺", "穿刺",
                "射出数根金色光枪自动追踪空中敌人，命中后强制取消飞行能力并钉向地面（关闭重力+下压）。",
                0xFFFFF1C4, 450, 240, CharacterSkill.Shape.RAY,
                24, 1.2, 25, 0, 0, 6, 0, 0, 0),
            new CharacterSkill(GATE, "天国之门·断章", "断章",
                "身前展开金色法阵：飞来的弹道被湮灭；近战劈砍者武器耐久归零（不可损坏物品改为 10 秒失效）。",
                0xFFFFD77A, 400, 280, CharacterSkill.Shape.BURST,
                6, 2.2, 180, 0, 0, 1, 0, 0, 0),
            new CharacterSkill(SWORD, "裁决光剑", "光剑",
                "凝聚光刃：15 秒内近战改为魔法伤害（无视护甲），对咒灵 ×2。",
                0xFFFFE9A8, 350, 300, CharacterSkill.Shape.RAY,
                4, .5, 30, 0, 0, 1, 0, 0, 0),
            new CharacterSkill(THUNDER, "神罚·落雷", "落雷",
                "从天空引下金色落雷劈向准星：高额雷击伤害 + 2 秒强制眩晕（关 AI / 零位移）。",
                0xFFFFF1C4, 550, 320, CharacterSkill.Shape.RAY,
                30, 3, 20, 0, 0, 4, 0, 0, 0),
            new CharacterSkill(STORM, "净化风暴", "净化",
                "以自身为中心旋转释放金色光刃风暴：击退范围内所有敌人，并清空他们身上全部增益/减益。",
                0xFFFFD77A, 700, 360, CharacterSkill.Shape.BURST,
                10, 8, 180, 0, 0, 16, 0, 0, 0),
            new CharacterSkill(EXECUTE, "破魔·重击", "破魔",
                "对准星前方 3 格内、未在施术（或术式熔断）的敌人造成巨额斩杀伤害。",
                0xFFFFE9A8, 650, 400, CharacterSkill.Shape.RAY,
                3, 1, 30, 0, 0, 1, 0, 0, 0),
            new CharacterSkill(HORN, "邪去悔天梯", "号角",
                "长按释放：举起金色号角吹奏，移速 -50%、无法普攻；天空裂开，圣光柱持续轰击准星区域 3 秒。",
                0xFFFFF6D8, 900, 600, CharacterSkill.Shape.RAY,
                32, 4, 8, 0, 0, 32, 0, 0, 0),
            new CharacterSkill(SUPPER, "极之番·最后的晚餐", "晚餐",
                "蓄力 3 秒给场上敌人打上原罪标记，随后引爆：全体受到最大生命 20% 真实伤害，召唤物被净化消失。",
                0xFFFFD77A, 1500, 1200, CharacterSkill.Shape.BURST,
                32, 8, 180, 0, 0, 32, 0, 0, 0),
            new CharacterSkill(DOMAIN, "领域展开·天国之门", "天国",
                "展开开放式纯白领域（半径 30）：领域内敌人所有生得术式被封印（只能平A），来栖华平A伤害翻倍。",
                0xFFFFF6D8, 2000, 1800, CharacterSkill.Shape.BURST,
                30, 8, 180, 0, 0, 32, 0, 0, 0)
        );
    }
    private KurisuSkills() {}
}
