package com.bgsoftware.ssbslimeworldmanager.listeners;

import com.bgsoftware.ssbslimeworldmanager.SlimeWorldModule;
import com.bgsoftware.superiorskyblock.api.events.IslandDisbandEvent;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class IslandsListener implements Listener {

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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        SuperiorPlayer superiorPlayer = module.getPlugin().getPlayers().getSuperiorPlayer(event.getPlayer());
        Island island = superiorPlayer.getIsland();
        if (island != null) {
            Location defaultWorldTeleportLocation = event.getPlayer().getLocation();
            defaultWorldTeleportLocation.setWorld(Bukkit.getWorlds().get(0));
            // We check if the player was teleported to the default world.
            // If so, we teleport them to their island again.
            boolean teleportToIsland = defaultWorldTeleportLocation.equals(superiorPlayer.getLocation());

            // We want to load the worlds of the player's island.
            for (World.Environment environment : World.Environment.values()) {
                if (isWorldEnabledForIsland(island, environment))
                    module.getSlimeWorldsProvider().getSlimeWorldAsBukkitAsync(island.getUniqueId(), environment).whenComplete((world, error) -> {
                        if (teleportToIsland)
                            superiorPlayer.teleport(island);
                    });
            }

            // Because it takes time for the worlds to load, we teleport them to spawn in the time being.
            if (teleportToIsland)
                superiorPlayer.teleport(module.getPlugin().getGrid().getSpawnIsland());
        }
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
