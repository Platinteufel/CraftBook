package com.sk89q.craftbook.bukkit;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;

import com.sk89q.craftbook.CraftBookMechanic;
import com.sk89q.craftbook.LocalComponent;
import com.sk89q.craftbook.bukkit.commands.MechanismCommands;
import com.sk89q.craftbook.mech.Ammeter;
import com.sk89q.craftbook.mech.BetterLeads;
import com.sk89q.craftbook.mech.BetterPhysics;
import com.sk89q.craftbook.mech.BetterPistons;
import com.sk89q.craftbook.mech.BetterPistons.Types;
import com.sk89q.craftbook.mech.Bookcase;
import com.sk89q.craftbook.mech.Bridge;
import com.sk89q.craftbook.mech.Cauldron;
import com.sk89q.craftbook.mech.Chair;
import com.sk89q.craftbook.mech.ChunkAnchor;
import com.sk89q.craftbook.mech.CommandItems;
import com.sk89q.craftbook.mech.CommandSigns;
import com.sk89q.craftbook.mech.CookingPot;
import com.sk89q.craftbook.mech.CustomDrops;
import com.sk89q.craftbook.mech.Door;
import com.sk89q.craftbook.mech.Elevator;
import com.sk89q.craftbook.mech.Footprints;
import com.sk89q.craftbook.mech.Gate;
import com.sk89q.craftbook.mech.HeadDrops;
import com.sk89q.craftbook.mech.HiddenSwitch;
import com.sk89q.craftbook.mech.LightStone;
import com.sk89q.craftbook.mech.LightSwitch;
import com.sk89q.craftbook.mech.MapChanger;
import com.sk89q.craftbook.mech.PaintingSwitch;
import com.sk89q.craftbook.mech.Payment;
import com.sk89q.craftbook.mech.SignCopier;
import com.sk89q.craftbook.mech.Snow;
import com.sk89q.craftbook.mech.Teleporter;
import com.sk89q.craftbook.mech.TreeLopper;
import com.sk89q.craftbook.mech.XPStorer;
import com.sk89q.craftbook.mech.ai.AIMechanic;
import com.sk89q.craftbook.mech.area.Area;
import com.sk89q.craftbook.mech.area.CopyManager;
import com.sk89q.craftbook.mech.cauldron.ImprovedCauldron;
import com.sk89q.craftbook.mech.crafting.CustomCrafting;
import com.sk89q.craftbook.mech.dispenser.DispenserRecipes;
import com.sk89q.craftbook.mech.dispenser.Recipe;

/**
 * Author: Turtle9598
 */
@SuppressWarnings("deprecation")
public class MechanicalCore implements LocalComponent {

    private static MechanicalCore instance;

    private CraftBookPlugin plugin = CraftBookPlugin.inst();
    private final CopyManager copyManager = new CopyManager();
    private CustomCrafting customCrafting;

    private List<CraftBookMechanic> mechanics;

    public static boolean isEnabled() {

        return instance != null;
    }

    public MechanicalCore() {

        instance = this;
    }

    public static MechanicalCore inst() {

        return instance;
    }

    @Override
    public void enable() {

        plugin.registerCommands(MechanismCommands.class);
        mechanics = new ArrayList<CraftBookMechanic>();

        registerMechanics();
    }

