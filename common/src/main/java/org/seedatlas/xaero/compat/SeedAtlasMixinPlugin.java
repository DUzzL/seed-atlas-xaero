package org.seedatlas.xaero.compat;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.MixinService;

/**
 * Applies the Seed Atlas mixins only when the installed Xaero's World Map still matches them
 * (see {@link XaeroCompatibility}); otherwise none is applied and Xaero runs untouched.
 */
public final class SeedAtlasMixinPlugin implements IMixinConfigPlugin {
    private Boolean compatible;

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (this.compatible == null) {
            XaeroCompatibility.Report report = XaeroCompatibility.check(classSource());
            XaeroCompatibility.publish(report);
            log(report);
            this.compatible = report.compatible();
        }
        return this.compatible;
    }

    /**
     * Untransformed classes: this runs while mixins are being prepared, before any target loads.
     * Fabric's Mixin service serves untransformed bytecode, but the Forge (ModLauncher) and NeoForge
     * (FML) services refuse it with an IllegalArgumentException, and asking them for transformed
     * bytecode would apply mixins while this decides whether to apply its own. So the class files
     * are also read as plain resources from the game's class loaders.
     */
    static XaeroCompatibility.ClassSource classSource() {
        return classSource(MixinService.getService().getBytecodeProvider(), Thread.currentThread().getContextClassLoader());
    }

    /** Visible for the compatibility test, which stands in for the Forge and NeoForge services. */
    public static XaeroCompatibility.ClassSource classSource(IClassBytecodeProvider mixin, ClassLoader context) {
        ClassLoader own = SeedAtlasMixinPlugin.class.getClassLoader();
        return XaeroCompatibility.ClassSource.firstOf(
            name -> readFromMixin(mixin, name),
            context != null ? XaeroCompatibility.ClassSource.of(context) : name -> null,
            own != context ? XaeroCompatibility.ClassSource.of(own) : name -> null);
    }

    private static ClassNode readFromMixin(IClassBytecodeProvider mixin, String internalName) {
        try {
            return mixin.getClassNode(internalName.replace('/', '.'), false);
        } catch (ClassNotFoundException | IOException exception) {
            return null;
        } catch (IllegalArgumentException | UnsupportedOperationException unsupported) {
            // Forge and NeoForge: no untransformed bytecode from this service; the class loaders answer.
            return null;
        }
    }

    private static void log(XaeroCompatibility.Report report) {
        ILogger logger = MixinService.getService().getLogger("seedatlas_xaero");
        if (!report.compatible()) {
            logger.error("Seed Atlas stays disabled: the installed Xaero's World Map changed what it relies on. "
                + "Xaero's map works normally; update Seed Atlas to restore the seed layer.");
            report.problems().forEach(problem -> logger.error("  - {}", problem));
        }
        report.warnings().forEach(warning -> logger.warn("Seed Atlas optional feature unavailable: {}", warning));
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
