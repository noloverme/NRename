package com.noloverme.NRename;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RenameCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final Pattern hexPattern = Pattern.compile("&#([0-9A-Fa-f]{6})");
    private final String LORE_IDENTIFIER = ChatColor.DARK_GRAY + "" + ChatColor.MAGIC + "NRename";

    public RenameCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sendMsg(sender, "not-a-player");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("nrename.rename")) {
            sendMsg(player, "permission-message");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            sendMsg(player, "no-item-in-hand");
            return true;
        }

        // /rename reset
        if (args.length == 1 && args[0].equalsIgnoreCase("reset")) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                sendMsg(player, "meta-error");
                return true;
            }

            meta.setDisplayName(null);

            List<String> lore = meta.getLore();
            if (lore != null) {
                lore.removeIf(line -> ChatColor.stripColor(line).contains(ChatColor.stripColor(LORE_IDENTIFIER)));
                if (lore.isEmpty()) lore = null;
            }
            meta.setLore(lore);
            item.setItemMeta(meta);

            sendMsg(player, "rename-reset");
            return true;
        }

        if (args.length == 0) {
            sendMsg(player, "usage");
            return true;
        }

        String rawName = String.join(" ", args);
        String coloredName = ChatColor.translateAlternateColorCodes('&', translateHexColorCodes(rawName));
        String visibleName = stripMinecraftColors(coloredName);

        int maxLength = plugin.getConfig().getInt("settings.max-name-length", 30);
        if (visibleName.length() > maxLength) {
            String msg = getMsg("name-too-long").replace("%max%", String.valueOf(maxLength));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
            return true;
        }

        if (visibleName.trim().isEmpty()) {
            sendMsg(player, "empty-name");
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            sendMsg(player, "meta-error");
            plugin.getLogger().log(Level.WARNING, "ItemMeta is null for player " + player.getName());
            return true;
        }

        meta.setDisplayName(coloredName);

        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();

        if (player.hasPermission("nrename.bypasslore")) {
            lore.removeIf(line -> line.contains(ChatColor.stripColor(LORE_IDENTIFIER)));
            if (lore.isEmpty()) lore = null;
            meta.setLore(lore);
        } else {
            meta.setLore(processLore(lore, player));
        }

        item.setItemMeta(meta);

        String message = getMsg("rename-success").replace("%name%", coloredName);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        return true;
    }

    private List<String> processLore(List<String> originalLore, Player player) {
        boolean hasNRenameLore = false;
        for (String line : originalLore) {
            if (ChatColor.stripColor(line).contains(ChatColor.stripColor(LORE_IDENTIFIER))) {
                hasNRenameLore = true;
                break;
            }
        }

        if (!hasNRenameLore && plugin.getConfig().getBoolean("settings.add-lore")) {
            String loreFormat = getMsg("lore-format").replace("%player%", player.getName());

            List<String> newLore = new ArrayList<>(originalLore);
            newLore.add("");
            newLore.add(LORE_IDENTIFIER);

            if (loreFormat.length() > 50) loreFormat = loreFormat.substring(0, 50);

            newLore.add(ChatColor.translateAlternateColorCodes('&', translateHexColorCodes(loreFormat)));
            return newLore;
        } else {
            return originalLore;
        }
    }

    private String translateHexColorCodes(String message) {
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String stripMinecraftColors(String input) {
        if (input == null) return "";
        String hexColorPattern = "§x(§[0-9a-fA-F]){6}";
        String normalColorPattern = "§[0-9a-fk-orK-OR]";
        return input.replaceAll(hexColorPattern, "").replaceAll(normalColorPattern, "");
    }

    private String getMsg(String path) {
        return plugin.getConfig().getString("messages." + path, "&c[" + path + "]");
    }

    private void sendMsg(CommandSender sender, String path) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getMsg(path)));
    }
}
