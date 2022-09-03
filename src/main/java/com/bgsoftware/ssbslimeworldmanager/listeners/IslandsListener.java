package com.bgsoftware.ssbslimeworldmanager.listeners;

import com.bgsoftware.ssbslimeworldmanager.SlimeWorldModule;
import com.bgsoftware.superiorskyblock.api.events.IslandDisbandEvent;
import com.bgsoftware.superiorskyblock.api.island.Island;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class IslandsListener implements Listener {

    private final SlimeWorldModule module;

    public IslandsListener(SlimeWorldModule module) {
        this.module = module;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandDisband(IslandDisbandEvent event) {
        Bukkit.getScheduler().runTaskLater(module.getPlugin(), () -> {
            // We want to delete the worlds one tick later, so the plugin will not try and load the worlds again
            for (World.Environment environment : World.Environment.values()) {
                if (isWorldEnabledForIsland(event.getIsland(), environment))
                    module.getSlimeAdapter().deleteWorld(event.getIsland(), environment);
            }
        }, 1L);
    }

    private static boolean isWorldEnabledForIsland(Island island, World.Environment environment) {
        switch (environment) {
            case NORMAL:
                return island.isNormalEnabled();
            case NETHER:
                return island.isNetherEnabled();
            case THE_END:
                return island.isEndEnabled();
            default:
                return false;
        }
    }

}
