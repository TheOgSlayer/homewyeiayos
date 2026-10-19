package smp.jalebi.homes;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class HomeGui {
    private static final int ICONS_PER_PAGE = 36;
    private static final TextColor GRAYISH_BLACK = NamedTextColor.DARK_GRAY;
    private static final ClickCallback.Options OPTS =
            ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).build();

    private static final List<Material> ALL_ITEMS = Arrays.stream(Material.values())
            .filter(m -> m.isItem() && !m.isAir() && !m.isLegacy())
            .sorted((a, b) -> a.name().compareTo(b.name()))
            .toList();

    private final JalebiHomes plugin;

    public HomeGui(JalebiHomes plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ helpers

    private ActionButton btn(Component label, Component tooltip, int width, Consumer<Player> onClick) {
        return ActionButton.create(label, tooltip, width, DialogAction.customClick((view, audience) -> {
            if (audience instanceof Player p) Bukkit.getScheduler().runTask(plugin, () -> onClick.accept(p));
        }, OPTS));
    }

    /** Button that also reads a text input (by key) when clicked. */
    private ActionButton btnText(Component label, Component tooltip, int width, String key,
                                 BiConsumer<Player, String> onClick) {
        return ActionButton.create(label, tooltip, width, DialogAction.customClick((view, audience) -> {
            String text = view.getText(key);
            if (audience instanceof Player p)
                Bukkit.getScheduler().runTask(plugin, () -> onClick.accept(p, text == null ? "" : text));
        }, OPTS));
    }

    private Dialog dialog(Component title, List<DialogBody> body, List<DialogInput> inputs, DialogType type) {
        return Dialog.create(f -> f.empty()
                .base(DialogBase.builder(title)
                        .canCloseWithEscape(true)
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.NONE)
                        .body(body)
                        .inputs(inputs)
                        .build())
                .type(type));
    }

    /** Tooltip: bold-ish name line + small caps gray lore line. */
    private Component tip(String name, TextColor nameColor, String lore) {
        Component c = Component.text(name, nameColor);
        if (lore != null) c = c.appendNewline().append(Msg.sc(lore, NamedTextColor.GRAY));
        return c;
    }

    private static String pretty(Material m) {
        StringBuilder sb = new StringBuilder();
        for (String part : m.name().toLowerCase(Locale.ROOT).split("_")) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ main /homes screen

    public void openMain(Player p, int page) {
        UUID id = p.getUniqueId();
        int max = plugin.homes().max();
        int perPage = Math.max(1, plugin.getConfig().getInt("homes-per-page", 5));
        int pages = (int) Math.ceil(max / (double) perPage);
        page = Math.max(0, Math.min(page, pages - 1));
        int start = page * perPage + 1;
        int end = Math.min(max, start + perPage - 1);
        final int curPage = page;

        List<ActionButton> actions = new ArrayList<>();
        for (int slot = start; slot <= end; slot++) {
            final int s = slot;
            Home h = plugin.homes().get(id, slot);
            if (h != null) {
                actions.add(btn(Component.text(h.name(), NamedTextColor.WHITE),
                        tip(h.name(), NamedTextColor.WHITE, "click to manage"),
                        110, pl -> openManage(pl, s, curPage)));
            } else {
                actions.add(btn(Component.text("New Home", GRAYISH_BLACK),
                        tip("Home " + slot, GRAYISH_BLACK, "click to set a home here"),
                        110, pl -> {
                            plugin.setHomeAt(pl, s, "Home " + s);
                            pl.closeDialog();
                        }));
            }
        }
        if (page < pages - 1) {
            actions.add(btn(Component.text("Show More"), null, 110, pl -> openMain(pl, curPage + 1)));
        }
        ActionButton exit = null;
        if (page > 0) {
            exit = btn(Component.text("Show Less"), null, 150, pl -> openMain(pl, curPage - 1));
        }

        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.item(new ItemStack(Material.WHITE_BED)).build());

        Dialog d = dialog(Component.text("Homes"), body, List.of(),
                DialogType.multiAction(actions, exit, Math.min(4, Math.max(1, actions.size()))));
        p.showDialog(d);
    }

    // ------------------------------------------------------------------ manage a home

    public void openManage(Player p, int slot, int fromPage) {
        UUID id = p.getUniqueId();
        Home h = plugin.homes().get(id, slot);
        if (h == null) { openMain(p, fromPage); return; }

        List<ActionButton> actions = new ArrayList<>();
        actions.add(btn(Component.text("Teleport", NamedTextColor.GREEN),
                tip("Teleport", NamedTextColor.GREEN, "teleports you to the location of the home"),
                150, pl -> teleport(pl, slot)));
        actions.add(btn(Component.text("Delete", NamedTextColor.RED),
                tip("Delete", NamedTextColor.RED, "click to delete this home"),
                150, pl -> openDelete(pl, slot, fromPage)));
        actions.add(btn(Component.text("Rename", NamedTextColor.WHITE),
                tip("Rename", NamedTextColor.WHITE, "rename this home"),
                150, pl -> openRename(pl, slot, fromPage)));
        actions.add(btn(Component.text("Change Icon", NamedTextColor.WHITE),
                tip("Change Icon", NamedTextColor.WHITE, "change the icon of this home"),
                150, pl -> openIcons(pl, slot, fromPage, "", 0)));
        ActionButton back = btn(Component.text("Back"), null, 150, pl -> openMain(pl, fromPage));

        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.item(new ItemStack(h.icon())).build());

        p.showDialog(dialog(Component.text(h.name()), body, List.of(),
                DialogType.multiAction(actions, back, 1)));
    }

    private void teleport(Player p, int slot) {
        Home h = plugin.homes().get(p.getUniqueId(), slot);
        if (h == null) return;
        Location loc = h.toLocation();
        p.closeDialog();
        if (loc == null) {
            Msg.chat(p, Msg.error("that home's world is not loaded"));
            return;
        }
        p.teleport(loc);
        Msg.chat(p, Msg.teleported(h.name()));
    }

    // ------------------------------------------------------------------ delete

    private void openDelete(Player p, int slot, int fromPage) {
        Home h = plugin.homes().get(p.getUniqueId(), slot);
        if (h == null) { openMain(p, fromPage); return; }

        ActionButton yes = btn(Component.text("Delete", NamedTextColor.RED), null, 150, pl -> {
            Home cur = plugin.homes().get(pl.getUniqueId(), slot);
            plugin.homes().delete(pl.getUniqueId(), slot);
            pl.closeDialog();
            Msg.action(pl, Msg.sc("home", NamedTextColor.WHITE)
                    .append(Msg.sc(" deleted", NamedTextColor.RED)));
        });
        ActionButton no = btn(Component.text("Cancel", NamedTextColor.GRAY), null, 150,
                pl -> openManage(pl, slot, fromPage));

        List<DialogBody> body = List.of(DialogBody.plainMessage(
                Component.text("Are you sure you want to delete " + h.name() + "?")));
        p.showDialog(dialog(Component.text("Delete Home"), body, List.of(), DialogType.confirmation(yes, no)));
    }

    // ------------------------------------------------------------------ rename

    private void openRename(Player p, int slot, int fromPage) {
        Home h = plugin.homes().get(p.getUniqueId(), slot);
        if (h == null) { openMain(p, fromPage); return; }

        List<DialogInput> inputs = List.of(
                DialogInput.text("name", Component.text("New name"))
                        .initial(h.name())
                        .maxLength(24)
                        .build());

        ActionButton save = btnText(Component.text("Save", NamedTextColor.GREEN), null, 150, "name", (pl, text) -> {
            String n = text.trim();
            if (n.isEmpty()) {
                pl.closeDialog();
                Msg.action(pl, Msg.error("name cannot be empty"));
                return;
            }
            Home cur = plugin.homes().get(pl.getUniqueId(), slot);
            if (cur == null) { pl.closeDialog(); return; }
            plugin.homes().set(pl.getUniqueId(), slot, cur.withName(n));
            pl.closeDialog();
            Msg.action(pl, Msg.sc("home renamed", NamedTextColor.GREEN));
        });
        ActionButton cancel = btn(Component.text("Cancel", NamedTextColor.GRAY), null, 150,
                pl -> openManage(pl, slot, fromPage));

        p.showDialog(dialog(Component.text("Rename Home"), List.of(), inputs,
                DialogType.confirmation(save, cancel)));
    }

    // ------------------------------------------------------------------ change icon

    private List<Material> filter(String query) {
        String q = query.toLowerCase(Locale.ROOT).trim();
        if (q.isEmpty()) return ALL_ITEMS;
        return ALL_ITEMS.stream()
                .filter(m -> m.name().toLowerCase(Locale.ROOT).replace('_', ' ').contains(q))
                .toList();
    }

    public void openIcons(Player p, int slot, int fromPage, String query, int page) {
        Home h = plugin.homes().get(p.getUniqueId(), slot);
        if (h == null) { openMain(p, fromPage); return; }

        List<Material> results = filter(query);
        int pages = Math.max(1, (int) Math.ceil(results.size() / (double) ICONS_PER_PAGE));
        int pg = Math.max(0, Math.min(page, pages - 1));

        List<ActionButton> actions = new ArrayList<>();
        actions.add(btnText(Component.text("Search", NamedTextColor.AQUA), null, 130, "query",
                (pl, q) -> openIcons(pl, slot, fromPage, q, 0)));
        actions.add(btn(Component.text("Delete Icon", NamedTextColor.RED),
                tip("Delete Icon", NamedTextColor.RED, "resets to the default white bed"),
                130, pl -> {
                    Home cur = plugin.homes().get(pl.getUniqueId(), slot);
                    if (cur != null) plugin.homes().set(pl.getUniqueId(), slot, cur.withIcon(Material.WHITE_BED));
                    pl.closeDialog();
                    Msg.action(pl, Msg.sc("home icon changed", NamedTextColor.GREEN));
                }));
        if (pg > 0)
            actions.add(btn(Component.text("< Previous"), null, 130, pl -> openIcons(pl, slot, fromPage, query, pg - 1)));
        if (pg < pages - 1)
            actions.add(btn(Component.text("Next >"), null, 130, pl -> openIcons(pl, slot, fromPage, query, pg + 1)));

        int from = pg * ICONS_PER_PAGE;
        int to = Math.min(results.size(), from + ICONS_PER_PAGE);
        for (int i = from; i < to; i++) {
            final Material m = results.get(i);
            actions.add(btn(Component.text(pretty(m)), null, 130,
                    pl -> openConfirmIcon(pl, slot, fromPage, m, query, pg)));
        }

        List<DialogInput> inputs = List.of(
                DialogInput.text("query", Component.text("Search blocks and items"))
                        .initial(query)
                        .maxLength(50)
                        .build());
        List<DialogBody> body = List.of(DialogBody.plainMessage(Component.text(
                results.size() + " results - page " + (pg + 1) + "/" + pages)));

        ActionButton back = btn(Component.text("Back"), null, 150, pl -> openManage(pl, slot, fromPage));
        p.showDialog(dialog(Component.text("Change Icon"), body, inputs,
                DialogType.multiAction(actions, back, 3)));
    }

    private void openConfirmIcon(Player p, int slot, int fromPage, Material m, String query, int page) {
        ActionButton save = btn(Component.text("Save", NamedTextColor.GREEN), null, 150, pl -> {
            Home cur = plugin.homes().get(pl.getUniqueId(), slot);
            if (cur != null) plugin.homes().set(pl.getUniqueId(), slot, cur.withIcon(m));
            pl.closeDialog();
            Msg.action(pl, Msg.sc("home icon changed", NamedTextColor.GREEN));
        });
        ActionButton cancel = btn(Component.text("Cancel", NamedTextColor.GRAY), null, 150,
                pl -> openIcons(pl, slot, fromPage, query, page));

        List<DialogBody> body = List.of(
                DialogBody.item(new ItemStack(m)).build(),
                DialogBody.plainMessage(Component.text("Use " + pretty(m) + " as the icon?")));
        p.showDialog(dialog(Component.text("Confirm Icon"), body, List.of(),
                DialogType.confirmation(save, cancel)));
    }
}
