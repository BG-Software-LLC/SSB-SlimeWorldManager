package com.bgsoftware.ssbslimeworldmanager.listeners;

import com.bgsoftware.ssbslimeworldmanager.SlimeWorldModule;
import com.bgsoftware.superiorskyblock.api.events.IslandDisbandEvent;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.world.Dimension;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class IslandsListener implements Listener {

    private static final int MAX_ATTEMPTS_ON_WORLDS_DELETION = 10;

    private final SlimeWorldModule module;

    public IslandsListener(SlimeWorldModule module) {
        this.module = module;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandDisband(IslandDisbandEvent event) {
        List<Dimension> enabledDimensions = new LinkedList<>();
        for (Dimension dimension : Dimension.values()) {
            if (isWorldGeneratedForIsland(event.getIsland(), dimension))
                enabledDimensions.add(dimension);
        }

        deleteWorldsForIsland(event.getIsland(), enabledDimensions, 0);
    }

    private void deleteWorldsForIsland(Island island, List<Dimension> dimensions, int attemptNumber) {
        Bukkit.getScheduler().runTaskLater(module.getPlugin(), () -> {
            for (Dimension dimension : dimensions) {
                if (!module.getSlimeAdapter().deleteWorld(island, dimension)) {
                    if (attemptNumber < MAX_ATTEMPTS_ON_WORLDS_DELETION)
                        deleteWorldsForIsland(island, dimensions, attemptNumber + 1);
                    return;
                }
            }
        }, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        SuperiorPlayer superiorPlayer = module.getPlugin().getPlayers().getSuperiorPlayer(event.getPlayer());
        Island island = superiorPlayer.getIsland();
        if (island != null) {
            boolean teleportToIsland = checkIfTeleportToIsland(event.getPlayer());

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

    private boolean checkIfTeleportToIsland(Player player) {
        if (!module.getSettings().teleportBackToIsland)
            return false;

        // We check if the player was teleported to the default world.
        // If so, we teleport them to their island again.
        Location defaultWorldTeleportLocation = player.getLocation();
        return defaultWorldTeleportLocation.getWorld().equals(Bukkit.getWorlds().get(0));
    }

}
