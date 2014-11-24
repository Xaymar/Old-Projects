/* Authors: Xaymar
 * Copyright: 2013 (c) RealityBends
 * License: This file is part of Project Kube.
 * 
 *      This Source Code Form is subject to the terms of the Mozilla Public
 *      License, v. 2.0. If a copy of the MPL was not distributed with this
 *      file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.RealityBends.ExtraGenerators;

import de.RealityBends.ExtraGenerators.World.Handler;
import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_6_R2.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

public class Plugin extends JavaPlugin implements Listener {

    private File oDataPath;
    private HashMap<World, Handler> oWorldHandlerMap;

    // <editor-fold defaultstate="collapsed" desc="Method: onEnable (Plugin Constructor)">
    @Override
    public void onEnable() {
        // Create directory references and create nonexisting folders.
        oDataPath = this.getDataFolder();
        if (!oDataPath.exists()) {
            getLogger().fine("Plugin directory is missing, creating...");
            if (!oDataPath.mkdirs()) {
                getLogger().severe("Failed to create plugin directory.");
            }
        }
        
        // Bukkit: Change the accessibility of the CraftWorld.generator and CraftWorld.populators
        try {
            Field rfWorldGenerator = CraftWorld.class.getDeclaredField("generator");
            rfWorldGenerator.setAccessible(true);
        } catch (NoSuchFieldException | SecurityException ex) {
            getLogger().warning("[Core] Unable to set generator field to accessible. Are you running CraftBukkit?");
            getLogger().log(Level.FINEST, ex.getMessage(), ex);
        }
        try {
            Field rfWorldPopulators = CraftWorld.class.getDeclaredField("populators");
            rfWorldPopulators.setAccessible(true);
        } catch (NoSuchFieldException | SecurityException ex) {
            getLogger().warning("[Core] Unable to set populators field to accessible. Are you running CraftBukkit?");
            getLogger().log(Level.FINEST, ex.getMessage(), ex);
        }
        
        // Create the WorldHandler HashMap for later use.
        oWorldHandlerMap = new HashMap<>();
        for (World oWorld : getServer().getWorlds()) {
            Handler oHandler = new Handler(this, oWorld);
            oWorldHandlerMap.put(oWorld, oHandler);
            oHandler.onEnable();
        }
        
        // Register our global event handler
        Bukkit.getPluginManager().registerEvents(this, this);

        getLogger().info("[Core] Done.");
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Method: onDisable (Plugin Destructor)">
    @Override
    public void onDisable() {
        // Delete and unload world handlers and destroy the list of world handlers.
        for (World oWorld : getServer().getWorlds()) {
            oWorldHandlerMap.remove(oWorld).onDisable();
        }
        oWorldHandlerMap = null;
        
        // Bukkit: Change the accessibility of the CraftWorld.generator and CraftWorld.populators back to normal.
        try {
            Field rfWorldGenerator = CraftWorld.class.getDeclaredField("generator");
            rfWorldGenerator.setAccessible(false);
        } catch (NoSuchFieldException | SecurityException ex) {
            getLogger().warning("[Core] Unable to set generator field to inaccessible. Are you running CraftBukkit?");
            getLogger().log(Level.FINEST, ex.getMessage(), ex);
        }
        try {
            Field rfWorldPopulators = CraftWorld.class.getDeclaredField("populators");
            rfWorldPopulators.setAccessible(false);
        } catch (NoSuchFieldException | SecurityException ex) {
            getLogger().warning("[Core] Unable to set populators field to inaccessible. Are you running CraftBukkit?");
            getLogger().log(Level.FINEST, ex.getMessage(), ex);
        }
        
        getLogger().info("[Core] Done.");
    }
    // </editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Event Handlers">
    // Install a new WorldHandler when a world is loaded.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldLoad(WorldLoadEvent oEvent) {
        World oWorld = oEvent.getWorld();
        Handler oWorldHandler = oWorldHandlerMap.get(oWorld);
        
        // If we don't have a Handler for this world, create and insert one.
        if (oWorldHandler == null) {
            oWorldHandler = new Handler(this, oWorld);
            oWorldHandlerMap.put(oWorld, oWorldHandler);
        }
        
        // Enable the World Handler.
        oWorldHandler.onEnable();
    }

    // Uninstall the installed WorldHandler when a world is unloaded.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent oEvent) {
        World oWorld = oEvent.getWorld();
        Handler oHandler = oWorldHandlerMap.get(oWorld);
        
        // If we have a Handler for this world, disable it and remove the entry.
        if (oHandler != null) {
            oHandler.onDisable();
            oWorldHandlerMap.remove(oWorld);
        }
    }
    
    
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Assign Generators">
    @Override
    public ChunkGenerator getDefaultWorldGenerator(String stWolrdName, String stId) {
        World oWorld = Bukkit.getWorld(stWolrdName);
        if (oWorld != null) {
            Handler oHandler = oWorldHandlerMap.get(oWorld);
            if (oHandler != null) {
                return oHandler.getGenerator();
            }
        }
        
        return null;
    }
    //</editor-fold>
}
