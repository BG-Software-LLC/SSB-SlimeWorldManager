package com.bgsoftware.ssbslimeworldmanager.listeners;

import com.bgsoftware.ssbslimeworldmanager.api.SlimeUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;

public class WorldsListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent e) {
        SlimeUtils.notifyUnloadWorld(e.getWorld());
    }

}
