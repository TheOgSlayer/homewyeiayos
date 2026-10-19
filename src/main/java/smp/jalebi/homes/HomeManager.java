package smp.jalebi.homes;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class HomeManager {
    private final JalebiHomes plugin;
    private final File file;
    private final YamlConfiguration data;

    public HomeManager(JalebiHomes plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        plugin.getDataFolder().mkdirs();
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public int max() {
        return Math.max(1, plugin.getConfig().getInt("default-homes", 3));
    }

    private String path(UUID id, int slot) {
        return "players." + id + "." + slot;
    }

    /** slots are 1-based */
    public Home get(UUID id, int slot) {
        ConfigurationSection s = data.getConfigurationSection(path(id, slot));
        if (s == null) return null;
        Material icon = Material.matchMaterial(s.getString("icon", "WHITE_BED"));
        if (icon == null) icon = Material.WHITE_BED;
        return new Home(s.getString("name", "Home " + slot), s.getString("world", "world"),
                s.getDouble("x"), s.getDouble("y"), s.getDouble("z"),
                (float) s.getDouble("yaw"), (float) s.getDouble("pitch"), icon);
    }

    public void set(UUID id, int slot, Home h) {
        String p = path(id, slot);
        data.set(p + ".name", h.name());
        data.set(p + ".world", h.world());
        data.set(p + ".x", h.x());
        data.set(p + ".y", h.y());
        data.set(p + ".z", h.z());
        data.set(p + ".yaw", h.yaw());
        data.set(p + ".pitch", h.pitch());
        data.set(p + ".icon", h.icon().name());
        save();
    }

    public void delete(UUID id, int slot) {
        data.set(path(id, slot), null);
        save();
    }

    public int firstEmpty(UUID id) {
        for (int i = 1; i <= max(); i++) if (get(id, i) == null) return i;
        return -1;
    }

    public int findByName(UUID id, String name) {
        for (int i = 1; i <= max(); i++) {
            Home h = get(id, i);
            if (h != null && h.name().equalsIgnoreCase(name)) return i;
        }
        return -1;
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml: " + e.getMessage());
        }
    }
}
