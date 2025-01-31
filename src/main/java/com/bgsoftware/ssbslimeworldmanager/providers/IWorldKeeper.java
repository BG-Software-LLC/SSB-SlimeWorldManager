package com.bgsoftware.ssbslimeworldmanager.providers;

import org.bukkit.World;

public interface IWorldKeeper {

    boolean shouldKeepWorldLoaded(World world);

}
