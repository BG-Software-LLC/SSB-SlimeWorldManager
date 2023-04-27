package com.bgsoftware.ssbslimeworldmanager.hook;

import com.bgsoftware.ssbslimeworldmanager.utils.SlimeUtils;
import com.bgsoftware.ssbslimeworldmanager.SlimeWorldModule;
import com.bgsoftware.ssbslimeworldmanager.swm.ISlimeWorld;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.schematic.Schematic;
import com.bgsoftware.superiorskyblock.api.world.algorithm.IslandCreationAlgorithm;
import com.bgsoftware.superiorskyblock.api.wrappers.BlockPosition;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.world.WorldLoadEvent;

import java.util.Objects;
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
    public CompletableFuture<IslandCreationResult> createIsland(UUID uuid, SuperiorPlayer owner,
                                                                BlockPosition blockPosition, String name,
                                                                Schematic schematic) {
        return createIsland(Island.newBuilder().setOwner(owner).setUniqueId(uuid).setName(name).setSchematicName(schematic.getName()), blockPosition);
    }

    @Override
    public CompletableFuture<IslandCreationResult> createIsland(Island.Builder builder, BlockPosition blockPosition) {
        Schematic schematic = builder.getScehmaticName() == null ? null : module.getPlugin().getSchematics().getSchematic(builder.getScehmaticName());
        Objects.requireNonNull(schematic, "Cannot create an island from builder with invalid schematic name.");

        World.Environment environment = module.getPlugin().getSettings().getWorlds().getDefaultWorld();
        String worldName = SlimeUtils.getWorldName(builder.getUniqueId(), environment);

        CompletableFuture<IslandCreationResult> completableFuture = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(module.getPlugin(), () -> {
            // Loading the world asynchronous.
            ISlimeWorld slimeWorld = this.module.getSlimeAdapter().createOrLoadWorld(worldName, environment);
            Bukkit.getScheduler().runTask(module.getPlugin(), () -> {
                // Generating the world synchronized
                this.module.getSlimeAdapter().generateWorld(slimeWorld);

                Bukkit.getPluginManager().callEvent(new WorldLoadEvent(Bukkit.getWorld(slimeWorld.getName())));

                // We run the original logic now
                originalAlgorithm.createIsland(builder, blockPosition).whenComplete((result, error) -> {
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
