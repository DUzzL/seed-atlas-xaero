package org.seedatlas.xaero.integration;

import java.io.IOException;
import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Checks the external bytecode contracts that javac cannot validate for mixins. */
public final class XaeroCompatibilityTest {
    private static final String PROVIDER = "xaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider";
    private static final String RENDERER = "xaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer";

    public static void main(String[] args) throws IOException {
        ClassNode map = read("xaero/map/gui/GuiMap");
        for (String field : List.of("cameraX", "cameraZ", "scale")) {
            check(map.fields.stream().anyMatch(f -> f.name.equals(field) && f.desc.equals("D")),
                "GuiMap shadow changed: " + field);
        }
        for (String name : List.of("init", "tick", "removed")) {
            method(map, name, "()V");
        }
        MethodNode render = method(map, "extractRenderState",
            "(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V");
        int providers = 0;
        int draws = 0;
        for (AbstractInsnNode instruction : render.instructions) {
            if (!(instruction instanceof MethodInsnNode call)) continue;
            if (call.owner.equals("xaero/map/MapProcessor")
                && call.name.equals("getMultiTextureRenderTypeRenderers")
                && call.desc.equals("()L" + PROVIDER + ";")) {
                providers++;
                checkLocals(render, instruction);
            }
            if (call.owner.equals(PROVIDER) && call.name.equals("draw")
                && call.desc.equals("(L" + RENDERER + ";)V")) {
                draws++;
                if (draws == 2) checkLocals(render, instruction.getNext());
            }
        }
        check(providers == 1, "Expected one biome-background injection point, got " + providers);
        check(draws >= 2, "Missing terrain flush used by the biome-focus injection");
        ClassNode branch = read("xaero/map/region/texture/BranchTextureRenderer");
        check(branch.fields.stream().anyMatch(f -> f.name.equals("black4f")
            && f.desc.equals("Lorg/joml/Vector4f;")), "Unknown-terrain color shadow changed");
        check(branch.methods.stream().anyMatch(m -> m.name.equals("<init>")), "Missing branch renderer constructor");
        ClassNode texture = read("xaero/map/region/texture/BranchRegionTexture");
        check(texture.methods.stream().filter(m -> m.name.equals("readCacheData")
            && m.desc.endsWith(")V")).count() == 1, "Cache transparency injection changed");
        method(read("xaero/map/WorldMapClientOnly"), "loadLaterClientRender", "()V");
        System.out.println("Xaero mixin bytecode contracts passed");
    }

    private static void checkLocals(MethodNode method, AbstractInsnNode injection) {
        int index = method.instructions.indexOf(injection);
        for (String name : List.of("matrixStack", "flooredCameraX", "flooredCameraZ")) {
            String descriptor = name.equals("matrixStack") ? "Lcom/mojang/blaze3d/vertex/PoseStack;" : "I";
            long matches = method.localVariables.stream().filter(local -> local.name.equals(name)
                && local.desc.equals(descriptor)
                && method.instructions.indexOf(local.start) <= index
                && method.instructions.indexOf(local.end) > index).count();
            check(matches == 1, "Missing or ambiguous captured local at injection: " + name);
        }
    }

    private static ClassNode read(String name) throws IOException {
        try (var stream = XaeroCompatibilityTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            check(stream != null, "Missing Xaero class: " + name);
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, 0);
            return node;
        }
    }

    private static MethodNode method(ClassNode owner, String name, String descriptor) {
        return owner.methods.stream().filter(m -> m.name.equals(name) && m.desc.equals(descriptor))
            .findFirst().orElseThrow(() -> new AssertionError("Missing " + owner.name + "." + name + descriptor));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
