package fr.asashiin.worldfinder.seedsight;

import fr.asashiin.worldfinder.api.waypoint.WaypointProvider;
import fr.asashiin.worldfinder.api.waypoint.WaypointRequest;

/** Adds persistent visited checks to the original marker context menu. */
public final class SeedSightVisitedPoiProvider implements WaypointProvider {
    private final boolean undo;

    public SeedSightVisitedPoiProvider(boolean undo) {
        this.undo = undo;
    }

    @Override
    public String id() {
        return undo ? "seedsight:mark_unvisited" : "seedsight:mark_visited";
    }

    @Override
    public String displayName() {
        return undo ? "Visited - uncheck" : "Not visited - check off";
    }

    @Override
    public boolean canCreateWaypoint(WaypointRequest request) {
        return VisitedPois.isVisited(request) == undo;
    }

    @Override
    public boolean createWaypoint(WaypointRequest request) {
        return VisitedPois.setVisited(request, !undo);
    }
}
