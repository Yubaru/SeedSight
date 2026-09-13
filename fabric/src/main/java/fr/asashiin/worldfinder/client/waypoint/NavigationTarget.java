package fr.asashiin.worldfinder.client.waypoint;

/** A map destination. Arrival is horizontal because seed-map heights are estimates. */
public record NavigationTarget(String name, String dimension, int x, int y, int z) {
    public NavigationTarget {
        if (name == null || name.isBlank() || dimension == null || dimension.isBlank()) {
            throw new IllegalArgumentException("A waypoint needs a name and dimension");
        }
        name = name.replaceAll("[\\p{Cntrl}§]", "").strip();
        if (name.isEmpty()) throw new IllegalArgumentException("Empty waypoint name");
        if (name.length() > 80) name = name.substring(0, 80);
        if (Math.abs((long) x) > 30_000_000 || Math.abs((long) z) > 30_000_000) {
            throw new IllegalArgumentException("Waypoint outside world bounds");
        }
    }

    public double distance(double playerX, double playerZ) {
        return Math.hypot(x + 0.5 - playerX, z + 0.5 - playerZ);
    }

    public double bearing(double playerX, double playerZ, double yaw) {
        return wrapDegrees(Math.toDegrees(Math.atan2(-(x + 0.5 - playerX), z + 0.5 - playerZ)) - yaw);
    }

    public static double wrapDegrees(double degrees) {
        return ((degrees + 180) % 360 + 360) % 360 - 180;
    }
}
