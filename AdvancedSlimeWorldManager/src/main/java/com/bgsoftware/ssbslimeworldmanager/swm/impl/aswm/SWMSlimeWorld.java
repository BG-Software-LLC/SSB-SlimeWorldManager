package com.bgsoftware.ssbslimeworldmanager.swm.impl.aswm;

import com.bgsoftware.ssbslimeworldmanager.swm.ISlimeWorld;
import com.grinderwolf.swm.api.world.SlimeWorld;

public record SWMSlimeWorld(SlimeWorld handle) implements ISlimeWorld {

    @Override
    public String getName() {
        return handle.getName();
    }

}
