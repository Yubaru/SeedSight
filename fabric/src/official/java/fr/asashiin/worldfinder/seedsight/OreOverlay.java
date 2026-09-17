package fr.asashiin.worldfinder.seedsight;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/** Toggleable, seed-derived diamond and ancient-debris overlay. */
final class OreOverlay {
    private static final int DIAMOND_STROKE = 0xFF42E8F5;
    private static final int DIAMOND_FILL = 0x4032CBD6;
    private static final int DEBRIS_STROKE = 0xFFFF8A3D;
    private static final int DEBRIS_FILL = 0x405E2D18;
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("seedsight", "controls"));
    private static final KeyMapping TOGGLE = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.seedsight.ore_overlay", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, CATEGORY));

    private OrePredictionEngine engine;
    private boolean enabled;
    private CacheKey cacheKey;
    private List<OrePredictionEngine.PredictedOre> predictions = List.of();

    void tick(Minecraft client) {
        while (TOGGLE.consumeClick()) {
            toggle(client);
        }
        if (!enabled || client.level == null || client.player == null) return;

        Long seed = SeedSightSeedState.current(client);
        if (seed == null) {
            enabled = false;
            message(client, "SeedSight ore overlay needs a seed. Open the map with M and enter one first.");
            return;
        }
        BlockPos player = client.player.blockPosition();
        CacheKey nextKey = new CacheKey(seed, client.level.dimension().identifier(),
                player.getX() >> 4, player.getY() >> 4, player.getZ() >> 4,
                client.level.getGameTime() / 40L);
        if (!nextKey.equals(cacheKey)) {
            try {
                if (engine == null) engine = new OrePredictionEngine();
                predictions = engine.predict(seed, client.level, player);
                cacheKey = nextKey;
            } catch (RuntimeException exception) {
                enabled = false;
                predictions = List.of();
                message(client, "SeedSight could not initialize ore prediction; see the game log.");
                SeedSightWaypointClient.LOGGER.error("Could not initialize the ore overlay", exception);
                return;
            }
        }
        for (OrePredictionEngine.PredictedOre prediction : predictions) {
            GizmoStyle style = style(prediction);
            Gizmos.cuboid(prediction.pos(), style).setAlwaysOnTop();
        }
    }

    void reset() {
        enabled = false;
        cacheKey = null;
        predictions = List.of();
    }

    private void toggle(Minecraft client) {
        enabled = !enabled;
        cacheKey = null;
        if (enabled && SeedSightSeedState.current(client) == null) {
            enabled = false;
            message(client, "SeedSight ore overlay needs a seed. Open the map with M and enter one first.");
            return;
        }
        String target = client.level != null && client.level.dimension() == net.minecraft.world.level.Level.NETHER
                ? "ancient debris" : "diamonds";
        message(client, "SeedSight ore overlay " + (enabled ? "enabled (" + target + ", 48-block radius)." : "disabled."));
    }

    private static GizmoStyle style(OrePredictionEngine.PredictedOre prediction) {
        boolean diamond = prediction.kind() == OrePredictionEngine.OreKind.DIAMOND;
        int stroke = diamond ? DIAMOND_STROKE : DEBRIS_STROKE;
        int fill = diamond ? DIAMOND_FILL : DEBRIS_FILL;
        return prediction.confirmed()
                ? GizmoStyle.strokeAndFill(stroke, 2.5F, fill | 0x30000000)
                : GizmoStyle.strokeAndFill(stroke, 1.5F, fill);
    }

    private static void message(Minecraft client, String text) {
        if (client.player != null) client.player.sendSystemMessage(Component.literal(text));
    }

    private record CacheKey(long seed, Identifier dimension, int chunkX, int sectionY, int chunkZ,
            long refresh) { }
}
