package fr.asashiin.worldfinder.seedsight.mixin;

import fr.asashiin.worldfinder.seedsight.SeedSightSeedState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "fr.asashiin.worldfinder.client.WorldFinderScreen", remap = false)
abstract class WorldFinderSeedMixin {
    @Inject(method = "changeActiveSeed", at = @At("TAIL"), remap = false)
    private void seedsight$rememberSeed(Long seed, CallbackInfo callback) {
        SeedSightSeedState.select(seed);
    }
}
