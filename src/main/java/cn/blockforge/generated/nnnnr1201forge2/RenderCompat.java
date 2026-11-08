package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;

/** 1.21：buildOrThrow() 在无顶点时会抛「BufferBuilder was empty」；
 *  很多特效批次可能为空（没有目标/被裁剪），这里改用 build() 并在空批次时直接跳过。 */
public final class RenderCompat {
    public static void draw(BufferBuilder builder) {
        MeshData mesh = builder.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
    }
    private RenderCompat() { }
}
