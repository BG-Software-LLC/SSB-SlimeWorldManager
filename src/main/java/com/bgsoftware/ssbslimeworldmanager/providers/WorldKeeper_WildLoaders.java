package com.bgsoftware.ssbslimeworldmanager.providers;

import com.bgsoftware.wildloaders.api.WildLoadersAPI;
import org.bukkit.Chunk;
import org.bukkit.World;

public class WorldKeeper_WildLoaders implements IWorldKeeper {

    @Override
    public boolean shouldKeepWorldLoaded(World world) {
        for (Chunk chunk : world.getLoadedChunks()) {
            if (WildLoadersAPI.getChunkLoader(chunk).isPresent())
                return true;
        }

        return false;
    }

}
