package fr.asashiin.worldfinder.client;

import fr.asashiin.worldfinder.common.map.StructureMarker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "fr.asashiin.worldfinder.client.WorldFinderScreen$RenderedStructureMarker")
public interface RenderedStructureMarkerAccessor {
    @Accessor("marker")
    StructureMarker seedsight$marker();
}