    @Override
    public void disable() {

        customCrafting = null;
        Iterator<String> it = Elevator.flyingPlayers.iterator();
        while(it.hasNext()) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(it.next());
            if(!op.isOnline()) {
                it.remove();
                continue;
            }
            op.getPlayer().setFlying(false);
            op.getPlayer().setAllowFlight(op.getPlayer().getGameMode() == GameMode.CREATIVE);
            it.remove();
        }
        for(CraftBookMechanic mech : mechanics)
            mech.disable();
        mechanics = null;
        instance = null;
    }

    public CopyManager getCopyManager() {

        return copyManager;
    }

    public CustomCrafting getCustomCrafting() {

        return customCrafting;
    }

    private void registerMechanics() {

        BukkitConfiguration config = plugin.getConfiguration();

        // Let's register mechanics!

        //Register Chunk Anchors first so that they are always the first mechanics to be checked for, allowing other mechanics to stay loaded in unloaded chunks.
        if (config.chunkAnchorEnabled) plugin.registerMechanic(new ChunkAnchor.Factory());

        if (config.signCopyEnabled) plugin.registerMechanic(new SignCopier.Factory()); // Keep SignCopy close to the start, so it can copy mechanics without triggering them.
        if (config.ammeterEnabled) plugin.registerMechanic(new Ammeter.Factory());
        if (config.bookcaseEnabled) {
            plugin.createDefaultConfiguration(new File(plugin.getDataFolder(), "books.txt"), "books.txt");
            plugin.registerMechanic(new Bookcase.Factory());
        }
        if (config.gateEnabled) plugin.registerMechanic(new Gate.Factory());
        if (config.bridgeEnabled) plugin.registerMechanic(new Bridge.Factory());
        if (config.doorEnabled) plugin.registerMechanic(new Door.Factory());
        if (config.elevatorEnabled) plugin.registerMechanic(new Elevator.Factory());
        if (config.teleporterEnabled) plugin.registerMechanic(new Teleporter.Factory());
        if (config.areaEnabled) plugin.registerMechanic(new Area.Factory());
        if (config.commandSignEnabled) plugin.registerMechanic(new CommandSigns.Factory());
        if (config.lightstoneEnabled) plugin.registerMechanic(new LightStone.Factory());
        if (config.lightSwitchEnabled) plugin.registerMechanic(new LightSwitch.Factory());
        if (config.hiddenSwitchEnabled) plugin.registerMechanic(new HiddenSwitch.Factory());
        if (config.cookingPotEnabled) plugin.registerMechanic(new CookingPot.Factory());
        if (config.legacyCauldronEnabled) plugin.registerMechanic(new Cauldron.Factory());
        if (config.cauldronEnabled) plugin.registerMechanic(new ImprovedCauldron.Factory());
        if (config.xpStorerEnabled) plugin.registerMechanic(new XPStorer.Factory());
        if (config.mapChangerEnabled) plugin.registerMechanic(new MapChanger.Factory());
        if (config.treeLopperEnabled) plugin.registerMechanic(new TreeLopper.Factory());
        for(Types type : BetterPistons.Types.values())
            if (config.pistonsEnabled && Types.isEnabled(type)) plugin.registerMechanic(new BetterPistons.Factory(type));

        // Special mechanics.
        if (plugin.getEconomy() != null && config.paymentEnabled) plugin.registerMechanic(new Payment.Factory());

        // New System Mechanics
        if (config.customCraftingEnabled) mechanics.add(customCrafting = new CustomCrafting());
        if (config.customDispensingEnabled) mechanics.add(new DispenserRecipes());
        if (config.snowPiling || config.snowPlace) mechanics.add(new Snow());
        if (config.customDropEnabled) mechanics.add(new CustomDrops());
        if (config.aiEnabled) mechanics.add(new AIMechanic());
        if (config.chairEnabled) mechanics.add(new Chair());
        if (config.footprintsEnabled) mechanics.add(new Footprints());
        if (config.paintingsEnabled) mechanics.add(new PaintingSwitch());
        if (config.physicsEnabled) mechanics.add(new BetterPhysics());
        if (config.headDropsEnabled) mechanics.add(new HeadDrops());
        if (config.commandItemsEnabled) mechanics.add(new CommandItems());
        if (config.leadsEnabled) mechanics.add(new BetterLeads());

        Iterator<CraftBookMechanic> iter = mechanics.iterator();
        while(iter.hasNext()) {
            CraftBookMechanic mech = iter.next();
            if(!mech.enable()) {
                iter.remove();
                break;
            }
            plugin.getServer().getPluginManager().registerEvents(mech, plugin);
        }
    }

    /**
     * Register a Dispenser Recipe
     *
     * @param recipe
     *
     * @return if successfully added.
     * @deprecated Use DispenserRecipes.inst().addRecipe(recipe);
     */
    @Deprecated
    public boolean registerDispenserRecipe(Recipe recipe) {

        return plugin.getConfiguration().customDispensingEnabled && DispenserRecipes.inst().addRecipe(recipe);
    }
}
