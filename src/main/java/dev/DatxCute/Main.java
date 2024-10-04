package dev.DatxCute;

import dev.DatxCute.Events.BlockListener.BlockListener;
import dev.DatxCute.commands.Rewardcommand;
import dev.DatxCute.utils.CC;
import dev.DatxCute.utils.CountdownTimer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;

public class Main extends JavaPlugin implements Listener {

    private static Main instance;
    private boolean eventStarted = false;
    private final Map<Location, String> rewardLocations = new HashMap<>();
    private final Map<Location, Integer> particleTasks = new HashMap<>();
    private final List<Location> activeRewardLocations = new ArrayList<>();

    private CountdownTimer eventTimer;
    @Override
    public void onEnable() {
        instance = this;

        getConfig().options().copyDefaults(true);

        saveDefaultConfig();

        loadLocations();
        loadActiveRewardLocations();

        getServer().getConsoleSender().sendMessage("[RewardClick] Plugins Enable Copy Right DatxCute");

        getCommand("reward").setExecutor(new Rewardcommand(this));
        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
    }

    @Override
    public void onDisable() {
        saveLocations();
        resetRewardLocations();
        particleTasks.values().forEach(id -> getServer().getScheduler().cancelTask(id));
        rewardLocations.clear();
        activeRewardLocations.clear();
        if (eventTimer != null && eventTimer.isRunning()) {
            eventTimer.cancel();
        }
    }
    public int getRemainingRewards() {
        return activeRewardLocations.size();
    }
    public void decrementRemainingRewards() {
        if (!activeRewardLocations.isEmpty()) {
            activeRewardLocations.remove(0);
        }
    }
    public List<Location> getActiveRewardLocations() {
        return activeRewardLocations;
    }


    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Location location = event.getBlock().getLocation();
        if (isRewardBlock(location)) {
            BlockListener blockListener = new BlockListener(this);
            blockListener.startParticleEffect(location);
        }
    }

    private boolean isRewardBlock(Location location) {
        return rewardLocations.containsKey(location);
    }

    public void loadLocations() {
        rewardLocations.clear();

        ConfigurationSection locationsSection = getConfig().getConfigurationSection("locations");
        if (locationsSection != null) {
            for (String tier : locationsSection.getKeys(false)) {
                List<String> list = locationsSection.getStringList(tier);
                for(String locationString: list) {
                    Location location = parseLocation(locationString);
                    if (location != null) {
                        if (tier.isEmpty()) {
                            getLogger().warning("Tier for location " + locationString + " is null or empty.");
                            continue;
                        }
                        if (!getConfig().contains("tiers." + tier + ".item-use")) {
                            getLogger().warning("Item-use for tier " + tier + " is not defined.");
                            continue;
                        }
                        if (!getConfig().contains("tiers." + tier + ".material")) {
                            getLogger().warning("Material for tier " + tier + " is not defined.");
                            continue;
                        }
                        rewardLocations.put(location, tier);
                    }
                }
            }
        }
    }

    public void saveLocations() {
        for (String tier : getConfig().getConfigurationSection("tiers").getKeys(false)) {
            List<String> locList = new ArrayList<>();
            for (Map.Entry<Location, String> entry : rewardLocations.entrySet()) {
                if (entry.getValue().equals(tier)) {
                    Location loc = entry.getKey();
                    locList.add(loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
                }
            }
            getConfig().set("locations." + tier, locList);
        }
        saveConfig();
    }


    public void hideAllRewardBlocks() {
        for (Location location : rewardLocations.keySet()) {
            if (location.getWorld() == null) {
                getLogger().warning("World is null for location: " + locationToString(location));
                continue;
            }
            Block block = location.getBlock();
            block.setType(Material.AIR);
            cancelParticleEffect(location);
        }
    }

    public void editBlock(Location location, String tier) {
        String material = getConfig().getString("tiers." + tier + ".item-use");
        if(material == null || material.isEmpty())
            material = "BEDROCK";
        if(material.startsWith("head-")) {
            location.getBlock().setType(Material.PLAYER_HEAD);
            String base64Profile = material.replace("head-", "");
            Skull playerHead = (Skull) location.getBlock().getState();
            playerHead.update(true);
            ClassLoader serverLoader = getServer().getClass().getClassLoader();
            try {
                String profileName = "head_profile_" + System.nanoTime();
                Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile", true, serverLoader);
                Object tempProfile = gameProfileClass.getConstructors()[0].newInstance(UUID.randomUUID(), profileName);
                Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property", true, serverLoader);
                Object property = propertyClass.getConstructors()[0].newInstance("textures", base64Profile);
                Object propertyMap = tempProfile.getClass().getMethod("getProperties", new Class[0]).invoke(tempProfile);
                propertyMap.getClass().getMethod("put", new Class[] { Object.class, Object.class })
                        .invoke(propertyMap, new Object[] { "textures", property });
                Field fieldProfile = playerHead.getClass().getDeclaredField("profile");
                fieldProfile.setAccessible(true); fieldProfile.set(playerHead, tempProfile);
                playerHead.update(true);
            } catch(Exception err) { err.printStackTrace(); }
        } else {
            Material materialBukkit = Material.getMaterial(material);
            if(materialBukkit != null && materialBukkit.isBlock())
                location.getBlock().setType(materialBukkit);
        }
    }

    public void showSelectedRewardBlocks() {
        for (Location location : activeRewardLocations) {
            String tier = rewardLocations.get(location);
            if (tier == null || tier.isEmpty()) {
                getLogger().warning("Tier for location " + location + " is null or empty.");
                continue;
            }
            String materialName = getConfig().getString("tiers." + tier + ".item-use");
            if (materialName == null || materialName.isEmpty()) {
                getLogger().warning("Material name for tier " + tier + " is null or empty at location: " + location.toString());
                continue;
            }
            Material material = materialName.startsWith("head-") ? Material.PLAYER_HEAD : Material.matchMaterial(materialName);
            if (material == null || !material.isBlock()) {
                getLogger().warning("Invalid material for tier " + tier + " at location: " + location.toString());
                continue;
            }
            editBlock(location, tier);
            startParticleEffect(location);
        }
    }


    public void startEvent(CommandSender sender) {
        if (eventStarted) {
            sender.sendMessage(CC.translate(getConfig().getString("messages.event_already_starteds")));
            return;
        }
        eventStarted = true;
        sender.sendMessage(CC.translate(getConfig().getString("messages.event_starting")));
        hideAllRewardBlocks();
        List<Location> locations = new ArrayList<>(rewardLocations.keySet());
        if (locations.isEmpty()) {
            sender.sendMessage(CC.translate(getConfig().getString("messages.no_reward_locations")));
            return;
        }
        String defaultTier = rewardLocations.values().iterator().next();
        int randomRewards = getConfig().getInt("tiers." + defaultTier + ".random_rewards");

        if (randomRewards > locations.size()) {
            randomRewards = locations.size();
        }
        Collections.shuffle(locations);
        activeRewardLocations.clear();
        for (int i = 0; i < randomRewards; i++) {
            activeRewardLocations.add(locations.get(i));
        }
        showSelectedRewardBlocks();
        saveLocations();
        int eventTimeSeconds = getConfig().getInt("Event-time", 300);
        eventTimer = new CountdownTimer(eventTimeSeconds, () -> {
            sender.sendMessage(CC.translate(getConfig().getString("Event-end-message")));
            stopEvent(sender);
        });
        eventTimer.start();
    }
    public void stopEvent(CommandSender sender) {
        if (!eventStarted) {
            sender.sendMessage(CC.translate(getConfig().getString("messages.event_not_started")));
            return;
        }
        eventStarted = false;
        hideAllRewardBlocks();
        for (Location location : activeRewardLocations) {
            cancelParticleEffect(location);
        }
        activeRewardLocations.clear();
        saveLocations();

        if (eventTimer != null && eventTimer.isRunning()) {
            eventTimer.cancel();
        }
    }
    public static Main getInstance() {
        return instance;
    }
    public void resetRewardLocations() {
        new HashSet<>(rewardLocations.keySet()).forEach(location -> {
            location.getBlock().setType(Material.AIR);
            rewardLocations.remove(location);
        });
        reloadConfig();
        loadLocations();
    }
    public void loadActiveRewardLocations() {
        activeRewardLocations.clear();
        ConfigurationSection activeLocationsSection = getConfig().getConfigurationSection("active_reward_locations");
        if (activeLocationsSection != null) {
            for (String locString : activeLocationsSection.getKeys(false)) {
                Location location = parseLocation(locString);
                if (location != null) {
                    activeRewardLocations.add(location);
                }
            }
        }
    }
    public boolean isEventStarted() {
        return eventStarted;
    }
    public Location parseLocation(String locString) {
        String[] parts = locString.split(",");
        if (parts.length == 4) {
            String worldName = parts[0];
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return new Location(getServer().getWorld(worldName), x, y, z);
        } else {
            getLogger().warning("Invalid format for location string: " + locString);
            return null;
        }
    }

    public void hideRewardBlock(Location location) {
        if (rewardLocations.containsKey(location)) {
            location.getBlock().setType(Material.AIR);
            cancelParticleEffect(location);
        }
    }
    public String locationToString(Location location) {
        return location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    public void addRewardLocation(Location location, String tier) {
        rewardLocations.put(location, tier);
        saveLocations();
    }

    public String getBlockTierAtLocation(Location location) {
        return rewardLocations.get(location);
    }

    public Map<Location, String> getRewardLocations() {
        return rewardLocations;
    }
    public void startParticleEffect(Location location) {
        int taskId = new BukkitRunnable() {
            final double radius = 0.65;

            @Override
            public void run() {
                if (!rewardLocations.containsKey(location)) {
                    cancel();
                    particleTasks.remove(location);
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
        }.runTaskTimer(this, 0, 10).getTaskId();

        particleTasks.put(location, taskId);
    }

    public void cancelParticleEffect(Location location) {
        int taskId = particleTasks.getOrDefault(location, -1);
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            particleTasks.remove(location);
        }
    }
}
