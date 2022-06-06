package com.bgsoftware.ssbslimeworldmanager.swm.impl.swm;

import com.bgsoftware.ssbslimeworldmanager.swm.ISlimeLoader;
import com.grinderwolf.swm.api.loaders.SlimeLoader;

import java.io.IOException;
import java.util.function.Supplier;

public final class SWMSlimeLoader implements ISlimeLoader {

    private final Supplier<SlimeLoader> handle;

    public SWMSlimeLoader(Supplier<SlimeLoader> handle) {
        this.handle = handle;
    }

    @Override
    public void saveWorld(String worldName, byte[] data) throws IOException {
        handle.get().saveWorld(worldName, data, true);
    }

}
