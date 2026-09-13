package fr.asashiin.worldfinder.seedsight;

import fr.asashiin.worldfinder.api.waypoint.WorldFinderWaypoints;
import fr.asashiin.worldfinder.client.waypoint.BuiltInWaypoints;
import fr.asashiin.worldfinder.client.waypoint.WaypointHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Adds navigation while leaving the official WorldFinder client and screen untouched. */
public final class SeedSightWaypointClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        WorldFinderWaypoints.registerProvider(new SeedSightWaypointProvider());
        WorldFinderWaypoints.registerProvider(new SeedSightVisitedPoiProvider(false));
        WorldFinderWaypoints.registerProvider(new SeedSightVisitedPoiProvider(true));
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("seedsight", "waypoint"), WaypointHud::render);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            BuiltInWaypoints.reset();
            VisitedPois.reset();
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            BuiltInWaypoints.tick(client);
            VisitedPois.tick(client);
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
            dispatcher.register(ClientCommands.literal("wf")
                    .then(ClientCommands.literal("waypoint")
                            .then(ClientCommands.literal("clear").executes(command -> {
                                BuiltInWaypoints.clear();
                                command.getSource().sendFeedback(Component.literal("SeedSight waypoint cleared."));
                                return 1;
                            }))));
        });
    }
}
