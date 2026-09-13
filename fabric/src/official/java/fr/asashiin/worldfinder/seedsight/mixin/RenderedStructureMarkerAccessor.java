package fr.asashiin.worldfinder.seedsight.mixin;

import fr.asashiin.worldfinder.common.map.StructureMarker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "fr.asashiin.worldfinder.client.WorldFinderScreen$RenderedStructureMarker", remap = false)
public interface RenderedStructureMarkerAccessor {
    @Accessor(value = "marker", remap = false)
    StructureMarker seedsight$marker();
}
