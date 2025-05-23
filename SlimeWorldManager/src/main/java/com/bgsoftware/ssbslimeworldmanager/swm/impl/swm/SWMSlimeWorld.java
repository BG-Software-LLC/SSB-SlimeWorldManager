package com.bgsoftware.ssbslimeworldmanager.swm.impl.swm;

import com.bgsoftware.ssbslimeworldmanager.api.ISlimeWorld;
import com.grinderwolf.swm.api.world.SlimeWorld;

public class SWMSlimeWorld implements ISlimeWorld {

    private final SlimeWorld handle;

    public SWMSlimeWorld(SlimeWorld handle) {
        this.handle = handle;
    }

    @Override
    public String getName() {
        return handle.getName();
    }

    public SlimeWorld getHandle() {
        return handle;
    }

}
