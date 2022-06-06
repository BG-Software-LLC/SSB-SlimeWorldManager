package com.bgsoftware.ssbslimeworldmanager.swm;

import com.bgsoftware.ssbslimeworldmanager.SlimeUtils;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblock;
import com.bgsoftware.superiorskyblock.api.island.Island;
import org.bukkit.World;

import java.util.UUID;

public interface ISlimeAdapter {

    void unloadAllWorlds();

    default ISlimeWorld loadAndGetWorld(Island island, World.Environment environment) {
        return loadAndGetWorld(island.getUniqueId(), environment);
    }

    default ISlimeWorld loadAndGetWorld(UUID islandUUID, World.Environment environment) {
        return loadAndGetWorld(SlimeUtils.getWorldName(islandUUID, environment), environment);
    }

    ISlimeWorld loadAndGetWorld(String worldName, World.Environment environment);

    void deleteWorld(SuperiorSkyblock plugin, Island island, World.Environment environment);

}
