package org.seedatlas.xaero.nativeapi;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A Minecraft release the engine can generate, named like the Seed Atlas app does: "1.20" stands
 * for the latest 1.20.x. Only 1.18 and newer are offered, because the terrain look relies on the
 * noise-based generation those versions share.
 */
public record GameVersion(String name) {
    /** Newest first, like the Seed Atlas version box; 1.19.2 is left out there as well. */
    public static final List<GameVersion> ALL = List.of(
        new GameVersion("26.3"),
        new GameVersion("26.2"),
        new GameVersion("26.1.2"),
        new GameVersion("26.1.1"),
        new GameVersion("26.1"),
        new GameVersion("1.21.11"),
        new GameVersion("1.21.10"),
        new GameVersion("1.21.9"),
        new GameVersion("1.21.8"),
        new GameVersion("1.21.7"),
        new GameVersion("1.21.6"),
        new GameVersion("1.21.5"),
        new GameVersion("1.21.4"),
        new GameVersion("1.21.3"),
        new GameVersion("1.21.2"),
        new GameVersion("1.21.1"),
        new GameVersion("1.20"),
        new GameVersion("1.19"),
        new GameVersion("1.18")
    );
    /** The version this build of the mod runs on, and the engine's newest. */
    public static final GameVersion NEWEST = ALL.getFirst();

    public GameVersion {
        Objects.requireNonNull(name, "name");
    }

    public boolean isNewest() {
        return this.equals(NEWEST);
    }

    /** Accepts the listed names and the patch names players know, e.g. "1.20.6" or "1.21". */
    public static Optional<GameVersion> parse(String input) {
        if (input == null) {
            return Optional.empty();
        }
        String name = input.trim();
        switch (name) {
            case "1.21", "1.21.0" -> name = "1.21.1";
            case "1.20.6", "1.20.5", "1.20.4", "1.20.3", "1.20.2", "1.20.1" -> name = "1.20";
            case "1.19.4", "1.19.3" -> name = "1.19";
            case "1.18.2", "1.18.1" -> name = "1.18";
            default -> { }
        }
        for (GameVersion version : ALL) {
            if (version.name.equals(name)) {
                return Optional.of(version);
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return this.name;
    }
}
