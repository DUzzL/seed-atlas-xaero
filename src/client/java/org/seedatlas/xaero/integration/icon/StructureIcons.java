package org.seedatlas.xaero.integration.icon;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener.SharedState;
import org.seedatlas.xaero.config.MarkerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Measures each resource once per reload; map and settings share the same geometry. */
public final class StructureIcons {
    private static final Logger LOGGER = LoggerFactory.getLogger(StructureIcons.class);
    private static final Map<String, Icon> DEFAULTS = defaults();
    private static volatile Map<String, Icon> icons = DEFAULTS;

    private StructureIcons() { }

    public record Icon(Identifier texture, IconLayout layout) { }

    private static Map<String, Icon> defaults() {
        Map<String, Icon> result = new HashMap<>();
        for (MarkerType type : MarkerType.all()) {
            result.put(type.id(), fallback(type.id()));
        }
        result.put("camp_special", fallback("camp_special"));
        return Map.copyOf(result);
    }

    private static Icon fallback(String id) {
        return new Icon(Identifier.fromNamespaceAndPath("seedatlas_xaero", "textures/structure/" + id + ".png"),
            new IconLayout(20, 20, 0, 0, 20, 20));
    }

    public static Icon get(String id) {
        return icons.getOrDefault(id, DEFAULTS.get("camp"));
    }

    public static void initialize() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
            Identifier.fromNamespaceAndPath("seedatlas_xaero", "structure_icons"),
            new SimpleReloadListener<Map<String, Icon>>() {
                @Override
                protected Map<String, Icon> prepare(SharedState state) {
                    Map<String, Icon> loaded = new HashMap<>();
                    DEFAULTS.forEach((id, fallback) -> {
                        try (var stream = state.resourceManager().open(fallback.texture());
                             var image = NativeImage.read(stream)) {
                            loaded.put(id, new Icon(fallback.texture(),
                                IconLayout.measure(image.getWidth(), image.getHeight(), image::getPixel)));
                        } catch (IOException | RuntimeException exception) {
                            LOGGER.warn("Could not measure structure icon {}", id, exception);
                            loaded.put(id, fallback);
                        }
                    });
                    return Map.copyOf(loaded);
                }

                @Override
                protected void apply(Map<String, Icon> loaded, SharedState state) {
                    icons = loaded;
                }
            });
    }
}
