package fr.asashiin.worldfinder.seedsight;

import fr.asashiin.worldfinder.api.waypoint.WaypointProvider;
import fr.asashiin.worldfinder.api.waypoint.WaypointRequest;
import fr.asashiin.worldfinder.client.waypoint.BuiltInWaypoints;
import fr.asashiin.worldfinder.client.waypoint.NavigationTarget;

/** Makes SeedSight navigation appear inside WorldFinder's original waypoint menu. */
public final class SeedSightWaypointProvider implements WaypointProvider {
    @Override
    public String id() {
        return "seedsight:navigation";
    }

    @Override
    public String displayName() {
        return "Built-in navigation";
    }

    @Override
    public boolean createWaypoint(WaypointRequest request) {
        String dimension = "minecraft:" + request.dimension().serializedName();
        return BuiltInWaypoints.set(new NavigationTarget(
                request.name(), dimension, request.blockX(), request.blockY(), request.blockZ()));
    }
}
