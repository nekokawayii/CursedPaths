package character.kurisu;

import character.CharacterDefinition;
import character.CharacterConfig;
import java.util.List;
import java.util.Map;

/** 来栖华：邪去悔天梯（雅各天梯）· 神圣审判 / 天使凭依。 */
public final class KurisuCharacter {
    public static CharacterDefinition create() {
        return new CharacterDefinition(
            "kurisu", "来栖华", "邪去悔天梯",
            "被天使「雅各天梯」凭依的女高中生：金色圣光、剥夺与净化。"
                + "代价是理智——每次净化/剥夺都会侵蚀她，理智过低会误伤同伴，归零则失控。吃甜食可以恢复理智。",
            CharacterDefinition.Family.CUSTOM, CharacterDefinition.Resource.CURSED_ENERGY, 0xFFFFD77A,
            List.of(KurisuSkills.BAPTISM, KurisuSkills.FLAME, KurisuSkills.SPEARS, KurisuSkills.GATE,
                KurisuSkills.SWORD, KurisuSkills.THUNDER, KurisuSkills.STORM, KurisuSkills.EXECUTE,
                KurisuSkills.HORN, KurisuSkills.SUPPER, KurisuSkills.DOMAIN),
            List.of(new CharacterDefinition.Chapter("天使凭依",10,"击败10名敌人，熟悉剥夺与赎罪之炎","咒力上限提高"),
                new CharacterDefinition.Chapter("圣裁",30,"累计击败30名敌人，掌握光剑与落雷","术式与近战成长"),
                new CharacterDefinition.Chapter("天国之门",60,"累计击败60名敌人，展开天国之门","咒力上限再次提高")),
            new CharacterConfig(
                new CharacterConfig.Wheel("来栖华", "邪去悔天梯", 0xE0161208, 0xE02B2416, 0xE0FFD77A, 0xB05A4A20, 0xFFFFF6D8),
                new CharacterConfig.Stats(2600, 20, 4, 10, 18),
                new CharacterConfig.Stats(500, 10, 2, 8, 0),
                KurisuSkills.all(),
                Map.of(KurisuSkills.BAPTISM, 0),
                Map.of())
        );
    }
    private KurisuCharacter() {}
}
