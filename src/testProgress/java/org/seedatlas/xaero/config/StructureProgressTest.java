package org.seedatlas.xaero.config;

import java.nio.file.Files;
import java.util.List;
import org.seedatlas.xaero.config.SeedAtlasConfig.StructureKey;
import org.seedatlas.xaero.nativeapi.Dimension;

/** Executable persistence regression; runs without opening a Minecraft client. */
public final class StructureProgressTest {
    public static void main(String[] args) throws Exception {
        var directory = Files.createTempDirectory("seedatlas-progress-test");
        var file = directory.resolve("config.json");
        try {
            Files.writeString(file, """
                {"version":5,"layer":{"enabled":true,"opacity":180},
                 "structures":{"enabled":true},"markers":{"village":false},
                 "seeds":{"server-a":{"input":"123","value":123,"label":"A"}}}
                """);
            var config = SeedAtlasConfigIO.load(file);
            check(!config.overlayExplored(), "Old configs keep predictions beneath explored terrain");
            check(config.setOverlayExplored(true), "Enable overlay above explored terrain");
            check(!config.setOverlayExplored(true), "Repeated overlay toggle is idempotent");
            check(!config.hideCompletedStructures(), "Old configs must show completed structures");
            var key = new StructureKey("server-a", 123, false, "minecraft:overworld", "village", -128, 256);
            check(config.setCompleted(key, true), "First completion");
            check(!config.setCompleted(key, true), "Completion must be idempotent");
            check(!config.hideCompletedStructures(), "Completing must not enable hiding");
            SeedAtlasConfigIO.save(file, config);
            var loaded = SeedAtlasConfigIO.load(file);
            check(loaded.overlayExplored(), "Overlay preference survives reload");
            loaded.setOverlayExplored(false);
            SeedAtlasConfigIO.save(file, loaded);
            check(!SeedAtlasConfigIO.load(file).overlayExplored(), "Overlay off survives reload");
            check(loaded.isCompleted(key), "Completion survives reload");
            check(loaded.opacity() == 180 && loaded.structuresEnabled() && !loaded.markerEnabled("village"), "Preserve existing settings");
            check(loaded.seedProfile("server-a").base().seed() == 123, "Preserve seed profiles");
            dimensionSeeds(file, loaded);
            for (var other : List.of(
                new StructureKey("server-b",123,false,"minecraft:overworld","village",-128,256),
                new StructureKey("server-a",124,false,"minecraft:overworld","village",-128,256),
                new StructureKey("server-a",123,true,"minecraft:overworld","village",-128,256),
                new StructureKey("server-a",123,false,"minecraft:the_nether","village",-128,256),
                new StructureKey("server-a",123,false,"minecraft:overworld","outpost",-128,256),
                new StructureKey("server-a",123,false,"minecraft:overworld","village",128,256),
                new StructureKey("server-a",123,false,"minecraft:overworld","village",-128,257))) {
                check(!loaded.isCompleted(other), "Do not share progress across identities: " + other);
            }
            loaded.setHideCompletedStructures(true);
            SeedAtlasConfigIO.save(file, loaded);
            loaded = SeedAtlasConfigIO.load(file);
            check(loaded.hideCompletedStructures() && loaded.isCompleted(key), "Filter persists independently");
            loaded.setBiomeHighlightEnabled(true);
            loaded.toggleHighlightedBiome(1);
            loaded.toggleHighlightedBiome(27);
            check(loaded.highlightedBiomeCount() == 2 && loaded.isBiomeHighlighted(27), "Highlight selection");
            check(!loaded.highlightSignature().isEmpty(), "An active highlight must invalidate map tiles");
            SeedAtlasConfigIO.save(file, loaded);
            loaded = SeedAtlasConfigIO.load(file);
            check(loaded.biomeHighlightEnabled()
                && loaded.isBiomeHighlighted(1) && loaded.isBiomeHighlighted(27)
                && !loaded.isBiomeHighlighted(2), "Highlight selection survives reload");
            check(loaded.toggleHighlightedBiome(1) && !loaded.isBiomeHighlighted(1), "Unselect a biome");
            check(loaded.clearHighlightedBiomes() && loaded.highlightedBiomeCount() == 0, "Clear the selection");
            check(loaded.highlightSignature().isEmpty(), "A cleared highlight must not invalidate map tiles");
            // These two selections collide under the previous 31-based rolling hash.
            loaded.addHighlightedBiome(1);
            loaded.addHighlightedBiome(32);
            var firstSelection = loaded.highlightSignature();
            loaded.clearHighlightedBiomes();
            loaded.addHighlightedBiome(2);
            loaded.addHighlightedBiome(1);
            var secondSelection = loaded.highlightSignature();
            check(!firstSelection.equals(secondSelection), "Different biome sets must invalidate cached colours");
            loaded.clearHighlightedBiomes();
            loaded.addHighlightedBiome(1);
            loaded.addHighlightedBiome(2);
            check(secondSelection.equals(loaded.highlightSignature()), "Selection order must not rebuild tiles");
            loaded.setBiomeHighlightEnabled(false);
            check(loaded.highlightSignature().isEmpty() && loaded.highlightedBiomeCount() == 2,
                "Disabling restores normal colours but remembers the selection");
            loaded.completedSnapshot().clear();
            check(loaded.isCompleted(key), "Snapshot cannot mutate live progress");
            check(loaded.setCompleted(key, false), "Reopen structure");
            SeedAtlasConfigIO.save(file, loaded);
            check(!SeedAtlasConfigIO.load(file).isCompleted(key), "Reopening survives reload");
            loaded.setCompleted(key, true);
            SeedAtlasConfigIO.save(file, loaded);
            String json = Files.readString(file).replace("\"completed\": [", "\"completed\": [null, {}, {\"seed\":123,\"x\":0,\"z\":0},");
            Files.writeString(file, json);
            check(SeedAtlasConfigIO.load(file).isCompleted(key), "Malformed entries must not lose valid progress");
            System.out.println("Structure progress persistence tests passed");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory.resolve("config.json.tmp"));
            Files.deleteIfExists(directory);
        }
    }

    /** Issue #4: the Nether and End may use their own seeds; the base seed covers the rest. */
    private static void dimensionSeeds(java.nio.file.Path file, SeedAtlasConfig config) throws Exception {
        var base = config.seedProfile("server-a");
        check(base.seedFor(Dimension.NETHER).seed() == 123 && base.ownSeed(Dimension.NETHER) == null,
            "Old profiles generate every dimension from their seed");
        config.putSeedProfile("server-a", base.withDimension(Dimension.NETHER, new SeedAtlasConfig.SeedValue("nether text", 77)));
        config.putSeedProfile("server-c", new SeedAtlasConfig.SeedProfile(null, null, new SeedAtlasConfig.SeedValue("9", 9), "C"));
        SeedAtlasConfigIO.save(file, config);
        String json = Files.readString(file);
        check(json.contains("\"value\": 123") && json.contains("\"dimensions\""),
            "The base seed keeps the location older releases read; dimension seeds are additive");
        var loaded = SeedAtlasConfigIO.load(file);
        var a = loaded.seedProfile("server-a");
        check(a.seedFor(Dimension.NETHER).seed() == 77 && a.seedFor(Dimension.NETHER).input().equals("nether text"),
            "Nether seed and its text input survive reload");
        check(a.seedFor(Dimension.OVERWORLD).seed() == 123 && a.seedFor(Dimension.END).seed() == 123,
            "Dimensions without their own seed follow the base seed");
        var c = loaded.seedProfile("server-c");
        check(c != null && c.base() == null && c.seedFor(Dimension.OVERWORLD) == null && c.seedFor(Dimension.END).seed() == 9,
            "A profile may know only one dimension's seed");
        var cleared = a.withDimension(Dimension.NETHER, null);
        check(cleared.ownSeed(Dimension.NETHER) == null && cleared.seedFor(Dimension.NETHER).seed() == 123,
            "Clearing a dimension seed falls back to the base seed");
        check(new SeedAtlasConfig.SeedProfile(null, null, null, "x").isEmpty(), "Empty profiles are recognisable");
        try {
            a.withDimension(Dimension.OVERWORLD, new SeedAtlasConfig.SeedValue("1", 1));
            throw new AssertionError("The Overworld must use the base seed");
        } catch (IllegalArgumentException expected) {
            // The base seed is the Overworld seed.
        }
        config.putSeedProfile("server-a", base);
        config.removeSeedProfile("server-c");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
