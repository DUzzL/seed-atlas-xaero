package org.seedatlas.xaero.config;

import java.nio.file.Files;
import java.util.List;
import org.seedatlas.xaero.config.SeedAtlasConfig.StructureKey;

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
            check(!config.hideCompletedStructures(), "Old configs must show completed structures");
            var key = new StructureKey("server-a", 123, false, "minecraft:overworld", "village", -128, 256);
            check(config.setCompleted(key, true), "First completion");
            check(!config.setCompleted(key, true), "Completion must be idempotent");
            check(!config.hideCompletedStructures(), "Completing must not enable hiding");
            SeedAtlasConfigIO.save(file, config);
            var loaded = SeedAtlasConfigIO.load(file);
            check(loaded.isCompleted(key), "Completion survives reload");
            check(loaded.opacity() == 180 && loaded.structuresEnabled() && !loaded.markerEnabled("village"), "Preserve existing settings");
            check(loaded.seedProfile("server-a").seed() == 123, "Preserve seed profiles");
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

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
