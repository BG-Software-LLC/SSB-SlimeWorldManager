package com.bgsoftware.ssbslimeworldmanager.swm.impl.swm;

import com.bgsoftware.ssbslimeworldmanager.swm.ISlimeLoader;
import com.bgsoftware.ssbslimeworldmanager.swm.ISlimeWorld;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.nms.CraftSlimeWorld;

import java.util.concurrent.CompletableFuture;

public final class SWMSlimeWorld implements ISlimeWorld {

    private final SlimeWorld handle;
    private final SWMSlimeLoader slimeLoader;

    public SWMSlimeWorld(SlimeWorld handle) {
        this.handle = handle;
        this.slimeLoader = new SWMSlimeLoader(this.handle::getLoader);
    }

    @Override
    public String getName() {
        return handle.getName();
    }

    @Override
    public ISlimeLoader getLoader() {
        return slimeLoader;
    }

    @Override
    public CompletableFuture<byte[]> serialize() {
        return CompletableFuture.completedFuture(((CraftSlimeWorld) handle).serialize());
    }

    public SlimeWorld getHandle() {
        return handle;
    }

}
