package com.bgsoftware.ssbslimeworldmanager;

import com.bgsoftware.superiorskyblock.api.events.IslandDisbandEvent;
import com.bgsoftware.superiorskyblock.api.events.PluginInitializeEvent;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public final class SSBSlimeWorldManager extends JavaPlugin implements Listener {

    public static SSBSlimeWorldManager plugin;

    @Override
    public void onEnable() {
        plugin = this;

        SlimeUtils.init();

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        SlimeUtils.unloadAllWorlds();
    }

    @EventHandler
    public void onSSBInit(PluginInitializeEvent e){
        e.getPlugin().getProviders().setWorldsProvider(SSBWorldManager.createManager());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void onIslandDelete(IslandDisbandEvent e){
        Arrays.stream(World.Environment.values()).forEach(environment -> SlimeUtils.deleteWorld(e.getIsland(), environment));
    }

}
