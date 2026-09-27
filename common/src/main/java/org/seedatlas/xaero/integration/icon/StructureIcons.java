package org.seedatlas.xaero.integration.icon;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.resources.Identifier;
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

    public static PreparableReloadListener reloadListener() {
        return new SimplePreparableReloadListener<Map<String, Icon>>() {
                @Override
                protected Map<String, Icon> prepare(ResourceManager resources, ProfilerFiller profiler) {
                    Map<String, Icon> loaded = new HashMap<>();
                    DEFAULTS.forEach((id, fallback) -> {
                        try (var stream = resources.open(fallback.texture());
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
                protected void apply(Map<String, Icon> loaded, ResourceManager resources, ProfilerFiller profiler) {
                    icons = loaded;
                }
            };
    }
}
