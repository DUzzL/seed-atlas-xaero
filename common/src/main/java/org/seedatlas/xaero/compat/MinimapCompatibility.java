package org.seedatlas.xaero.compat;

import java.util.Map;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Optional targets are checked as bytes, before Mixin can load an absent or changed Minimap. */
public final class MinimapCompatibility {
    public static final String FBO = "xaero/common/minimap/render/MinimapFBORenderer";
    public static final String SUPPORT = "xaero/common/mods/SupportXaeroWorldmap";
    private MinimapCompatibility() { }

    public static boolean compatible(XaeroCompatibility.ClassSource source) {
        var fbo = source.read(FBO);
        var support = source.read(SUPPORT);
        if (fbo == null || support == null) return false;
        MethodNode render = fbo.methods.stream().filter(m -> m.name.equals("renderChunksToFBO")).findFirst().orElse(null);
        MethodNode draw = support.methods.stream().filter(m -> m.name.equals("drawMinimap")).findFirst().orElse(null);
        if (render == null || draw == null || render.localVariables == null) return false;
        var locals = Map.of("matrixStack", "Lcom/mojang/blaze3d/vertex/PoseStack;",
            "renderPos", "Lnet/minecraft/world/phys/Vec3;", "mapDimension", "Lnet/minecraft/resources/ResourceKey;",
            "xFloored", "I", "zFloored", "I", "maxVisibleLength", "D", "radiusBlocks", "D");
        if (!locals.entrySet().stream().allMatch(e -> render.localVariables.stream()
            .anyMatch(l -> l.name.equals(e.getKey()) && l.desc.equals(e.getValue())))) return false;
        boolean background = false, foreground = false, terrain = false;
        for (var instruction : render.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals("xaero/lib/client/graphics/XaeroBufferProvider")
                && call.name.equals("getBuffer") && call.desc.equals("(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/BufferBuilder;")) background = true;
            if (instruction instanceof FieldInsnNode field && field.owner.equals("xaero/hud/minimap/common/config/option/MinimapProfiledConfigOptions")
                && field.name.equals("CHUNK_GRID") && field.desc.equals("Lxaero/lib/common/config/option/RangeConfigOption;")) foreground = true;
        }
        for (var instruction : draw.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals("xaero/common/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider")
                && call.name.equals("getRenderer") && call.desc.equals("(Ljava/util/function/Consumer;Lnet/minecraft/client/renderer/rendertype/RenderType;)Lxaero/common/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;")) terrain = true;
        }
        return background && foreground && terrain;
    }
}
