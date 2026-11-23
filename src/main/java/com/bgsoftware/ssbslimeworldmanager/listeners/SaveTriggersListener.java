package com.bgsoftware.ssbslimeworldmanager.listeners;

import com.bgsoftware.ssbslimeworldmanager.SlimeWorldModule;
import com.bgsoftware.ssbslimeworldmanager.save.SaveReason;
import com.bgsoftware.superiorskyblock.api.events.IslandBiomeChangeEvent;
import com.bgsoftware.superiorskyblock.api.events.IslandLeaveEvent;
import com.bgsoftware.superiorskyblock.api.events.IslandSchematicPasteEvent;
import com.bgsoftware.superiorskyblock.api.events.IslandUpgradeEvent;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Requests saves of island worlds when something meaningful happened to them.
 * Every request is throttled by the `auto-save.min-delay` setting.
 */
public class SaveTriggersListener implements Listener {

    private final SlimeWorldModule module;

    public SaveTriggersListener(SlimeWorldModule module) {
        this.module = module;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandLeave(IslandLeaveEvent event) {
        module.getSaveService().requestSave(event.getIsland(), SaveReason.ISLAND_LEAVE);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandUpgrade(IslandUpgradeEvent event) {
        module.getSaveService().requestSave(event.getIsland(), SaveReason.ISLAND_UPGRADE);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandSchematicPaste(IslandSchematicPasteEvent event) {
        module.getSaveService().requestSave(event.getIsland(), SaveReason.SCHEMATIC_PASTE);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandBiomeChange(IslandBiomeChangeEvent event) {
        module.getSaveService().requestSave(event.getIsland(), SaveReason.BIOME_CHANGE);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        World from = event.getFrom();
        if (from.getPlayers().isEmpty())
            module.getSaveService().requestSave(from, SaveReason.WORLD_EMPTY);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        World world = event.getPlayer().getWorld();
        // The quitting player is still counted as a player of the world at this point.
        if (world.getPlayers().size() <= 1)
            module.getSaveService().requestSave(world, SaveReason.WORLD_EMPTY);
    }

}
