package fr.asashiin.worldfinder.seedsight.mixin;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps Fabric's End-biome map compatible with the key-only holders produced by
 * Minecraft's vanilla registry bootstrap and used by WorldFinder offline scans.
 */
@Mixin(targets = "net.fabricmc.fabric.impl.biome.TheEndBiomeData$ResourceKeyHashStrategy", remap = false)
abstract class FabricEndBiomeHolderEqualityMixin {
    @Inject(
            method = "equals(Lnet/minecraft/core/Holder;Lnet/minecraft/core/Holder;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void seedsight$compareWithoutDereferencing(
            Holder<?> first, Holder<?> second, CallbackInfoReturnable<Boolean> callback) {
        if (first == second) {
            callback.setReturnValue(true);
            return;
        }
        if (first == null || second == null || first.kind() != second.kind()) {
            callback.setReturnValue(false);
            return;
        }

        Optional<? extends ResourceKey<?>> firstKey = first.unwrapKey();
        Optional<? extends ResourceKey<?>> secondKey = second.unwrapKey();
        if (firstKey.isPresent() || secondKey.isPresent()) {
            callback.setReturnValue(firstKey.isPresent()
                    && secondKey.isPresent()
                    && firstKey.get() == secondKey.get());
            return;
        }

        callback.setReturnValue(first.value().equals(second.value()));
    }
}
