package smp.jalebi.homes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public record Home(String name, String world, double x, double y, double z,
                   float yaw, float pitch, Material icon) {

    public Location toLocation() {
        World w = Bukkit.getWorld(world);
        if (w == null) return null;
        return new Location(w, x, y, z, yaw, pitch);
    }

    public Home withName(String n) {
        return new Home(n, world, x, y, z, yaw, pitch, icon);
    }

    public Home withIcon(Material m) {
        return new Home(name, world, x, y, z, yaw, pitch, m);
    }

    public Home withLocation(Location l) {
        return new Home(name, l.getWorld().getName(), l.getX(), l.getY(), l.getZ(),
                l.getYaw(), l.getPitch(), icon);
    }

    public static Home at(String name, Location l) {
        return new Home(name, l.getWorld().getName(), l.getX(), l.getY(), l.getZ(),
                l.getYaw(), l.getPitch(), Material.WHITE_BED);
    }
}
