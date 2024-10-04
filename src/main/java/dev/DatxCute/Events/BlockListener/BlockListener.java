package dev.DatxCute.Events.BlockListener;

import dev.DatxCute.Main;
import dev.DatxCute.commands.Rewardcommand;
import dev.DatxCute.utils.CC;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class BlockListener implements Listener {

    private final Main plugin;
    private final Set<Player> interactedPlayers = new HashSet<>();

    public BlockListener(Main plugin) {
        this.plugin = plugin;
    }

    public void startParticleEffect(Location location) {
        new BukkitRunnable() {
            final double radius = 0.65;

            @Override
            public void run() {
                if (!plugin.getRewardLocations().containsKey(location)) {
                    this.cancel();
                    return;
                }

                double xOffset = (Math.random() * 2 - 1) * radius;
                double zOffset = (Math.random() * 2 - 1) * radius;

                while (xOffset * xOffset + zOffset * zOffset > radius * radius) {
                    xOffset = (Math.random() * 2 - 1) * radius;
                    zOffset = (Math.random() * 2 - 1) * radius;
                }

                Location particleLocation = location.clone().add(xOffset + 0.5, 0.5, zOffset + 0.5);
                location.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, particleLocation, 0);
            }
        }.runTaskTimer(plugin, 0, 10);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        if (interactedPlayers.contains(player)) {
            return;
        }
        if (Rewardcommand.editingTier.containsKey(player.getName())) {
            String tier = Rewardcommand.editingTier.get(player.getName());
            Material requiredItem = Material.getMaterial(plugin.getConfig().getString("tiers." + tier + ".material"));
            if (requiredItem != null) {
                if (clickedBlock != null && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    if (event.getItem() != null && event.getItem().getType() == requiredItem) {
                        Location location = clickedBlock.getLocation();
                        plugin.addRewardLocation(location, tier);
                        player.sendMessage(getMarkPositionMessage(tier));
                        plugin.editBlock(location, tier);
                        plugin.startParticleEffect(location);
                        event.setCancelled(true);
                        return;
                    }
                }
            } else {
                player.sendMessage(CC.translate(plugin.getConfig().getString("messages.invalid_item_or_block")));
            }
        } else {
            if (plugin.isEventStarted() && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (clickedBlock != null && plugin.getRewardLocations().containsKey(clickedBlock.getLocation())) {
                    Location location = clickedBlock.getLocation();
                    String tier = plugin.getBlockTierAtLocation(location);
                    if (tier != null) {
                        location.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, location.clone().add(0.5, 1, 0.5), 50);

                        player.playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

                        List<String> commands = plugin.getConfig().getStringList("tiers." + tier + ".commands");
                        if (!commands.isEmpty()) {
                            String command = commands.get(new Random().nextInt(commands.size())).replace("%player%", player.getName());
                            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
                            String message = getRewardMessage(player.getName(), tier);
                            Bukkit.broadcastMessage(CC.translate(message));

                            plugin.decrementRemainingRewards();
                            String remainingRewardsMessage = plugin.getConfig().getString("messages.rewards_remaining");
                            if (remainingRewardsMessage != null) {
                                remainingRewardsMessage = remainingRewardsMessage.replace("%remaining%", String.valueOf(plugin.getRemainingRewards()));
                                Bukkit.broadcastMessage(CC.translate(remainingRewardsMessage));
                            }
                        }
                        plugin.hideRewardBlock(location);
                    }
                }
            }
        }
        interactedPlayers.add(player);
        new BukkitRunnable() {
            @Override
            public void run() {
                interactedPlayers.remove(player);
            }
        }.runTaskLater(plugin, 10L);
    }

    private String getRewardMessage(String player, String tier) {
        String message = plugin.getConfig().getString("messages.reward_message");
        if (message != null) {
            message = message.replace("%player%", player).replace("%tier%", tier);
        }
        return CC.translate(message);
    }

    private String getMarkPositionMessage(String tier) {
        return CC.translate(plugin.getConfig().getString("messages.mark_position").replace("%tier%", tier));
    }
}
