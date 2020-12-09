package com.bgsoftware.ssbslimeworldmanager;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.grinderwolf.swm.api.SlimePlugin;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import com.grinderwolf.swm.plugin.config.ConfigManager;
import com.grinderwolf.swm.plugin.config.WorldData;
import com.grinderwolf.swm.plugin.config.WorldsConfig;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SlimeUtils {

    private static final Map<String, SlimeWorld> islandWorlds = new HashMap<>();

    private static final SlimePlugin slimePlugin;
    private static final WorldData defaultWorldData = buildDefaultWorldData();

    static {
        slimePlugin = (SlimePlugin) Bukkit.getPluginManager().getPlugin("SlimeWorldManager");
    }

    private SlimeUtils() { }

    public static void init() { }

    public static void unloadAllWorlds(){
        try{
            slimePlugin.getLoader(defaultWorldData.getDataSource()).listWorlds().forEach(worldName -> {
                if(isIslandWorldName(worldName) && Bukkit.getWorld(worldName) != null)
                    unloadWorld(worldName);
            });
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public static SlimeWorld loadAndGetWorld(Island island, World.Environment environment){
        return loadAndGetWorld(island.getUniqueId(), environment);
    }

    public static SlimeWorld loadAndGetWorld(UUID islandUUID, World.Environment environment){
        return loadAndGetWorld(getWorldName(islandUUID, environment), environment);
    }

    public static SlimeWorld loadAndGetWorld(String worldName, World.Environment environment){
        SlimeWorld slimeWorld = islandWorlds.get(worldName);

        if(slimeWorld == null){
            WorldData worldData = ConfigManager.getWorldConfig().getWorlds().get(worldName);

            try {
                // No world was found, creating a new world.
                if (worldData == null) {
                    SlimePropertyMap slimePropertyMap = defaultWorldData.toPropertyMap();
                    slimePropertyMap.setString(SlimeProperties.ENVIRONMENT, environment.name().toUpperCase());
                    slimeWorld = slimePlugin.createEmptyWorld(slimePlugin.getLoader(defaultWorldData.getDataSource()),
                            worldName, defaultWorldData.isReadOnly(), slimePropertyMap);

                    // Saving the world
                    WorldsConfig config = ConfigManager.getWorldConfig();
                    config.getWorlds().put(worldName, defaultWorldData);
                    config.save();
                } else {
                    slimeWorld = slimePlugin.loadWorld(slimePlugin.getLoader(worldData.getDataSource()),
                            worldName, worldData.isReadOnly(), worldData.toPropertyMap());
                }

                islandWorlds.put(worldName, slimeWorld);
            }catch (Exception ex){
                throw new RuntimeException(ex);
            }
        }

        if(Bukkit.getWorld(worldName) == null)
            slimePlugin.generateWorld(slimeWorld);

        return slimeWorld;
    }

    public static void deleteWorld(Island island, World.Environment environment){
        String worldName = getWorldName(island, environment);

        WorldData worldData = ConfigManager.getWorldConfig().getWorlds().get(worldName);

        unloadWorld(worldName);

        try {
            slimePlugin.getLoader(worldData.getDataSource()).deleteWorld(worldName);
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public static boolean isIslandsWorld(String worldName){
        String[] nameSections = worldName.split("_");
        try{
            return SuperiorSkyblockAPI.getGrid().getIslandByUUID(UUID.fromString(nameSections[1])) != null;
        }catch (Exception ex) {
            return false;
        }
    }

    public static void unloadWorld(String worldName){
        SlimeWorld slimeWorld = islandWorlds.remove(worldName);
        if(slimeWorld != null) {
            try {
                slimeWorld.getLoader().saveWorld(worldName, ((CraftSlimeWorld) slimeWorld).serialize(), true);
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }
        Bukkit.unloadWorld(worldName, true);
    }

    public static String getWorldName(Island island, World.Environment environment){
        return getWorldName(island.getUniqueId(), environment);
    }

    public static String getWorldName(UUID islandUUID, World.Environment environment){
        return "island_" + islandUUID + "_" + environment.name().toLowerCase();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static boolean isIslandWorldName(String worldName){
        String[] nameSections = worldName.split("_");
        try{
            UUID.fromString(nameSections[0]);
            World.Environment.valueOf(nameSections[1]);
            return true;
        }catch (Exception ex){
            return false;
        }
    }

    private static WorldData buildDefaultWorldData(){
        WorldData worldData = new WorldData();

        worldData.setDataSource("file");
        worldData.setDifficulty("normal");
        worldData.setLoadOnStartup(false);

        return worldData;
    }

}
