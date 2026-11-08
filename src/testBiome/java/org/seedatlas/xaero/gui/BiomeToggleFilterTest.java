package org.seedatlas.xaero.gui;

/** Checks the View Selected visibility rule without opening a Minecraft client. */
public final class BiomeToggleFilterTest {
    public static void main(String[] args) {
        check(BiomeToggleList.ViewFilter.accepts(true, false, false), "Full list keeps unhighlighted biomes");
        check(BiomeToggleList.ViewFilter.accepts(true, false, true), "Full list keeps highlighted biomes");
        check(!BiomeToggleList.ViewFilter.accepts(true, true, false), "View Selected hides unhighlighted biomes");
        check(BiomeToggleList.ViewFilter.accepts(true, true, true), "View Selected keeps highlighted biomes");
        check(!BiomeToggleList.ViewFilter.accepts(false, true, true), "Search still applies inside View Selected");
        check(!BiomeToggleList.ViewFilter.accepts(false, false, true), "Search still applies without View Selected");
        System.out.println("BiomeToggleFilterTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
