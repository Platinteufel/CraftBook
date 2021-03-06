/*
 * CraftBook Copyright (C) 2010-2017 sk89q <http://www.sk89q.com>
 * CraftBook Copyright (C) 2011-2017 me4502 <http://www.me4502.com>
 * CraftBook Copyright (C) Contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not,
 * see <http://www.gnu.org/licenses/>.
 */
package com.sk89q.craftbook.sponge.mechanics.minecart.block;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.me4502.modularframework.module.Module;
import com.me4502.modularframework.module.guice.ModuleConfiguration;
import com.sk89q.craftbook.core.util.ConfigValue;
import com.sk89q.craftbook.core.util.CraftBookException;
import com.sk89q.craftbook.core.util.PermissionNode;
import com.sk89q.craftbook.core.util.TernaryState;
import com.sk89q.craftbook.core.util.documentation.DocumentationProvider;
import com.sk89q.craftbook.sponge.util.BlockFilter;
import com.sk89q.craftbook.sponge.util.SignUtil;
import com.sk89q.craftbook.sponge.util.SpongePermissionNode;
import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.vehicle.minecart.Minecart;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.List;

@Module(id = "cartejector", name = "CartEjector", onEnable="onInitialize", onDisable="onDisable")
public class CartEjector extends SpongeCartBlockMechanic implements DocumentationProvider {

    @Inject
    @ModuleConfiguration
    public ConfigurationNode config;

    private SpongePermissionNode createPermissions = new SpongePermissionNode("craftbook.cartejector", "Allows the user to create the " + getName() + " mechanic.", PermissionDescription.ROLE_USER);

    private ConfigValue<BlockFilter> allowedBlocks = new ConfigValue<>("material", "The block that this mechanic requires.", new BlockFilter("IRON_BLOCK"), TypeToken.of(BlockFilter.class));

    @Override
    public void onInitialize() throws CraftBookException {
        super.onInitialize();

        allowedBlocks.load(config);

        createPermissions.register();
    }

    @Override
    public void impact(Minecart minecart, CartMechanismBlocks blocks, boolean minor) {
        super.impact(minecart, blocks, minor);

        if (minecart.getPassengers().isEmpty() || isActive(blocks) == TernaryState.FALSE) {
            return;
        }

        Location<World> teleportTarget = null;
        if (blocks.matches("eject")) {
            teleportTarget = blocks.getRail().getRelative(SignUtil.getFront(blocks.getSign()));
        }

        List<Entity> passengers = minecart.getPassengers();
        minecart.clearPassengers();
        if (teleportTarget != null) {
            for (Entity passenger : passengers) {
                passenger.setLocationSafely(teleportTarget);
            }
        }
    }

    @Override
    public String[] getValidSigns() {
        return new String[] {
                "[Eject]"
        };
    }

    @Override
    public SpongePermissionNode getCreatePermission() {
        return createPermissions;
    }

    @Override
    public BlockFilter getBlockFilter() {
        return allowedBlocks.getValue();
    }

    @Override
    public String getPath() {
        return "mechanics/minecart/block/ejector";
    }

    @Override
    public ConfigValue<?>[] getConfigurationNodes() {
        return new ConfigValue[] {
                allowedBlocks
        };
    }

    @Override
    public PermissionNode[] getPermissionNodes() {
        return new PermissionNode[] {
                createPermissions
        };
    }
}
