package com.bgsoftware.ssbslimeworldmanager.swm.impl.asp3;

import com.bgsoftware.ssbslimeworldmanager.api.ISlimeAdapter;
import com.bgsoftware.ssbslimeworldmanager.api.ISlimeWorld;
import com.bgsoftware.ssbslimeworldmanager.api.SlimeUtils;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblock;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.world.Dimension;
import com.google.common.base.Preconditions;
import com.infernalsuite.aswm.api.AdvancedSlimePaperAPI;
import com.infernalsuite.aswm.api.exceptions.CorruptedWorldException;
import com.infernalsuite.aswm.api.exceptions.NewerFormatException;
import com.infernalsuite.aswm.api.exceptions.UnknownWorldException;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.world.properties.SlimeProperties;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import com.infernalsuite.aswm.loaders.file.FileLoader;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public class SWMAdapter implements ISlimeAdapter {

    private static final SlimeLoader SLIME_LOADER = new FileLoader(new File("slime_worlds"));

    private final SuperiorSkyblock plugin;

    public SWMAdapter(SuperiorSkyblock plugin, String dataSource) {
        this.plugin = plugin;
    }

    @Override
    public List<String> getSavedWorlds() throws IOException {
        return SLIME_LOADER.listWorlds();
    }

    @Override
    public ISlimeWorld createOrLoadWorld(String worldName, World.Environment environment) {
        ISlimeWorld slimeWorld = SlimeUtils.getSlimeWorld(worldName);

        if (slimeWorld == null) {
            SlimePropertyMap properties = new SlimePropertyMap();

            try {
                if (SLIME_LOADER.worldExists(worldName)) {
                    slimeWorld = new SWMSlimeWorld(AdvancedSlimePaperAPI.instance().readWorld(
                            SLIME_LOADER, worldName, false, properties));
                } else {
                    // set the default island properties accordingly
                    properties.setValue(SlimeProperties.DIFFICULTY, plugin.getSettings().getWorlds().getDifficulty().toLowerCase(Locale.ENGLISH));
                    properties.setValue(SlimeProperties.ENVIRONMENT, environment.name().toLowerCase(Locale.ENGLISH));

                    slimeWorld = new SWMSlimeWorld(AdvancedSlimePaperAPI.instance().createEmptyWorld(
                            worldName, false, properties, SLIME_LOADER));
                }

                SlimeUtils.setSlimeWorld(worldName, slimeWorld);
            } catch (IOException | CorruptedWorldException | NewerFormatException | UnknownWorldException exception) {
                plugin.getLogger().log(Level.SEVERE, "An exception occurred while trying to create or load world: " + worldName, exception);
            }
        }

        return slimeWorld;
    }

    @Override
    public void generateWorld(ISlimeWorld slimeWorld) {
        Preconditions.checkState(Bukkit.isPrimaryThread(), "cannot generate worlds async.");
        AdvancedSlimePaperAPI.instance().loadWorld(((SWMSlimeWorld) slimeWorld).handle(), false);
    }

    @Override
    public void deleteWorld(Island island, Dimension dimension) {
        String worldName = SlimeUtils.getWorldName(island.getUniqueId(), dimension);

        if (Bukkit.unloadWorld(worldName, false)) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    SLIME_LOADER.deleteWorld(worldName);
                } catch (UnknownWorldException ignored) {
                    // World was not saved yet, who cares.
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            });
        }
    }

}
