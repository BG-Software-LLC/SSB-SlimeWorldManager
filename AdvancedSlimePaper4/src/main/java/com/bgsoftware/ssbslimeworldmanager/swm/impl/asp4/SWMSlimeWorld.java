package com.bgsoftware.ssbslimeworldmanager.swm.impl.asp4;

import com.bgsoftware.ssbslimeworldmanager.api.ISlimeWorld;
import com.infernalsuite.asp.api.world.SlimeWorld;

public record SWMSlimeWorld(SlimeWorld handle) implements ISlimeWorld {

    @Override
    public String getName() {
        return handle.getName();
    }

}
