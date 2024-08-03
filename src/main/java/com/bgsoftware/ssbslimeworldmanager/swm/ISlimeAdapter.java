package com.bgsoftware.ssbslimeworldmanager.swm;

import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.world.Dimension;
import org.bukkit.World;

import java.io.IOException;
import java.util.List;

public interface ISlimeAdapter {

    List<String> getSavedWorlds() throws IOException;

    ISlimeWorld createOrLoadWorld(String worldName, World.Environment environment);

    void generateWorld(ISlimeWorld slimeWorld);

    void deleteWorld(Island island, Dimension dimension);

}
