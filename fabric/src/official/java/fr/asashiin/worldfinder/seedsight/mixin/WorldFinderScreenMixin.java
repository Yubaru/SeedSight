package fr.asashiin.worldfinder.client;

import fr.asashiin.worldfinder.api.world.WorldDimension;
import fr.asashiin.worldfinder.common.map.StructureMarker;
import fr.asashiin.worldfinder.seedsight.VisitedPois;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Draws a tiny vanilla-style check badge without changing WorldFinder's layout. */
@Mixin(targets = "fr.asashiin.worldfinder.client.WorldFinderScreen")
public abstract class WorldFinderScreenMixin {
    @Shadow private List<?> renderedStructureMarkers;
    @Shadow private WorldDimension dimension;
    @Shadow private int worldToScreenX(double blockX) { throw new AssertionError(); }
    @Shadow private int worldToScreenY(double blockZ) { throw new AssertionError(); }

    @Inject(method = "drawRenderedStructureIcons", at = @At("TAIL"))
    private void seedsight$drawVisitedChecks(WorldFinderGuiGraphics graphics, CallbackInfo callback) {
        String dimensionId = "minecraft:" + dimension.serializedName();
        for (Object rendered : renderedStructureMarkers) {
            StructureMarker marker = ((RenderedStructureMarkerAccessor) rendered).seedsight$marker();
            int blockX = marker.position().blockX();
            int blockZ = marker.position().blockZ();
            if (!VisitedPois.isVisited(dimensionId, blockX, blockZ)) continue;

            int x = worldToScreenX(blockX) + 3;
            int y = worldToScreenY(blockZ) - 8;
            graphics.fill(x - 1, y - 1, x + 7, y + 7, 0xD9000000);
            graphics.fill(x, y + 3, x + 2, y + 5, 0xFF80C76A);
            graphics.fill(x + 2, y + 4, x + 4, y + 6, 0xFF80C76A);
            graphics.fill(x + 4, y + 1, x + 6, y + 5, 0xFF80C76A);
        }
    }
}
