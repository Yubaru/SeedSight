package fr.asashiin.worldfinder.seedsight.mixin;

import fr.asashiin.worldfinder.api.world.WorldDimension;
import fr.asashiin.worldfinder.common.map.StructureMarker;
import fr.asashiin.worldfinder.seedsight.VisitedPois;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

/** Adds compact red-X and green-check badges without changing WorldFinder's layout. */
@Mixin(targets = "fr.asashiin.worldfinder.client.WorldFinderScreen", remap = false)
public abstract class WorldFinderScreenStatusMixin {
    private static final Logger SEEDSIGHT_LOGGER = LoggerFactory.getLogger("SeedSight/VisitedPoiBadges");
    private static final MethodHandle SEEDSIGHT_FILL = seedsight$resolveFill();
    private static boolean seedsight$reportedDrawFailure;

    @Shadow(remap = false) private List<?> renderedStructureMarkers;
    @Shadow(remap = false) private WorldDimension dimension;
    @Shadow(remap = false) private int worldToScreenX(double blockX) { throw new AssertionError(); }
    @Shadow(remap = false) private int worldToScreenY(double blockZ) { throw new AssertionError(); }

    @Inject(method = "drawRenderedStructureIcons", at = @At("TAIL"), remap = false)
    private void seedsight$drawVisitedStatus(@Coerce Object graphics, CallbackInfo callback) {
        if (SEEDSIGHT_FILL == null) return;
        String dimensionId = "minecraft:" + dimension.serializedName();

        for (Object rendered : renderedStructureMarkers) {
            StructureMarker marker = ((RenderedStructureMarkerAccessor) rendered).seedsight$marker();
            int blockX = marker.position().blockX();
            int blockZ = marker.position().blockZ();
            boolean visited = VisitedPois.isVisited(dimensionId, blockX, blockZ);
            boolean pulse = VisitedPois.isRecentlyChanged(dimensionId, blockX, blockZ);
            int x = worldToScreenX(blockX) + 4;
            int y = worldToScreenY(blockZ) - 8;
            seedsight$badge(graphics, x, y, visited, pulse);
        }
    }

    private static void seedsight$badge(Object graphics, int x, int y, boolean visited, boolean pulse) {
        if (pulse) seedsight$fill(graphics, x - 2, y - 2, x + 9, y + 9, visited ? 0x8F80C76A : 0x8FDF5B57);
        seedsight$fill(graphics, x - 1, y - 1, x + 8, y + 8, 0xE8000000);
        if (visited) {
            seedsight$fill(graphics, x, y + 3, x + 2, y + 5, 0xFF80C76A);
            seedsight$fill(graphics, x + 2, y + 4, x + 4, y + 6, 0xFF80C76A);
            seedsight$fill(graphics, x + 4, y + 1, x + 7, y + 4, 0xFF80C76A);
        } else {
            seedsight$fill(graphics, x, y, x + 2, y + 2, 0xFFDF5B57);
            seedsight$fill(graphics, x + 2, y + 2, x + 5, y + 5, 0xFFDF5B57);
            seedsight$fill(graphics, x + 5, y + 5, x + 7, y + 7, 0xFFDF5B57);
            seedsight$fill(graphics, x + 5, y, x + 7, y + 2, 0xFFDF5B57);
            seedsight$fill(graphics, x, y + 5, x + 2, y + 7, 0xFFDF5B57);
        }
    }

    private static MethodHandle seedsight$resolveFill() {
        try {
            Class<?> graphicsType = Class.forName("fr.asashiin.worldfinder.client.WorldFinderGuiGraphics");
            MethodType targetType = MethodType.methodType(void.class,
                    int.class, int.class, int.class, int.class, int.class);
            MethodHandle target = MethodHandles.privateLookupIn(graphicsType, MethodHandles.lookup())
                    .findVirtual(graphicsType, "fill", targetType);
            return target.asType(targetType.insertParameterTypes(0, Object.class));
        } catch (ReflectiveOperationException | RuntimeException e) {
            SEEDSIGHT_LOGGER.warn("Could not initialize visited POI badge rendering", e);
            return null;
        }
    }

    private static void seedsight$fill(Object graphics, int left, int top, int right, int bottom, int color) {
        try {
            SEEDSIGHT_FILL.invokeExact(graphics, left, top, right, bottom, color);
        } catch (Throwable failure) {
            if (!seedsight$reportedDrawFailure) {
                seedsight$reportedDrawFailure = true;
                SEEDSIGHT_LOGGER.warn("Could not draw visited POI badges", failure);
            }
        }
    }
}
