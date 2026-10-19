package smp.jalebi.homes;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class JalebiHomes extends JavaPlugin {
    private HomeManager homes;
    private HomeGui gui;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        homes = new HomeManager(this);
        gui = new HomeGui(this);
    }

    public HomeManager homes() { return homes; }
    public HomeGui gui() { return gui; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return true;
        }
        switch (cmd.getName().toLowerCase()) {
            case "homes" -> gui.openMain(p, 0);
            case "sethome" -> setHome(p, args);
        }
        return true;
    }

    private void setHome(Player p, String[] args) {
        String name = args.length > 0 ? String.join(" ", args).trim() : null;
        if (name != null && name.length() > 24) {
            Msg.chat(p, Msg.error("home name is too long (max 24 characters)"));
            return;
        }
        int slot = -1;
        if (name != null && !name.isEmpty()) slot = homes.findByName(p.getUniqueId(), name);
        if (slot == -1) slot = homes.firstEmpty(p.getUniqueId());
        if (slot == -1) {
            Msg.chat(p, Msg.error("you have no free home slots, delete one first"));
            return;
        }
        if (name == null || name.isEmpty()) name = "Home " + slot;
        setHomeAt(p, slot, name);
    }

    /** Saves (or overwrites) a home in a slot at the player's current location. */
    public void setHomeAt(Player p, int slot, String name) {
        Home old = homes.get(p.getUniqueId(), slot);
        Home h = Home.at(name, p.getLocation());
        if (old != null) h = h.withIcon(old.icon());
        homes.set(p.getUniqueId(), slot, h);
        Msg.chat(p, Msg.homeSet(name));
    }
}
