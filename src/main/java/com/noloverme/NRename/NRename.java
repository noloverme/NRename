package com.noloverme.NRename;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class NRename extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("NRename включен!");
        getCommand("rename").setExecutor(new RenameCommand(this));
        getCommand("nrename").setExecutor(this);
        saveDefaultConfig(); // Создаёт config.yml, если отсутствует
    }

    @Override
    public void onDisable() {
        getLogger().info("NRename выключен!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("nrename")) {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.reload-usage")));
                return true;
            }
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("nrename.reload")) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.no-permission")));
                    return true;
                }
                reloadConfig();
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.reload-success")));
                return true;
            }
        }
        return false;
    }
}
