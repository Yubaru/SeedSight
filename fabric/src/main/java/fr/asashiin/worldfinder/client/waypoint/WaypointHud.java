package fr.asashiin.worldfinder.client.waypoint;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.Locale;

/** Compact bottom-center navigation, above the normal hotbar/status area. */
public final class WaypointHud {
    private static long lastFrame;
    private static double bearing;
    private static double opacity;
    private static NavigationTarget previous;
    private WaypointHud() {}
    public static void reset() { lastFrame = 0; bearing = 0; opacity = 0; previous = null; }
    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        NavigationTarget target = BuiltInWaypoints.displayTarget();
        if (target == null || client.player == null || client.level == null
                || client.gui.screen() != null) { lastFrame = 0; return; }
        long now = System.nanoTime();
        double dt = lastFrame == 0 ? 1.0 / 60 : Math.min(0.1, (now - lastFrame) / 1e9);
        lastFrame = now;
        double age = (now - BuiltInWaypoints.changedAt()) / 1e9;
        boolean active = BuiltInWaypoints.target() != null;
        boolean sameDimension = target.dimension().equals(client.level.dimension().identifier().toString());
        double desiredOpacity = active || BuiltInWaypoints.arrived() && age < 1.2 ? 1 : 0;
        opacity += (desiredOpacity - opacity) * (1 - Math.exp(-dt * 10));
        if (opacity < 0.015) return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        var position = client.player.getPosition(partial);
        double wantedBearing = target.bearing(position.x, position.z, client.player.getViewYRot(partial));
        if (previous != target) { bearing = wantedBearing; previous = target; }
        bearing += NavigationTarget.wrapDegrees(wantedBearing - bearing) * (1 - Math.exp(-dt * 18));
        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();
        int cx = width / 2;
        int y = height - 82;
        int textColor = color(0xE8EEE6, opacity * 0.92);
        int accent = color(0xB6E58B, opacity);
        String label = client.font.plainSubstrByWidth(target.name(), Math.min(145, width - 40));
        String detail;
        if (!active && BuiltInWaypoints.arrived()) detail = "Arrived";
        else if (!active) detail = "Waypoint cleared";
        else if (!sameDimension) detail = "In " + dimensionLabel(target.dimension());
        else {
            double distance = target.distance(position.x, position.z);
            detail = distance >= 1000 ? String.format(Locale.ROOT, "%.1f km", distance / 1000) : Math.round(distance) + " m";
            if (Math.abs(NavigationTarget.wrapDegrees(bearing)) > 100) detail += "  ·  Behind you";
        }
        int boxWidth = Math.max(client.font.width(label), client.font.width(detail)) + 26;
        graphics.fill(cx - boxWidth / 2, y - 4, cx + boxWidth / 2, y + 22, color(0x101510, opacity * 0.35));
        graphics.centeredText(client.font, label, cx, y, textColor);
        graphics.centeredText(client.font, detail, cx, y + 11, accent);
        if (active && sameDimension) {
            var pose = graphics.pose();
            pose.pushMatrix();
            pose.translate(cx, y - 12);
            pose.rotate((float) Math.toRadians(bearing));
            for (int row = 0; row < 5; row++) {
                graphics.fill(-row - 1, row - 3, -row + 1, row - 2, accent);
                graphics.fill(row - 1, row - 3, row + 1, row - 2, accent);
            }
            pose.popMatrix();
        }
    }
    private static int color(int rgb, double alpha) { return ((int) (Math.clamp(alpha, 0, 1) * 255) << 24) | rgb; }
    private static String dimensionLabel(String dimension) {
        return switch (dimension) {
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "the Nether";
            case "minecraft:the_end" -> "the End";
            default -> dimension;
        };
    }
}
