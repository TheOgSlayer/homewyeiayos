package smp.jalebi.homes;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

/** Every chat / action bar message goes through here so it is always small caps. */
public final class Msg {
    public static final TextColor BLUE = TextColor.fromHexString("#00a2ff");

    private Msg() {}

    public static Component sc(String text, TextColor color) {
        return Component.text(SmallCaps.of(text), color);
    }

    public static void chat(Player p, Component c) {
        p.sendMessage(c);
    }

    public static void action(Player p, Component c) {
        p.sendActionBar(c);
    }

    public static Component teleported(String homeName) {
        return sc("teleported to ", NamedTextColor.WHITE).append(sc(homeName, BLUE));
    }

    public static Component homeSet(String homeName) {
        return sc("home ", NamedTextColor.WHITE).append(sc(homeName, BLUE))
                .append(sc(" set", NamedTextColor.GREEN));
    }

    public static Component error(String text) {
        return sc(text, NamedTextColor.RED);
    }
}
