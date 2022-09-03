package com.bgsoftware.ssbslimeworldmanager;

import com.bgsoftware.ssbslimeworldmanager.swm.ISlimeWorld;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

    public static CompletableFuture<Boolean> unloadWorld(String worldName, boolean saveWorlds) {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();

        ISlimeWorld slimeWorld = islandWorlds.remove(worldName);
        if (slimeWorld != null) {
            if (saveWorlds) {
                try {
                    slimeWorld.serialize().whenComplete((serializeData, error) -> {
                        if (error == null) {
                            try {
                                slimeWorld.getLoader().saveWorld(worldName, serializeData);
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }

                            completableFuture.complete(Bukkit.unloadWorld(worldName, true));
                        } else {
                            completableFuture.completeExceptionally(error);
                        }
                    });
                } catch (IOException error) {
                    completableFuture.completeExceptionally(error);
                }
            } else {
                return CompletableFuture.completedFuture(Bukkit.unloadWorld(worldName, false));
            }
        }

        return completableFuture;
    }

    public static String getWorldName(UUID islandUUID, World.Environment environment) {
        return "island_" + islandUUID + "_" + environment.name().toLowerCase();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean isIslandWorldName(String worldName) {
        String[] nameSections = worldName.split("_");

        if (nameSections.length != 3)
            return false;

        try {
            UUID.fromString(nameSections[1]);
            World.Environment.valueOf(nameSections[2]);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

}
