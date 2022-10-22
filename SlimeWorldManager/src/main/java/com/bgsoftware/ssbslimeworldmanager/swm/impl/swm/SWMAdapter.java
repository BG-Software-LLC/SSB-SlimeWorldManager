package com.bgsoftware.ssbslimeworldmanager.swm.impl.swm;

import com.bgsoftware.ssbslimeworldmanager.utils.SlimeUtils;
import com.bgsoftware.ssbslimeworldmanager.swm.ISlimeAdapter;
import com.bgsoftware.ssbslimeworldmanager.swm.ISlimeWorld;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblock;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.google.common.base.Preconditions;
import com.grinderwolf.swm.api.SlimePlugin;
import com.grinderwolf.swm.api.exceptions.*;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public final class SWMAdapter implements ISlimeAdapter {

    private final SuperiorSkyblock plugin;
    private final SlimePlugin slimePlugin;
    private final SlimeLoader slimeLoader;

    public SWMAdapter(SuperiorSkyblock plugin, String dataSource) {
        this.plugin = plugin;
        this.slimePlugin = (SlimePlugin) Bukkit.getPluginManager().getPlugin("SlimeWorldManager");
        this.slimeLoader = this.slimePlugin.getLoader(dataSource);
    }

    @Override
    public List<String> getSavedWorlds() throws IOException {
        return slimeLoader.listWorlds();
    }

    @Override
    public ISlimeWorld createOrLoadWorld(String worldName, World.Environment environment) {
        ISlimeWorld slimeWorld = SlimeUtils.getSlimeWorld(worldName);

        if (slimeWorld == null) {
            SlimePropertyMap properties = new SlimePropertyMap();

            try {
                if (slimeLoader.worldExists(worldName)) {
                    slimeWorld = new SWMSlimeWorld(slimePlugin.loadWorld(slimeLoader, worldName, false, properties));
                } else {
                    // set the default island properties accordingly
                    properties.setString(SlimeProperties.DIFFICULTY, plugin.getSettings().getWorlds().getDifficulty().toLowerCase(Locale.ENGLISH));
                    properties.setString(SlimeProperties.ENVIRONMENT, environment.name().toLowerCase(Locale.ENGLISH));

                    slimeWorld = new SWMSlimeWorld(slimePlugin.createEmptyWorld(slimeLoader, worldName, false, properties));
                }

                SlimeUtils.setSlimeWorld(worldName, slimeWorld);
            } catch (IOException | CorruptedWorldException | NewerFormatException | WorldInUseException |
                     UnknownWorldException | WorldAlreadyExistsException exception) {
                plugin.getLogger().log(Level.SEVERE, "An exception occurred while trying to create or load world: " + worldName, exception);
            }
        }

        return slimeWorld;
    }

    @Override
    public void generateWorld(ISlimeWorld slimeWorld) {
        Preconditions.checkState(Bukkit.isPrimaryThread(), "cannot generate worlds async.");
        slimePlugin.generateWorld(((SWMSlimeWorld) slimeWorld).getHandle());
    }

    @Override
    public void deleteWorld(Island island, World.Environment environment) {
        String worldName = SlimeUtils.getWorldName(island.getUniqueId(), environment);

        if(Bukkit.unloadWorld(worldName, false)) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    slimeLoader.deleteWorld(worldName);
                } catch (UnknownWorldException | IOException exception) {
                    throw new RuntimeException(exception);
                }
            });
        }
    }

}
