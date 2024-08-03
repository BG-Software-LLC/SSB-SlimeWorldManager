package com.bgsoftware.ssbslimeworldmanager.listeners;

import com.bgsoftware.ssbslimeworldmanager.SlimeWorldModule;
import com.bgsoftware.superiorskyblock.api.events.IslandDisbandEvent;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.world.Dimension;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.concurrent.atomic.AtomicBoolean;

public class IslandsListener implements Listener {

    private final SlimeWorldModule module;

    public IslandsListener(SlimeWorldModule module) {
        this.module = module;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandDisband(IslandDisbandEvent event) {
        Bukkit.getScheduler().runTaskLater(module.getPlugin(), () -> {
            // We want to delete the worlds one tick later, so the plugin will not try and load the worlds again
            for (Dimension dimension : Dimension.values()) {
                if (isWorldGeneratedForIsland(event.getIsland(), dimension))
                    module.getSlimeAdapter().deleteWorld(event.getIsland(), dimension);
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

            AtomicBoolean teleportedToIsland = new AtomicBoolean(false);

            // We want to load the worlds of the player's island.
            for (Dimension dimension : Dimension.values()) {
                if (isWorldGeneratedForIsland(island, dimension)) {
                    module.getSlimeWorldsProvider().getSlimeWorldAsBukkitAsync(island.getUniqueId(), dimension).whenComplete((world, error) -> {
                        if (teleportToIsland && !teleportedToIsland.get()) {
                            superiorPlayer.teleport(island);
                            teleportedToIsland.set(true);
                        }
                    });
                }
            }

            // Because it takes time for the worlds to load, we teleport them to spawn in the time being.
            if (teleportToIsland && !teleportedToIsland.get())
                superiorPlayer.teleport(module.getPlugin().getGrid().getSpawnIsland());
        }
    }

    private static boolean isWorldGeneratedForIsland(Island island, Dimension dimension) {
        if (!island.wasSchematicGenerated(dimension))
            return false;

        return island.isDimensionEnabled(dimension);
    }

}
