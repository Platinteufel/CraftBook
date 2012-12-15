package com.sk89q.craftbook.gates.world.blocks;

import org.bukkit.Server;

import com.sk89q.craftbook.ChangedSign;
import com.sk89q.craftbook.ic.ChipState;
import com.sk89q.craftbook.ic.IC;
import com.sk89q.craftbook.ic.ICFactory;
import com.sk89q.craftbook.ic.SelfTriggeredIC;

public class SetBlockBelowST extends SetBlockBelow implements SelfTriggeredIC {

    public SetBlockBelowST (Server server, ChangedSign sign, ICFactory factory) {
        super(server, sign, factory);
    }

    @Override
    public String getTitle() {

        return "Self-Triggered Set Block Below";
    }

    @Override
    public String getSignTitle() {

        return "SET BLOCK BELOW ST";
    }

    @Override
    public boolean isActive () {
        return true;
    }

    @Override
    public void think (ChipState chip) {
        onTrigger();
    }

    public static class Factory extends SetBlockBelow.Factory {

        public Factory(Server server) {

            super(server);
        }

        @Override
        public IC create(ChangedSign sign) {

            return new SetBlockBelowST(getServer(), sign, this);
        }
    }
}