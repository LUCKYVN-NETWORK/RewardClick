package dev.DatxCute.commands;

import dev.DatxCute.Main;
import dev.DatxCute.utils.CC;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Rewardcommand implements CommandExecutor {

    private final Main plugin;
    public static final Map<String, String> editingTier = new HashMap<>();

    public Rewardcommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("reward")) {
            if (args.length < 1 || args[0].equalsIgnoreCase("help")) {
                sendHelpMessage(sender);
                return true;
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("edit")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§8[§6!§8]§c Lệnh này chỉ có thể được sử dụng bởi người chơi.");
                    return true;
                }

                Player player = (Player) sender;
                String tier = args[1];

                if (plugin.getConfig().contains("tiers." + tier)) {
                    Material material = Material.getMaterial(plugin.getConfig().getString("tiers." + tier + ".material"));
                    if (material != null) {
                        editingTier.put(player.getName(), tier);
                        ItemStack item = new ItemStack(material, 1);
                        ItemMeta itemMeta = item.getItemMeta();
                        if (itemMeta != null) {
                            itemMeta.setDisplayName(CC.translate(plugin.getConfig().getString("tiers." + tier + ".display-name")));
                            List<String> lore = plugin.getConfig().getStringList("tiers." + tier + ".lore");
                            itemMeta.setLore(CC.translate(lore));
                            item.setItemMeta(itemMeta);
                        }
                        player.getInventory().addItem(item);
                        player.sendMessage(getEditingTierMessage(tier));
                        player.sendMessage(getReceivedItemMessage(material.name()));
                    } else {
                        sender.sendMessage(CC.translate(plugin.getConfig().getString("messages.invalid_item")));
                    }
                    return true;
                } else {
                    sender.sendMessage(CC.translate(plugin.getConfig().getString("messages.tier_not_exist")));
                }
            } else if (args.length == 1 && args[0].equalsIgnoreCase("save")) {
                plugin.saveLocations();
                plugin.hideAllRewardBlocks();

                sender.sendMessage(CC.translate(plugin.getConfig().getString("messages.positions_saved")));

                editingTier.clear();
                return true;
            } else if (args.length == 1 && args[0].equalsIgnoreCase("start")) {
                if (sender.hasPermission("reward.start")) {
                    if (!plugin.isEventStarted()) {
                        plugin.startEvent(sender);
                    } else {
                        sender.sendMessage(CC.translate(plugin.getConfig().getString("messages.event_already_started")));

                    }
                } else {
                    sender.sendMessage(CC.translate(plugin.getConfig().getString("messages.no_permission_to_start_event")));

                }
                return true;
            } else if (args.length == 1 && args[0].equalsIgnoreCase("stop")) {
                if (sender.hasPermission("reward.stop")) {
                    if (plugin.isEventStarted()) {
                        plugin.stopEvent(sender);
                    } else {
                        sender.sendMessage(CC.translate(plugin.getConfig().getString("messages.event_not_started")));
                    }
                } else {
                    sender.sendMessage(CC.translate(plugin.getConfig().getString("messages.no_permission_to_stop_event")));
                }
                return true;
            } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("reward.reload")) {
                    plugin.reloadConfig();
                    plugin.resetRewardLocations();
                    sender.sendMessage(CC.translate("§8[§6!§8]§a Config by DatxCute has been reloaded"));
                } else {
                    sender.sendMessage(CC.translate("§8[§6!§8]§c You don't have permission"));
                }
                return true;
            }

        }
        return false;
    }

    private void sendHelpMessage(CommandSender sender) {
        FileConfiguration config = plugin.getConfig();
        if (config.contains("help-messages")) {
            for (String key : config.getStringList("help-messages")) {
                sender.sendMessage(CC.translate(key));
            }
        }
    }
    private String getEditingTierMessage(String tier) {
        return CC.translate(plugin.getConfig().getString("messages.editing_tier").replace("%tier%", tier));
    }

    private String getReceivedItemMessage(String material) {
        return CC.translate(plugin.getConfig().getString("messages.received_item").replace("%material%", material));
    }
}