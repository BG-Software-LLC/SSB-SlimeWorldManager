package com.bgsoftware.ssbslimeworldmanager.providers;

import com.bgsoftware.ssbslimeworldmanager.SlimeWorldModule;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.LinkedList;
import java.util.List;

public class ProvidersManager {

    private final List<IWorldKeeper> worldKeeperList = new LinkedList<>();

    private final SlimeWorldModule module;

    public ProvidersManager(SlimeWorldModule module) {
        this.module = module;
    }

    public void loadHooks() {
        Bukkit.getScheduler().runTaskLater(module.getPlugin(), this::loadHooksInternal, 1L);
    }

    private void loadHooksInternal() {
        if(Bukkit.getPluginManager().isPluginEnabled("WildLoaders"))
            worldKeeperList.add(new WorldKeeper_WildLoaders());
    }

    public boolean shouldKeepWorldLoaded(World world) {
        for (IWorldKeeper worldKeeper : worldKeeperList) {
            if (worldKeeper.shouldKeepWorldLoaded(world))
                return true;
        }

        return false;
    }


}
