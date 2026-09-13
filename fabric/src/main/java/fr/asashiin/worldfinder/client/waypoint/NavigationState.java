package fr.asashiin.worldfinder.client.waypoint;

/** Tick-based arrival detection, independent of rendering and frame rate. */
public final class NavigationState {
    public static final double ARRIVAL_RADIUS = 12;
    public static final int ARRIVAL_TICKS = 15;
    private NavigationTarget target;
    private int nearTicks;

    public NavigationTarget target() { return target; }
    public void set(NavigationTarget value) { target = value; nearTicks = 0; }

    /** Returns the completed target once; a dead player or wrong dimension never completes it. */
    public NavigationTarget tick(String dimension, double x, double z, boolean alive) {
        if (target == null) return null;
        if (!alive || !target.dimension().equals(dimension) || target.distance(x, z) > ARRIVAL_RADIUS) {
            nearTicks = 0;
            return null;
        }
        if (++nearTicks < ARRIVAL_TICKS) return null;
        NavigationTarget arrived = target;
        set(null);
        return arrived;
    }
}
