package fr.asashiin.worldfinder.seedsight;

import net.minecraft.client.Minecraft;

/** Shares the seed selected in WorldFinder with SeedSight's in-world tools. */
public final class SeedSightSeedState {
    private static Long selectedSeed;

    private SeedSightSeedState() {
    }

    public static void select(Long seed) {
        selectedSeed = seed;
    }

    public static Long current(Minecraft client) {
        if (client.getSingleplayerServer() != null) {
            return client.getSingleplayerServer().overworld().getSeed();
        }
        return selectedSeed;
    }

    public static void reset() {
        selectedSeed = null;
    }
}
