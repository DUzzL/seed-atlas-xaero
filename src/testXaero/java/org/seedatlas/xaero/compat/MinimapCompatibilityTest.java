package org.seedatlas.xaero.compat;

import java.io.IOException;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Absence/changed hooks must disable only the optional Minimap integration, without loading it. */
public final class MinimapCompatibilityTest {
    public static void run() throws IOException {
        check(!MinimapCompatibility.compatible(name -> null), "Absent Minimap must be skipped");
        var fbo = new ClassNode();
        var support = new ClassNode();
        var render = new MethodNode(Opcodes.ACC_PUBLIC, "renderChunksToFBO", "()V", null, null);
        var draw = new MethodNode(Opcodes.ACC_PUBLIC, "drawMinimap", "()V", null, null);
        fbo.methods.add(render);
        support.methods.add(draw);
        var start = new LabelNode();
        var end = new LabelNode();
        String[][] locals = {{"matrixStack", "Lcom/mojang/blaze3d/vertex/PoseStack;"},
            {"renderPos", "Lnet/minecraft/world/phys/Vec3;"}, {"mapDimension", "Lnet/minecraft/resources/ResourceKey;"},
            {"xFloored", "I"}, {"zFloored", "I"}, {"maxVisibleLength", "D"}, {"radiusBlocks", "D"}};
        for (int i = 0; i < locals.length; i++) {
            render.localVariables.add(new LocalVariableNode(locals[i][0], locals[i][1], null, start, end, i));
        }
        render.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "xaero/lib/client/graphics/XaeroBufferProvider",
            "getBuffer", "(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/BufferBuilder;", false));
        render.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC,
            "xaero/hud/minimap/common/config/option/MinimapProfiledConfigOptions", "CHUNK_GRID", "Lxaero/lib/common/config/option/RangeConfigOption;"));
        draw.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
            "xaero/common/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider", "getRenderer",
            "(Ljava/util/function/Consumer;Lnet/minecraft/client/renderer/rendertype/RenderType;)Lxaero/common/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;", false));
        XaeroCompatibility.ClassSource fixture = name -> name.equals(MinimapCompatibility.FBO) ? fbo : support;
        check(MinimapCompatibility.compatible(fixture), "Known hooks should be accepted");
        var local = render.localVariables.removeLast();
        check(!MinimapCompatibility.compatible(fixture), "Missing captured local must disable hooks");
        render.localVariables.add(local);
        var point = render.instructions.getLast();
        render.instructions.remove(point);
        check(!MinimapCompatibility.compatible(fixture), "Missing foreground hook must disable integration");
        render.instructions.add(point);
        draw.instructions.clear();
        check(!MinimapCompatibility.compatible(fixture), "Changed terrain hook must disable integration");
        String path = System.getProperty("seedatlas.minimapJar");
        if (path != null) {
            try (JarFile jar = new JarFile(path)) {
                check(MinimapCompatibility.compatible(name -> {
                    var entry = jar.getJarEntry(name + ".class");
                    if (entry == null) return null;
                    try (var input = jar.getInputStream(entry)) {
                        var node = new ClassNode();
                        new ClassReader(input).accept(node, 0);
                        return node;
                    } catch (IOException exception) { throw new java.io.UncheckedIOException(exception); }
                }), "Installed Minimap must match injection targets: " + path);
            }
            System.out.println("Minimap jar render hooks verified: " + path);
        }
        System.out.println("Minimap optional compatibility checks passed");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
