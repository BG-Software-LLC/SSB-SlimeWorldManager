package com.bgsoftware.ssbslimeworldmanager;

import com.bgsoftware.ssbslimeworldmanager.swm.ISlimeWorld;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SlimeUtils {

    private static final Map<String, ISlimeWorld> islandWorlds = new HashMap<>();

    private SlimeUtils() {
    }

    public static ISlimeWorld getSlimeWorld(String worldName) {
        return islandWorlds.get(worldName);
    }

    public static void setSlimeWorld(String worldName, ISlimeWorld slimeWorld) {
        islandWorlds.put(worldName, slimeWorld);
    }

    public static boolean isIslandsWorld(String worldName) {
        String[] nameSections = worldName.split("_");
        try {
            return SuperiorSkyblockAPI.getGrid().getIslandByUUID(UUID.fromString(nameSections[1])) != null;
        } catch (Exception ex) {
            return false;
        }
    }

    public static void unloadWorld(String worldName) {
        ISlimeWorld slimeWorld = islandWorlds.remove(worldName);
        if (slimeWorld != null) {
            try {
                slimeWorld.serialize().whenComplete((serializeData, error) -> {
                    if (error == null) {
                        try {
                            slimeWorld.getLoader().saveWorld(worldName, serializeData);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }

                        Bukkit.unloadWorld(worldName, true);
                    } else {
                        error.printStackTrace();
                    }
                });
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static String getWorldName(Island island, World.Environment environment) {
        return getWorldName(island.getUniqueId(), environment);
    }

    public static String getWorldName(UUID islandUUID, World.Environment environment) {
        return "island_" + islandUUID + "_" + environment.name().toLowerCase();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean isIslandWorldName(String worldName) {
        String[] nameSections = worldName.split("_");
        try {
            UUID.fromString(nameSections[0]);
            World.Environment.valueOf(nameSections[1]);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

}
