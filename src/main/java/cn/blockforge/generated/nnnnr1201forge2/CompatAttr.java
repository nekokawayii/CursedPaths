package cn.blockforge.generated.nnnnr1201forge2;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

/** 1.21 迁移兼容：属性修饰符从 UUID 改成 ResourceLocation 后，把旧 UUID 稳定映射成同名 id。 */
public final class CompatAttr {
    private static final String NS = GeneratedMod.MOD_ID;

    public static ResourceLocation id(UUID uuid) {
        return ResourceLocation.fromNamespaceAndPath(NS, uuid.toString());
    }

    public static ResourceLocation id(String name) {
        StringBuilder b = new StringBuilder();
        for (char c : name.toLowerCase(java.util.Locale.ROOT).toCharArray())
            b.append(Character.isLetterOrDigit(c) ? c : '_');
        String s = b.toString().replaceAll("_+", "_");
        if (s.isEmpty()) s = "modifier";
        return ResourceLocation.fromNamespaceAndPath(NS, s);
    }

    private CompatAttr() { }
}
