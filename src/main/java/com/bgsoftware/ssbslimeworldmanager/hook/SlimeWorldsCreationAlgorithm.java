package com.bgsoftware.ssbslimeworldmanager.hook;

import com.bgsoftware.ssbslimeworldmanager.SlimeUtils;
import com.bgsoftware.ssbslimeworldmanager.SlimeWorldModule;
import com.bgsoftware.ssbslimeworldmanager.swm.ISlimeWorld;
import com.bgsoftware.superiorskyblock.api.schematic.Schematic;
import com.bgsoftware.superiorskyblock.api.world.algorithm.IslandCreationAlgorithm;
import com.bgsoftware.superiorskyblock.api.wrappers.BlockPosition;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SlimeWorldsCreationAlgorithm implements IslandCreationAlgorithm {

    private final SlimeWorldModule module;
    private final IslandCreationAlgorithm originalAlgorithm;

    public SlimeWorldsCreationAlgorithm(SlimeWorldModule module, IslandCreationAlgorithm originalAlgorithm) {
        this.module = module;
        this.originalAlgorithm = originalAlgorithm;
    }

    @Override
    public CompletableFuture<IslandCreationResult> createIsland(UUID uuid, SuperiorPlayer superiorPlayer,
                                                                BlockPosition blockPosition, String name,
                                                                Schematic schematic) {
        World.Environment environment = module.getPlugin().getSettings().getWorlds().getDefaultWorld();
        String worldName = SlimeUtils.getWorldName(uuid, environment);

        CompletableFuture<IslandCreationResult> completableFuture = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(module.getPlugin(), () -> {
            // Loading the world asynchronous.
            ISlimeWorld slimeWorld = this.module.getSlimeAdapter().loadWorld(worldName, environment);
            Bukkit.getScheduler().runTask(module.getPlugin(), () -> {
                // Generating the world synchronized
                this.module.getSlimeAdapter().generateWorld(slimeWorld);
                // We run the original logic now
                originalAlgorithm.createIsland(uuid, superiorPlayer, blockPosition, name, schematic).whenComplete((result, error) -> {
                    if (error != null) {
                        completableFuture.completeExceptionally(error);
                    } else {
                        completableFuture.complete(result);
                    }
                });
            });
        });

        return completableFuture;
    }

}
