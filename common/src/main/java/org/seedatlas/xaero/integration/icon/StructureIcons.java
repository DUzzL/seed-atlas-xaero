package org.seedatlas.xaero.integration.icon;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.seedatlas.xaero.config.MarkerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Marker icons. Most show a Minecraft block or item, read from the game's own (or a resource
 * pack's) textures at runtime and never bundled; the rest are Seed Atlas pixel art. Each resource
 * is measured once per reload, and map and settings share the same geometry.
 */
public final class StructureIcons {
    private static final Logger LOGGER = LoggerFactory.getLogger(StructureIcons.class);
    /** Cracked stone bricks with the moss of mossy stone bricks; vanilla has no such block. */
    private static final Identifier CRACKED_MOSSY_STONE_BRICKS =
        Identifier.fromNamespaceAndPath("seedatlas_xaero", "dynamic/cracked_mossy_stone_bricks");
    private static final Map<String, Source> SOURCES = sources();
    private static final Map<String, Icon> DEFAULTS = defaults();
    private static volatile Map<String, Icon> icons = DEFAULTS;

    private StructureIcons() { }

    /** {@code block} icons are full squares and get a dark frame on the map. */
    public record Icon(Identifier texture, IconLayout layout, boolean block) { }

    private record Source(Identifier texture, boolean block) {
        static Source own(String id) {
            return new Source(Identifier.fromNamespaceAndPath("seedatlas_xaero", "textures/structure/" + id + ".png"), false);
        }

        static Source item(String name) {
            return new Source(Identifier.withDefaultNamespace("textures/item/" + name + ".png"), false);
        }

        static Source block(String name) {
            return new Source(Identifier.withDefaultNamespace("textures/block/" + name + ".png"), true);
        }
    }

    private static Map<String, Source> sources() {
        Map<String, Source> result = new HashMap<>();
        result.put("camp", Source.own("camp"));
        result.put("camp_special", Source.own("camp_special"));
        result.put("ancient_city", Source.item("echo_shard"));
        result.put("bastion", Source.block("cracked_polished_blackstone_bricks"));
        result.put("chambers", Source.item("trial_key"));
        result.put("desert", Source.block("chiseled_sandstone"));
        result.put("endcity", Source.block("purpur_block"));
        result.put("end_ship", Source.item("elytra"));
        result.put("fortress", Source.block("nether_bricks"));
        result.put("gateway", Source.item("ender_pearl"));
        result.put("geode", Source.item("amethyst_shard"));
        result.put("hut", Source.own("hut"));
        result.put("igloo", Source.own("igloo"));
        result.put("jungle", Source.block("mossy_cobblestone"));
        result.put("mansion", Source.item("totem_of_undying"));
        result.put("mineshaft", Source.block("rail"));
        result.put("monument", Source.block("prismarine_bricks"));
        result.put("orevein", Source.item("raw_iron"));
        result.put("orevein_copper", Source.item("raw_copper"));
        result.put("outpost", Source.item("crossbow_standby"));
        result.put("portal", Source.own("portal"));
        result.put("ruins", new Source(CRACKED_MOSSY_STONE_BRICKS, true));
        result.put("shipwreck", Source.item("oak_boat"));
        result.put("slime", Source.item("slime_ball"));
        result.put("spawn", Source.item("compass_00"));
        result.put("stronghold", Source.item("ender_eye"));
        result.put("trails", Source.item("brush"));
        result.put("treasure", Source.item("heart_of_the_sea"));
        result.put("village", Source.item("bell"));
        result.put("well", Source.item("water_bucket"));
        for (MarkerType type : MarkerType.all()) {
            if (!result.containsKey(type.id())) throw new IllegalStateException("No icon for " + type.id());
        }
        return Map.copyOf(result);
    }

    private static Map<String, Icon> defaults() {
        Map<String, Icon> result = new HashMap<>();
        SOURCES.forEach((id, source) -> result.put(id,
            new Icon(source.texture(), new IconLayout(16, 16, 0, 0, 16, 16), source.block())));
        return Map.copyOf(result);
    }

    public static Icon get(String id) {
        return icons.getOrDefault(id, DEFAULTS.get("camp"));
    }

    public static PreparableReloadListener reloadListener() {
        return new SimplePreparableReloadListener<Prepared>() {
                @Override
                protected Prepared prepare(ResourceManager resources, ProfilerFiller profiler) {
                    NativeImage ruins = crackedMossyStoneBricks(resources);
                    Map<String, Icon> loaded = new HashMap<>();
                    SOURCES.forEach((id, source) -> {
                        if (source.texture().equals(CRACKED_MOSSY_STONE_BRICKS)) {
                            loaded.put(id, ruins == null ? DEFAULTS.get(id)
                                : new Icon(source.texture(), measure(ruins), true));
                            return;
                        }
                        try (var stream = resources.open(source.texture());
                             var image = NativeImage.read(stream)) {
                            loaded.put(id, new Icon(source.texture(), measure(image), source.block()));
                        } catch (IOException | RuntimeException exception) {
                            LOGGER.warn("Could not measure structure icon {}", id, exception);
                            loaded.put(id, DEFAULTS.get("camp"));
                        }
                    });
                    return new Prepared(Map.copyOf(loaded), ruins);
                }

                @Override
                protected void apply(Prepared prepared, ResourceManager resources, ProfilerFiller profiler) {
                    if (prepared.ruins() != null) {
                        Minecraft.getInstance().getTextureManager().register(CRACKED_MOSSY_STONE_BRICKS,
                            new DynamicTexture(() -> "Seed Atlas cracked mossy stone bricks", prepared.ruins()));
                    }
                    icons = prepared.icons();
                }
            };
    }

    private record Prepared(Map<String, Icon> icons, NativeImage ruins) { }

    /** Animated textures stack their frames vertically; the icon shows the first one. */
    private static IconLayout measure(NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight() > width && image.getHeight() % width == 0 ? width : image.getHeight();
        IconLayout frame = IconLayout.measure(width, height, image::getPixel);
        return new IconLayout(width, image.getHeight(), frame.u(), frame.v(), frame.width(), frame.height());
    }

    /** Cracked stone bricks, with every pixel where mossy stone bricks differ from stone bricks. */
    private static NativeImage crackedMossyStoneBricks(ResourceManager resources) {
        try (var cracked = read(resources, "cracked_stone_bricks");
             var mossy = read(resources, "mossy_stone_bricks");
             var plain = read(resources, "stone_bricks")) {
            int size = cracked.getWidth();
            NativeImage result = new NativeImage(size, size, false);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int pixel = cracked.getPixel(x, y);
                    if (mossy.getWidth() == size && plain.getWidth() == size
                        && mossy.getPixel(x, y) != plain.getPixel(x, y)) {
                        pixel = mossy.getPixel(x, y);
                    }
                    result.setPixel(x, y, pixel);
                }
            }
            return result;
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Could not compose the ocean ruin icon", exception);
            return null;
        }
    }

    private static NativeImage read(ResourceManager resources, String block) throws IOException {
        try (var stream = resources.open(Identifier.withDefaultNamespace("textures/block/" + block + ".png"))) {
            return NativeImage.read(stream);
        }
    }
}
