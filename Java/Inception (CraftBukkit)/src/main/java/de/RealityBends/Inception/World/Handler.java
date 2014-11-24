/* Authors: Xaymar
 * Copyright: 2012-2013 (c) Inception Plugin Team.
 * License: CC BY-SA 3.0
 *      Inception by Inception Plugin Team is licensed under a
 *      Creative Commons Attribution-ShareAlike 3.0 Unported
 *      License.
 */
package de.RealityBends.Inception.World;

import de.RealityBends.Inception.Plugin;
import de.RealityBends.Inception.Utility;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.EnumSet;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.Vector;

public class Handler
        implements Listener, Runnable {

    private Plugin oPlugin;
    private World oWorld;
    private int iHandlerTaskId = -1;

    //<editor-fold defaultstate="collapsed" desc="Constructor">
    public Handler(Plugin poPlugin, World poWorld) {
        // Assign Plugin and World reference.
        oPlugin = poPlugin;
        oWorld = poWorld;

        // Create a File reference to the configuration and create a YamlConfiguration object.
        oConfigurationFile = new File(oPlugin.getWorldDirectory() + File.separator + oWorld.getName() + ".yml");
        oConfiguration = new YamlConfiguration();

        // Set the defaults and save the configuration if there is no file.
        oConfiguration.setDefaults(poPlugin.getDefaultConfiguration());
    }
    //</editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Enabler and Disabler">
    public void onEnable() {
        // Tell Console we are starting work.
        oPlugin.getLogger().fine("[" + oWorld.getName() + "] Enabling...");

        // Load the worlds configuration, so that we can use it.
        try {
            loadConfiguration();
        } catch (FileNotFoundException ex) {
            oPlugin.getLogger().log(Level.SEVERE, "[" + oWorld.getName() + "] Configuration file was not found:", ex);
            return;
        } catch (IOException ex) {
            oPlugin.getLogger().log(Level.SEVERE, "[" + oWorld.getName() + "] Something went wrong while reading the configuration file:", ex);
            return;
        } catch (InvalidConfigurationException ex) {
            oPlugin.getLogger().log(Level.SEVERE, "[" + oWorld.getName() + "] The given configuration is invalid:", ex);
            return;
        }

        // Only do the following if the world is even enabled.
        if (tWorldEnabled) {
            enableTeleporting();
            enableOverlapping();
        }

        // Tell Console we are done.
        oPlugin.getLogger().info("[" + oWorld.getName() + "] Enabled.");
    }

    public void onDisable() {
        // Tell Console we are starting work.
        oPlugin.getLogger().fine("[" + oWorld.getName() + "] Disabling...");

        // Only do the following if the world is even enabled.
        if (tWorldEnabled) {
            disableTeleporting();
            disableOverlapping();
        }

        // Tell Console we are done.
        oPlugin.getLogger().info("[" + oWorld.getName() + "] Disabled.");
    }
    // </editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Teleporting">
    private void enableTeleporting() {
        // Create a synchronous repeating task to check entities if teleporting is enabled.
        if ((tAboveTeleportEnabled || tBelowTeleportEnabled) && iHandlerTaskId == -1) {
            iHandlerTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(oPlugin, this, 0, oPlugin.getGeneralTaskWaitTime());
        }
    }

    private void disableTeleporting() {
        // Cancel the synchronous repeating task that we started earlier when teleporting is enabled.
        if ((tAboveTeleportEnabled || tBelowTeleportEnabled) && iHandlerTaskId != -1) {
            Bukkit.getScheduler().cancelTask(iHandlerTaskId);
            iHandlerTaskId = -1;
        }
    }

    @Override
    public void run() {
        boolean tGeneralPredictPosition = oPlugin.isGeneralPredictPositionEnabled();
        int iGeneralTaskWaitTime = oPlugin.getGeneralTaskWaitTime();
        PluginManager bukkitPluginManager = Bukkit.getPluginManager();

        // Synchronize Time across worlds.
        if (oWorldSynchronizeWith != null) {
            oWorld.setTime(oWorldSynchronizeWith.getTime());
        }

        // Iterate through entities if it matches the following:
        // Are any worlds set?
        if ((oAboveWorld != null) || (oBelowWorld != null)) {
            Location upperTeleportExit = new Location(oAboveWorld, 0, 0, 0);
            Location lowerTeleportExit = new Location(oBelowWorld, 0, 0, 0);

            for (Entity entity : oWorld.getEntities()) {
                // Disallow entities that are driving other entities to be teleported before their vehicle is.
                if (entity.getVehicle() != null) {
                    continue; //Skip this entity.
                }

                // Skip players that don't have the permission to teleport.
                if (entity.getType() == EntityType.PLAYER && !hasTeleportPermission((Player) entity)) {
                    continue;
                }

                // Cache the Entity location and velocity for later use.
                Location entityLocation = entity.getLocation();
                Vector entityVelocity = entity.getVelocity();

                // If enabled, advance the entites position by their velocity * handlerConfig.getWorldDelayedTicks().
                if (tGeneralPredictPosition == true) {
                    entityLocation.setX(entityLocation.getX() + entityVelocity.getX() * iGeneralTaskWaitTime);
                    entityLocation.setY(entityLocation.getY() + entityVelocity.getY() * iGeneralTaskWaitTime);
                    entityLocation.setZ(entityLocation.getZ() + entityVelocity.getZ() * iGeneralTaskWaitTime);
                }

                //Teleportation for worldAbove
                if ((oAboveWorld != null) && (tAboveTeleportEnabled == true)) {
                    if ((Math.round(entityLocation.getY()) >= sAboveTeleportFrom) && (!oAboveTeleportEntityFilterSet.contains(entity.getType()))) {
                        upperTeleportExit.setX(entityLocation.getX());
                        upperTeleportExit.setY(sAboveTeleportTo + (entityLocation.getY() - sAboveTeleportFrom));
                        upperTeleportExit.setZ(entityLocation.getZ());
                        upperTeleportExit.setPitch(entityLocation.getPitch());
                        upperTeleportExit.setYaw(entityLocation.getYaw());

                        //Look! CraftBukkit already has this!
                        EntityTeleportEvent entityTeleportEvent = new EntityTeleportEvent(entity, entityLocation, upperTeleportExit);
                        bukkitPluginManager.callEvent(entityTeleportEvent);

                        if (!entityTeleportEvent.isCancelled()) {
                            // Should we make the target position safe?
                            if (tAboveTeleportSafe) {
                                if (entityTeleportEvent.getEntityType() == EntityType.PLAYER) {
                                    Location oTargetLocationCopy = entityTeleportEvent.getTo().clone();
                                    if (oAboveWorld.getBlockAt(oTargetLocationCopy).isEmpty() && tAboveTeleportPlatform) {
                                        oAboveWorld.getBlockAt(oTargetLocationCopy).setType(Material.GLASS);
                                    }
                                    if (!oAboveWorld.getBlockAt(oTargetLocationCopy.add(0, 1, 0)).isEmpty() && oAboveWorld.getBlockAt(oTargetLocationCopy).getType().isSolid()) {
                                        oAboveWorld.getBlockAt(oTargetLocationCopy).setType(Material.AIR);
                                    }
                                    if (!oAboveWorld.getBlockAt(oTargetLocationCopy.add(0, 1, 0)).isEmpty() && oAboveWorld.getBlockAt(oTargetLocationCopy).getType().isSolid()) {
                                        oAboveWorld.getBlockAt(oTargetLocationCopy).setType(Material.AIR);
                                    }
                                    entityTeleportEvent.getTo().add(0, 1, 0);
                                }
                            }
                            Utility.EntityTeleportEx(entity, entityTeleportEvent.getTo(), oPlugin);
                            entity.setVelocity(entityVelocity);
                        }
                    }
                }
                //Teleportation for worldBelow
                if ((oBelowWorld != null) && (tBelowTeleportEnabled == true)) {
                    if ((Math.round(entityLocation.getY()) <= sBelowTeleportFrom) && (!oBelowTeleportEntityFilterSet.contains(entity.getType()))) {
                        lowerTeleportExit.setX(entityLocation.getX());
                        lowerTeleportExit.setY(sBelowTeleportTo - (sBelowTeleportFrom - entityLocation.getY()));
                        lowerTeleportExit.setZ(entityLocation.getZ());
                        lowerTeleportExit.setPitch(entityLocation.getPitch());
                        lowerTeleportExit.setYaw(entityLocation.getYaw());

                        //Look! CraftBukkit already has this!
                        EntityTeleportEvent entityTeleportEvent = new EntityTeleportEvent(entity, entityLocation, lowerTeleportExit);
                        bukkitPluginManager.callEvent(entityTeleportEvent);

                        if (!entityTeleportEvent.isCancelled()) {
                            if (entityTeleportEvent.getEntityType() == EntityType.PLAYER) {
                                // Should we make the target position safe?
                                if (tBelowTeleportSafe) {
                                    Location oTargetLocationCopy = entityTeleportEvent.getTo().clone();
                                    if (!oBelowWorld.getBlockAt(oTargetLocationCopy).isEmpty() && oBelowWorld.getBlockAt(oTargetLocationCopy).getType().isSolid()) {
                                        oBelowWorld.getBlockAt(oTargetLocationCopy).setType(Material.AIR);
                                    }
                                    if (!oBelowWorld.getBlockAt(oTargetLocationCopy.add(0, 1, 0)).isEmpty() && oBelowWorld.getBlockAt(oTargetLocationCopy).getType().isSolid()) {
                                        oBelowWorld.getBlockAt(oTargetLocationCopy).setType(Material.AIR);
                                    }
                                }
                                if (tBelowTeleportPreventFalldamage) {
                                    entityTeleportEvent.setTo(entityTeleportEvent.getTo());
                                    entity.setMetadata("takeFallDamage", oPlugin.getNoFalldamageMetadata());
                                }
                            }
                            Utility.EntityTeleportEx(entity, entityTeleportEvent.getTo(), oPlugin);
                            entity.setVelocity(entityVelocity);
                        }
                    }
                }
            }
        }
    }

    private boolean hasTeleportPermission(Player player) {
        if (player != null) {
            return player.hasPermission("inception.teleport");
        }
        return true;
    }
    //</editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Overlapping">
    private void enableOverlapping() {
        // Register an event handler if overlapping is enabled.
        if (tAboveOverlapEnabled || tBelowOverlapEnabled) {
            for (OverlapEvents oBlockEvent : OverlapEvents.values()) {
                if (oWorldOverlapEvents.contains(oBlockEvent)) {
                    Bukkit.getPluginManager().registerEvent(oBlockEvent.getEventClass(), this, EventPriority.MONITOR, new OverlapEventExecutor(oWorld, oBlockEvent, (short)(sAboveOverlapFrom - sAboveOverlapLayers), (short)(sBelowOverlapFrom + sBelowOverlapLayers)), oPlugin);
                }
            }
        }
    }

    private void disableOverlapping() {
        // Unregister the event handler that was registered when overlapping is enabled.
        if (tAboveOverlapEnabled || tBelowOverlapEnabled) {
            for (OverlapEvents oBlockEvent : OverlapEvents.values()) {
                if (oWorldOverlapEvents.contains(oBlockEvent)) {
                    try { // Call me insane, but fuck yeah! Managed code rocks! Step it up, C/C++!
                        ((HandlerList) oBlockEvent.getEventClass().getMethod("getHandlerList").invoke(null)).unregister(this);
                    } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                        oPlugin.getLogger().log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

    //<editor-fold defaultstate="collapsed" desc="Helpers">
    private Location overlapGetAboveLocation(Location oLocation) {
        if (oAboveWorld != null && oLocation.getBlockY() >= (sAboveOverlapFrom - sAboveOverlapLayers) && oLocation.getBlockY() <= sAboveOverlapFrom) {
            return new Location(oAboveWorld, oLocation.getBlockX(), sAboveOverlapTo + (oLocation.getBlockY() - (sAboveOverlapFrom - sAboveOverlapLayers)), oLocation.getBlockZ());
        }
        return null;
    }

    private Location overlapGetBelowLocation(Location oLocation) {
        if (oBelowWorld != null && oLocation.getBlockY() >= sBelowOverlapFrom && oLocation.getBlockY() <= (sBelowOverlapFrom + sBelowOverlapLayers)) {
            return new Location(oBelowWorld, oLocation.getBlockX(), sBelowOverlapTo + (oLocation.getBlockY() - (sBelowOverlapFrom + sBelowOverlapLayers)), oLocation.getBlockZ());
        }
        return null;
    }

    private void overlapPlaceBlock(Location oLocation, final Block oBlock) {
        if (oAboveWorld != null && tAboveOverlapEnabled) {
            final Location oTargetLocation = overlapGetAboveLocation(oLocation);
            if (oTargetLocation != null && !tAboveOverlapSourceFilterArray[oBlock.getTypeId()]) {
                if (oAboveWorld.isChunkLoaded(oTargetLocation.getBlockX() >> 4, oTargetLocation.getBlockZ() >> 4)) {
                    final Block oTargetBlock = oAboveWorld.getBlockAt(oTargetLocation);
                    if (!tAboveOverlapTargetFilterArray[oTargetBlock.getTypeId()]) {
                        if (oPlugin.getOverlappingDelayedActionArray()[oBlock.getTypeId()]) {
                            Bukkit.getScheduler().scheduleSyncDelayedTask(oPlugin, new Runnable() {
                                @Override
                                public void run() {
                                    oTargetBlock.setTypeIdAndData(oBlock.getTypeId(), oBlock.getData(), false);
                                }

                            }, oPlugin.getGeneralTaskWaitTime());
                        } else {
                            oTargetBlock.setTypeIdAndData(oBlock.getTypeId(), oBlock.getData(), false);
                        }
                    }
                } else {
                    if (oPlugin.getOverlappingDelayedActionArray()[oBlock.getTypeId()]) {
                        Bukkit.getScheduler().scheduleSyncDelayedTask(oPlugin, new Runnable() {
                            @Override
                            public void run() {
                                oPlugin.cacheEvent(oTargetLocation.getWorld(), (byte) oTargetLocation.getBlockX(), oTargetLocation.getBlockZ(), (short) oTargetLocation.getBlockY(), (short) oBlock.getTypeId(), oBlock.getData());
                            }

                        }, oPlugin.getGeneralTaskWaitTime());
                    } else {
                        oPlugin.cacheEvent(oTargetLocation.getWorld(), (byte) oTargetLocation.getBlockX(), oTargetLocation.getBlockZ(), (short) oTargetLocation.getBlockY(), (short) oBlock.getTypeId(), oBlock.getData());
                    }
                }
            }
        }

        if (oBelowWorld != null && tBelowOverlapEnabled) {
            final Location oTargetLocation = overlapGetBelowLocation(oLocation);
            if (oTargetLocation != null && !tBelowOverlapSourceFilterArray[oBlock.getTypeId()]) {
                if (oBelowWorld.isChunkLoaded(oTargetLocation.getBlockX() >> 4, oTargetLocation.getBlockZ() >> 4)) {
                    final Block oTargetBlock = oBelowWorld.getBlockAt(oTargetLocation);
                    if (!tBelowOverlapTargetFilterArray[oTargetBlock.getTypeId()]) {
                        if (oPlugin.getOverlappingDelayedActionArray()[oBlock.getTypeId()]) {
                            Bukkit.getScheduler().scheduleSyncDelayedTask(oPlugin, new Runnable() {
                                @Override
                                public void run() {
                                    oTargetBlock.setTypeIdAndData(oBlock.getTypeId(), oBlock.getData(), false);
                                }

                            }, oPlugin.getGeneralTaskWaitTime());
                        } else {
                            oTargetBlock.setTypeIdAndData(oBlock.getTypeId(), oBlock.getData(), false);
                        }
                    }
                } else {
                    if (oPlugin.getOverlappingDelayedActionArray()[oBlock.getTypeId()]) {
                        Bukkit.getScheduler().scheduleSyncDelayedTask(oPlugin, new Runnable() {
                            @Override
                            public void run() {
                                oPlugin.cacheEvent(oTargetLocation.getWorld(), (byte) oTargetLocation.getBlockX(), oTargetLocation.getBlockZ(), (short) oTargetLocation.getBlockY(), (short) oBlock.getTypeId(), oBlock.getData());
                            }

                        }, oPlugin.getGeneralTaskWaitTime());
                    } else {
                        oPlugin.cacheEvent(oTargetLocation.getWorld(), (byte) oTargetLocation.getBlockX(), oTargetLocation.getBlockZ(), (short) oTargetLocation.getBlockY(), (short) oBlock.getTypeId(), oBlock.getData());
                    }
                }
            }
        }
    }

    private void overlapRemoveBlock(Location oLocation) {
        if (oAboveWorld != null && tAboveOverlapEnabled) {
            Location oTargetLocation = overlapGetAboveLocation(oLocation);
            if (oTargetLocation != null && !tAboveOverlapSourceFilterArray[0]) {
                if (oAboveWorld.isChunkLoaded(oTargetLocation.getBlockX() >> 4, oTargetLocation.getBlockZ() >> 4)) {
                    Block oTargetBlock = oAboveWorld.getBlockAt(oTargetLocation);
                    if (!tAboveOverlapTargetFilterArray[oTargetBlock.getTypeId()]) {
                        oTargetBlock.setTypeIdAndData(0, (byte) 0, false);
                    }
                } else {
                    oPlugin.cacheEvent(oTargetLocation.getWorld(), (byte) oTargetLocation.getBlockX(), oTargetLocation.getBlockZ(), (short) oTargetLocation.getBlockY(), (short) 0, (byte) 0);
                }
            }
        }

        if (oBelowWorld != null && tBelowOverlapEnabled) {
            Location oTargetLocation = overlapGetBelowLocation(oLocation);
            if (oTargetLocation != null && !tBelowOverlapSourceFilterArray[0]) {
                if (oBelowWorld.isChunkLoaded(oTargetLocation.getBlockX() >> 4, oTargetLocation.getBlockZ() >> 4)) {
                    Block oTargetBlock = oBelowWorld.getBlockAt(oTargetLocation);
                    if (!tBelowOverlapTargetFilterArray[oTargetBlock.getTypeId()]) {
                        oTargetBlock.setTypeIdAndData(0, (byte) 0, false);
                    }
                } else {
                    oPlugin.cacheEvent(oTargetLocation.getWorld(), (byte) oTargetLocation.getBlockX(), oTargetLocation.getBlockZ(), (short) oTargetLocation.getBlockY(), (short) 0, (byte) 0);
                }
            }
        }
    }

    private boolean hasOverlapPermission(Player player) {
        if (player != null) {
            return player.hasPermission("inception.overlap");
        }
        return true;
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Overlap Event Triggers">

    public void onOverlapEvent(BlockPlaceEvent event) {
        if (hasOverlapPermission(event.getPlayer()) && event.canBuild()) {
            overlapPlaceBlock(event.getBlock().getLocation(), event.getBlock());
        }
    }

    public void onOverlapEvent(BlockBreakEvent event) {
        if (hasOverlapPermission(event.getPlayer())) {
            overlapRemoveBlock(event.getBlock().getLocation());
        }
    }

    public void onOverlapEvent(BlockBurnEvent event) {
        overlapRemoveBlock(event.getBlock().getLocation());
    }

    public void onOverlapEvent(BlockFadeEvent event) {
        overlapRemoveBlock(event.getBlock().getLocation());
    }

    public void onOverlapEvent(BlockFormEvent event) {
        overlapRemoveBlock(event.getBlock().getLocation());
    }

    public void onOverlapEvent(BlockGrowEvent event) {
        overlapPlaceBlock(event.getBlock().getLocation(), event.getBlock());
    }

    public void onOverlapEvent(BlockSpreadEvent event) {
        overlapPlaceBlock(event.getBlock().getLocation(), event.getBlock());
    }

    public void onOverlapEvent(BlockFromToEvent event) {
        overlapPlaceBlock(event.getBlock().getLocation(), event.getBlock());
        overlapPlaceBlock(event.getToBlock().getLocation(), event.getToBlock());
    }

    public void onOverlapEvent(BlockPhysicsEvent event) {
        overlapRemoveBlock(event.getBlock().getLocation());
    }

    public void onOverlapEvent(LeavesDecayEvent event) {
        overlapRemoveBlock(event.getBlock().getLocation());
    }

    public void onOverlapEvent(SignChangeEvent event) {
        if (oAboveWorld != null && tAboveOverlapEnabled) {
            Location oTargetLocation = overlapGetAboveLocation(event.getBlock().getLocation());
            if (oTargetLocation != null && !tAboveOverlapSourceFilterArray[event.getBlock().getTypeId()]) {
                if (oAboveWorld.isChunkLoaded(oTargetLocation.getBlockX() >> 4, oTargetLocation.getBlockZ() >> 4)) {
                    Sign oTargetSign = (Sign) oAboveWorld.getBlockAt(oTargetLocation).getState();
                    if (oTargetSign != null) {
                        for (int line = 0; line < 4; line++) {
                            oTargetSign.setLine(line, event.getLine(line));
                        }
                    }
                } else {
                    oPlugin.cacheEvent(oTargetLocation.getWorld(), (byte) oTargetLocation.getBlockX(), oTargetLocation.getBlockZ(), (short) oTargetLocation.getBlockY(), event.getLine(0), event.getLine(1), event.getLine(2), event.getLine(3));
                }
            }
        }

        if (oBelowWorld != null && tBelowOverlapEnabled) {
            Location oTargetLocation = overlapGetBelowLocation(event.getBlock().getLocation());
            if (oTargetLocation != null && !tBelowOverlapSourceFilterArray[event.getBlock().getTypeId()]) {
                if (oBelowWorld.isChunkLoaded(oTargetLocation.getBlockX() >> 4, oTargetLocation.getBlockZ() >> 4)) {
                    Sign oTargetSign = (Sign) oBelowWorld.getBlockAt(oTargetLocation).getState();
                    if (oTargetSign != null) {
                        for (int line = 0; line < 4; line++) {
                            oTargetSign.setLine(line, event.getLine(line));
                        }
                    }
                } else {
                    oPlugin.cacheEvent(oTargetLocation.getWorld(), (byte) oTargetLocation.getBlockX(), oTargetLocation.getBlockZ(), (short) oTargetLocation.getBlockY(), event.getLine(0), event.getLine(1), event.getLine(2), event.getLine(3));
                }
            }
        }
    }
    //</editor-fold>
    // </editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Configuration">
    private YamlConfiguration oConfiguration;
    private File oConfigurationFile;

    //<editor-fold defaultstate="collapsed" desc="Loading and Saving">
    private void loadConfiguration()
            throws IOException, InvalidConfigurationException {
        oPlugin.getLogger().fine("Loading configuration...");
        if (oConfigurationFile.exists()) {
            oConfiguration.load(oConfigurationFile);
        }

        cacheWorldEnabled();
        cacheWorldSynchronizeWith();
        cacheWorldOverlapEvents();
        cacheAboveWorld();
        cacheAboveTeleportEnabled();
        cacheAboveTeleportFrom();
        cacheAboveTeleportTo();
        cacheAboveTeleportSafe();
        cacheAboveTeleportPlatform();
        cacheAboveTeleportEntityFilter();
        cacheAboveOverlapEnabled();
        cacheAboveOverlapFrom();
        cacheAboveOverlapLayers();
        cacheAboveOverlapTo();
        cacheAboveOverlapSourceFilter();
        cacheAboveOverlapTargetFilter();
        cacheBelowWorld();
        cacheBelowTeleportEnabled();
        cacheBelowTeleportFrom();
        cacheBelowTeleportTo();
        cacheBelowTeleportSafe();
        cacheBelowTeleportPreventFalldamage();
        cacheBelowTeleportEntityFilter();
        cacheBelowOverlapEnabled();
        cacheBelowOverlapFrom();
        cacheBelowOverlapLayers();
        cacheBelowOverlapTo();
        cacheBelowOverlapSourceFilter();
        cacheBelowOverlapTargetFilter();

        if (!oConfigurationFile.exists()) {
            saveDefaultConfiguration();
        }
        oPlugin.getLogger().fine("Done.");
    }

    private void saveConfiguration()
            throws IOException {
        oPlugin.getLogger().fine("Saving...");
        oConfiguration.save(oConfigurationFile);
        oPlugin.getLogger().fine("Done.");
    }

    private void saveDefaultConfiguration()
            throws IOException {
        oPlugin.getLogger().fine("Creating default configuration...");
        oConfiguration.options().header("Authors: Xaymar\n"
                                        + "Copyright: 2012-2013 (c) Inception Plugin Team.\n"
                                        + "License: CC BY-SA 3.0\n"
                                        + "     Inception by Inception Plugin Team is licensed under a\n"
                                        + "     Creative Commons Attribution-ShareAlike 3.0 Unported\n"
                                        + "     License.\n"
                                        + "\n"
                                        + "Title                    Address\n"
                                        + "-------------------------------------------------------------------------------\n"
                                        + "Entity Names:            http://jd.bukkit.org/apidocs/org/bukkit/entity/EntityType.html\n"
                                        + "Material Names:          http://jd.bukkit.org/apidocs/org/bukkit/Material.html\n"
                                        + "Regular Expressions:     http://www.regular-expressions.info/refflavors.html\n"
                                        + "\n"
                                        + "Node                     Explanation\n"
                                        + "-------------------------------------------------------------------------------\n"
                                        + "World:                   \n"
                                        + "  Enabled:               Is this WorldHandler enabled?\n"
                                        + "  SynchronizeWith:       Should this world be time synchronized to another world?\n"
                                        + "  OverlapEvents:         Events on which Overlapping can react.\n"
                                        + "    BlockPlace:          React to block placement.\n"
                                        + "    BlockBreak:          React to block breaking.\n"
                                        + "    BlockBurn:           React to blocks burning away.\n"
                                        + "    BlockFade:           React to blocks fading away.\n"
                                        + "    BlockForm:           React to blocks forming.\n"
                                        + "    BlockGrow:           React to blocks growing.\n"
                                        + "    BlockSpread:         React to block spreading.\n"
                                        + "    BlockFromTo:         React to moving blocks (liquids).\n"
                                        + "    BlockPhysics:        React to physics interactions (falling, launching, ...).\n"
                                        + "    LeavesDecay:         React to leaves decaying.\n"
                                        + "    SignChange:          React to sign changes.\n"
                                        + "Above:                   \n"
                                        + "  World:                 What world is above this one?\n"
                                        + "  Teleport:              \n"
                                        + "    Enabled:             Is teleporting enabled?\n"
                                        + "    From:                From what layer (and above) should we teleport players?\n"
                                        + "    To:                  To what layer (and above) should we teleport players?\n"
                                        + "    Safe:                Make the teleport upwards safe for players to breathe?\n"
                                        + "    Platform:            Make the teleport upwards safe for players to stand?\n"
                                        + "    EntityFilter:        A Regular Expression that matches all entities to be disallowed from teleporting. Matches by Class Name.\n"
                                        + "  Overlap:               \n"
                                        + "    Enabled:             Is overlapping enabled?\n"
                                        + "    From:                From what layer in the above world should we start from?\n"
                                        + "    To:                  To what layer should these get copied?\n"
                                        + "    Layers:              How many layers should get copied?\n"
                                        + "    SourceFilter:        A Regular Expression that matches all unplacable materials while Overlapping. Matches by ID.\n"
                                        + "    TargetFilter:        A Regular Expression that matches all unreplacable materials while Overlapping. Matches by ID.\n"
                                        + "Below:                   \n"
                                        + "  World:                 What world is below this one?\n"
                                        + "  Teleport:              \n"
                                        + "    Enabled:             Is teleporting enabled?\n"
                                        + "    From:                From what layer (and below) should we teleport players?\n"
                                        + "    To:                  To what layer (and below) should we teleport players?\n"
                                        + "    Safe:                Make the teleport downwards safe for players?\n"
                                        + "    PreventFalldamage:   Should Inception prevent falldamage for players?\n"
                                        + "    EntityFilter:        A Regular Expression that matches all entities to be disallowed from teleporting. Matches by Class Name.\n"
                                        + "  Overlap:               \n"
                                        + "    Enabled:             Is overlapping enabled?\n"
                                        + "    From:                From what layer in the above world should we start from?\n"
                                        + "    To:                  To what layer should these get copied?\n"
                                        + "    Layers:              How many layers should get copied?\n"
                                        + "    SourceFilter:        A Regular Expression that matches all unplacable materials while Overlapping. Matches by ID.\n"
                                        + "    TargetFilter:        A Regular Expression that matches all unreplacable materials while Overlapping. Matches by ID.");

        oConfiguration.set("World.Enabled", tWorldEnabled);
        oConfiguration.set("World.SynchronizeWith", stWorldSynchronizeWith);
        for (OverlapEvents oBlockEvent : OverlapEvents.values()) {
            oConfiguration.set("World.OverlapEvents." + oBlockEvent.getName(), oWorldOverlapEvents.contains(oBlockEvent));
        }
        oConfiguration.set("Above.World", stAboveWorld);
        oConfiguration.set("Above.Teleport.Enabled", tAboveTeleportEnabled);
        oConfiguration.set("Above.Teleport.From", sAboveTeleportFrom);
        oConfiguration.set("Above.Teleport.To", sAboveTeleportTo);
        oConfiguration.set("Above.Teleport.Safe", tAboveTeleportSafe);
        oConfiguration.set("Above.Teleport.Platform", tAboveTeleportPlatform);
        oConfiguration.set("Above.Teleport.EntityFilter", stAboveTeleportEntityFilter);
        oConfiguration.set("Above.Overlap.Enabled", tAboveOverlapEnabled);
        oConfiguration.set("Above.Overlap.From", sAboveOverlapFrom);
        oConfiguration.set("Above.Overlap.To", sAboveOverlapTo);
        oConfiguration.set("Above.Overlap.Layers", sAboveOverlapLayers);
        oConfiguration.set("Above.Overlap.SourceFilter", stAboveOverlapSourceFilter);
        oConfiguration.set("Above.Overlap.TargetFilter", stAboveOverlapTargetFilter);
        oConfiguration.set("Below.World", stBelowWorld);
        oConfiguration.set("Below.Teleport.Enabled", tBelowTeleportEnabled);
        oConfiguration.set("Below.Teleport.From", sBelowTeleportFrom);
        oConfiguration.set("Below.Teleport.To", sBelowTeleportTo);
        oConfiguration.set("Below.Teleport.Safe", tBelowTeleportSafe);
        oConfiguration.set("Below.Teleport.PreventFalldamage", tBelowTeleportPreventFalldamage);
        oConfiguration.set("Below.Teleport.EntityFilter", stBelowTeleportEntityFilter);
        oConfiguration.set("Below.Overlap.Enabled", tBelowOverlapEnabled);
        oConfiguration.set("Below.Overlap.From", sBelowOverlapFrom);
        oConfiguration.set("Below.Overlap.To", sBelowOverlapTo);
        oConfiguration.set("Below.Overlap.Layers", sBelowOverlapLayers);
        oConfiguration.set("Below.Overlap.SourceFilter", stBelowOverlapSourceFilter);
        oConfiguration.set("Below.Overlap.TargetFilter", stBelowOverlapTargetFilter);

        oConfiguration.save(oConfigurationFile);
        oPlugin.getLogger().fine("Done.");
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="World.Enabled">
    private boolean tWorldEnabled = false;

    private void cacheWorldEnabled() {
        this.tWorldEnabled = oConfiguration.getBoolean("World.Enabled");
    }

    public boolean getWorldEnabled() {
        return tWorldEnabled;
    }

    public <T> void setWorldEnabled(T newState) {
        if (newState instanceof Boolean) {
            oConfiguration.set("World.Enabled", (Boolean) newState);
        } else {
            oConfiguration.set("World.Enabled", null);
        }
        cacheWorldEnabled();
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="World.SynchronizeWith">

    private String stWorldSynchronizeWith = "";
    private World oWorldSynchronizeWith;

    private void cacheWorldSynchronizeWith() {
        this.stWorldSynchronizeWith = oConfiguration.getString("World.SynchronizeWith");
        if (this.stWorldSynchronizeWith != null) {
            this.oWorldSynchronizeWith = Bukkit.getWorld(this.stWorldSynchronizeWith);
        } else {
            this.oWorldSynchronizeWith = null;
        }
    }

    public String getWorldSynchronizeWith() {
        return stWorldSynchronizeWith;
    }

    public <T> void setWorldSynchronizeWith(T newWorld) {
        if (newWorld instanceof String) {
            oConfiguration.set("World.SynchronizeWith", (String) newWorld);
        } else if (newWorld instanceof World) {
            oConfiguration.set("World.SynchronizeWith", ((World) newWorld).getName());
        } else {
            oConfiguration.set("World.SynchronizeWith", null);
        }
        cacheWorldSynchronizeWith();
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="World.OverlapEvents">

    private EnumSet<OverlapEvents> oWorldOverlapEvents = EnumSet.noneOf(OverlapEvents.class);

    private void cacheWorldOverlapEvents() {
        for (OverlapEvents oOverlapEvent : OverlapEvents.values()) {
            cacheWorldOverlapEventEnabled(oOverlapEvent);
        }
    }

    private void cacheWorldOverlapEventEnabled(OverlapEvents oOverlapEvent) {
        if (oConfiguration.getBoolean("World.OverlapEvents." + oOverlapEvent.getName())) {
            oWorldOverlapEvents.add(oOverlapEvent);
        } else {
            oWorldOverlapEvents.remove(oOverlapEvent);
        }
    }

    public boolean getWorldOverlapEventEnabled(OverlapEvents oOverlapEvent) {
        return oWorldOverlapEvents.contains(oOverlapEvent);
    }

    public <T> void setWorldOverlapEventEnabled(OverlapEvents oOverlapEvent, T newState) {
        if (newState instanceof Boolean) {
            oConfiguration.set("World.OverlapEvents." + oOverlapEvent.getName(), (Boolean) newState);
        } else {
            oConfiguration.set("World.OverlapEvents." + oOverlapEvent.getName(), null);
        }
        cacheWorldOverlapEventEnabled(oOverlapEvent);
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Above.World">

    private String stAboveWorld = "null";
    private World oAboveWorld;

    private void cacheAboveWorld() {
        this.stAboveWorld = oConfiguration.getString("Above.World");
        if (this.stAboveWorld != null) {
            this.oAboveWorld = Bukkit.getWorld(this.stAboveWorld);
        } else {
            this.oAboveWorld = null;
        }
    }

    public String getAboveWorld() {
        return stAboveWorld;
    }

    public <T> void setAboveWorld(T newWorld) {
        if (newWorld instanceof String) {
            oConfiguration.set("Above.World", (String) newWorld);
        } else if (newWorld instanceof World) {
            oConfiguration.set("Above.World", ((World) newWorld).getName());
        } else {
            oConfiguration.set("Above.World", null);
        }
        cacheAboveWorld();
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Above.Teleport.Enabled">

    private boolean tAboveTeleportEnabled = false;

    private void cacheAboveTeleportEnabled() {
        this.tAboveTeleportEnabled = oConfiguration.getBoolean("Above.Teleport.Enabled");
    }

    public boolean getAboveTeleportEnabled() {
        return tAboveTeleportEnabled;
    }

    public <T> void setAboveTeleportEnabled(T newState) {
        if (newState instanceof Boolean) {
            oConfiguration.set("Above.Teleport.Enabled", (Boolean) newState);
        } else {
            oConfiguration.set("Above.Teleport.Enabled", null);
        }
        cacheAboveTeleportEnabled();
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Above.Teleport.From">

    private short sAboveTeleportFrom = (short) 247;

    private void cacheAboveTeleportFrom() {
        this.sAboveTeleportFrom = (short) oConfiguration.getInt("Above.Teleport.From");
    }

    public short getAboveTeleportFrom() {
        return sAboveTeleportFrom;
    }

    public <T> void setAboveTeleportFrom(T newValue) {
        if (newValue instanceof Long) {
            oConfiguration.set("Above.Teleport.From", (Long) newValue);
        } else if (newValue instanceof Integer) {
            oConfiguration.set("Above.Teleport.From", (Integer) newValue);
        } else if (newValue instanceof Short) {
            oConfiguration.set("Above.Teleport.From", (Short) newValue);
        } else if (newValue instanceof Byte) {
            oConfiguration.set("Above.Teleport.From", (Byte) newValue);
        } else {
            oConfiguration.set("Above.Teleport.From", null);
        }
        cacheAboveTeleportFrom();
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Above.Teleport.To">

    private short sAboveTeleportTo = (short) 24;

    private void cacheAboveTeleportTo() {
        this.sAboveTeleportTo = (short) oConfiguration.getInt("Above.Teleport.To");
    }

    public short getAboveTeleportTo() {
        return sAboveTeleportTo;
    }

    public <T> void setAboveTeleportTo(T newValue) {
        if (newValue instanceof Long) {
            oConfiguration.set("Above.Teleport.To", (Long) newValue);
        } else if (newValue instanceof Integer) {
            oConfiguration.set("Above.Teleport.To", (Integer) newValue);
        } else if (newValue instanceof Short) {
            oConfiguration.set("Above.Teleport.To", (Short) newValue);
        } else if (newValue instanceof Byte) {
            oConfiguration.set("Above.Teleport.To", (Byte) newValue);
        } else {
            oConfiguration.set("Above.Teleport.To", null);
        }
        cacheAboveTeleportTo();
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Above.Teleport.Safe">

    private boolean tAboveTeleportSafe = false;

    private void cacheAboveTeleportSafe() {
        this.tAboveTeleportSafe = oConfiguration.getBoolean("Above.Teleport.Safe");
    }

    public boolean getAboveTeleportSafe() {
        return tAboveTeleportSafe;
    }

    public <T> void setAboveTeleportSafe(T newState) {
        if (newState instanceof Boolean) {
            oConfiguration.set("Above.Teleport.Safe", (Boolean) newState);
        } else {
            oConfiguration.set("Above.Teleport.Safe", null);
        }
        cacheAboveTeleportSafe();
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Above.Teleport.Platform">

    private boolean tAboveTeleportPlatform = false;

    private void cacheAboveTeleportPlatform() {
        this.tAboveTeleportPlatform = oConfiguration.getBoolean("Above.Teleport.Platform");
    }

    public boolean getAboveTeleportPlatform() {
        return tAboveTeleportPlatform;
    }

    public <T> void setAboveTeleportPlatform(T newState) {
        if (newState instanceof Boolean) {
            oConfiguration.set("Above.Teleport.Platform", (Boolean) newState);
        } else {
            oConfiguration.set("Above.Teleport.Platform", null);
        }
        cacheAboveTeleportPlatform();
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Above.Teleport.EntityFilter">

    private String stAboveTeleportEntityFilter = "(Painting|EnderDragon|Lightning|Weather|ComplexEntityPart)";
    private EnumSet<EntityType> oAboveTeleportEntityFilterSet = EnumSet.noneOf(EntityType.class);

    private void cacheAboveTeleportEntityFilter() {
        this.stAboveTeleportEntityFilter = oConfiguration.getString("Above.Teleport.EntityFilter");

        if (this.stAboveTeleportEntityFilter != null && !this.stAboveTeleportEntityFilter.isEmpty()) {
            for (EntityType type : EntityType.values()) {
                if (type.getEntityClass() != null && type.getEntityClass().getSimpleName().matches(this.stAboveTeleportEntityFilter) /* Match by Class Name */) {
                    this.oAboveTeleportEntityFilterSet.add(type);
                } else {
                    this.oAboveTeleportEntityFilterSet.remove(type);
                }
            }
        } else {
            this.oAboveTeleportEntityFilterSet.clear();
        }
    }

    public String getAboveTeleportEntityFilter() {
        return stAboveTeleportEntityFilter;
    }

    public <T> void setAboveTeleportEntityFilter(T newString) {
        if (newString instanceof String) {
            oConfiguration.set("Above.Teleport.EntityFilter", (String) newString);
        } else {
            oConfiguration.set("Above.Teleport.EntityFilter", null);
        }
        cacheAboveTeleportEntityFilter();
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Above.Overlap.Enabled">

    private boolean tAboveOverlapEnabled = false;

    private void cacheAboveOverlapEnabled() {
        this.tAboveOverlapEnabled = oConfiguration.getBoolean("Above.Overlap.Enabled");
    }

    public boolean getAboveOverlapEnabled() {
        return tAboveOverlapEnabled;
    }

    public <T> void setAboveOverlapEnabled(T newState) {
        if (newState instanceof Boolean) {
            oConfiguration.set("Above.Overlap.Enabled", (Boolean) newState);
        } else {
            oConfiguration.set("Above.Overlap.Enabled", null);
        }
        cacheAboveOverlapEnabled();
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Above.Overlap.From">

    private short sAboveOverlapFrom = (short) 255;

    private void cacheAboveOverlapFrom() {
        this.sAboveOverlapFrom = (short) oConfiguration.getInt("Above.Overlap.From");
    }

    public short getAboveOverlapFrom() {
        return sAboveOverlapFrom;
    }

    public <T> void setAboveOverlapFrom(T newValue) {
        if (newValue instanceof Long) {
            oConfiguration.set("Above.Overlap.From", (Long) newValue);
        } else if (newValue instanceof Integer) {
            oConfiguration.set("Above.Overlap.From", (Integer) newValue);
        } else if (newValue instanceof Short) {
            oConfiguration.set("Above.Overlap.From", (Short) newValue);
        } else if (newValue instanceof Byte) {
            oConfiguration.set("Above.Overlap.From", (Byte) newValue);
        } else {
            oConfiguration.set("Above.Overlap.From", null);
        }
        cacheAboveOverlapFrom();
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Above.Overlap.Layers">

    private short sAboveOverlapLayers = (short) 32;

    private void cacheAboveOverlapLayers() {
        this.sAboveOverlapLayers = (short) oConfiguration.getInt("Above.Overlap.Layers");
    }

    public short getAboveOverlapLayers() {
        return sAboveOverlapLayers;
    }

    public <T> void setAboveOverlapLayers(T newValue) {
        if (newValue instanceof Long) {
            oConfiguration.set("Above.Overlap.Layers", (Long) newValue);
        } else if (newValue instanceof Integer) {
            oConfiguration.set("Above.Overlap.Layers", (Integer) newValue);
        } else if (newValue instanceof Short) {
            oConfiguration.set("Above.Overlap.Layers", (Short) newValue);
        } else if (newValue instanceof Byte) {
            oConfiguration.set("Above.Overlap.Layers", (Byte) newValue);
        } else {
            oConfiguration.set("Above.Overlap.Layers", null);
        }
        cacheAboveOverlapLayers();
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Above.Overlap.To">

    private short sAboveOverlapTo = (short) 0;

    private void cacheAboveOverlapTo() {
        this.sAboveOverlapTo = (short) oConfiguration.getInt("Above.Overlap.To");
    }

    public short getAboveOverlapTo() {
        return sAboveOverlapTo;
    }

    public <T> void setAboveOverlapTo(T newValue) {
        if (newValue instanceof Long) {
            oConfiguration.set("Above.Overlap.To", (Long) newValue);
        } else if (newValue instanceof Integer) {
            oConfiguration.set("Above.Overlap.To", (Integer) newValue);
        } else if (newValue instanceof Short) {
            oConfiguration.set("Above.Overlap.To", (Short) newValue);
        } else if (newValue instanceof Byte) {
            oConfiguration.set("Above.Overlap.To", (Byte) newValue);
        } else {
            oConfiguration.set("Above.Overlap.To", null);
        }
        cacheAboveOverlapTo();
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Above.Overlap.SourceFilter">

    private String stAboveOverlapSourceFilter = "";
    private boolean[] tAboveOverlapSourceFilterArray = new boolean[256];

    private void cacheAboveOverlapSourceFilter() {
        this.stAboveOverlapSourceFilter = oConfiguration.getString("Above.Overlap.SourceFilter");
        if (this.stAboveOverlapSourceFilter != null && !this.stAboveOverlapSourceFilter.isEmpty()) {
            for (Material material : Material.values()) {
                /* Match by ID */
                tAboveOverlapSourceFilterArray[material.getId()] = String.valueOf(material.getId()).matches(this.stAboveOverlapSourceFilter);
            }
        } else {
            for (int n = 0; n < 256; n++) {
                tAboveOverlapSourceFilterArray[n] = false;
            }
        }
    }

    public String getAboveOverlapSourceFilter() {
        return stAboveOverlapSourceFilter;
    }

    public <T> void setAboveOverlapSourceFilter(T newValue) {
        if (newValue instanceof String) {
            oConfiguration.set("Above.Overlap.SourceFilter", (String) newValue);
        } else {
            oConfiguration.set("Above.Overlap.SourceFilter", null);
        }
        cacheAboveOverlapSourceFilter();
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Above.Overlap.TargetFilter">

    private String stAboveOverlapTargetFilter = "";
    private boolean[] tAboveOverlapTargetFilterArray = new boolean[256];

    private void cacheAboveOverlapTargetFilter() {
        this.stAboveOverlapTargetFilter = oConfiguration.getString("Above.Overlap.TargetFilter");
        if (this.stAboveOverlapTargetFilter != null && !this.stAboveOverlapTargetFilter.isEmpty()) {
            for (Material material : Material.values()) {
                /* Match by ID */
                tAboveOverlapTargetFilterArray[material.getId()] = String.valueOf(material.getId()).matches(this.stAboveOverlapTargetFilter);
            }
        } else {
            for (int n = 0; n < 256; n++) {
                tAboveOverlapTargetFilterArray[n] = false;
            }
        }
    }

    public String getAboveOverlapTargetFilter() {
        return stAboveOverlapTargetFilter;
    }

    public <T> void setAboveOverlapTargetFilter(T newValue) {
        if (newValue instanceof String) {
            oConfiguration.set("Above.Overlap.TargetFilter", (String) newValue);
        } else {
            oConfiguration.set("Above.Overlap.TargetFilter", null);
        }
        cacheAboveOverlapTargetFilter();
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Below.World">

    private String stBelowWorld = "null";
    private World oBelowWorld;

    private void cacheBelowWorld() {
        this.stBelowWorld = oConfiguration.getString("Below.World");
        if (this.stBelowWorld != null) {
            this.oBelowWorld = Bukkit.getWorld(this.stBelowWorld);
        } else {
            this.oBelowWorld = null;
        }
    }

    public String getBelowWorld() {
        return stBelowWorld;
    }

    public <T> void setBelowWorld(T newWorld) {
        if (newWorld instanceof String) {
            oConfiguration.set("Below.World", (String) newWorld);
        } else if (newWorld instanceof World) {
            oConfiguration.set("Below.World", ((World) newWorld).getName());
        } else {
            oConfiguration.set("Below.World", null);
        }
        cacheBelowWorld();
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Below.Teleport.Enabled">

    private boolean tBelowTeleportEnabled = false;

    private void cacheBelowTeleportEnabled() {
        this.tBelowTeleportEnabled = oConfiguration.getBoolean("Below.Teleport.Enabled");
    }

    public boolean getBelowTeleportEnabled() {
        return tBelowTeleportEnabled;
    }

    public <T> void setBelowTeleportEnabled(T newState) {
        if (newState instanceof Boolean) {
            oConfiguration.set("Below.Teleport.Enabled", (Boolean) newState);
        } else {
            oConfiguration.set("Below.Teleport.Enabled", null);
        }
        cacheBelowTeleportEnabled();
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Below.Teleport.From">

    private short sBelowTeleportFrom = (short) 247;

    private void cacheBelowTeleportFrom() {
        this.sBelowTeleportFrom = (short) oConfiguration.getInt("Below.Teleport.From");
    }

    public short getBelowTeleportFrom() {
        return sBelowTeleportFrom;
    }

    public <T> void setBelowTeleportFrom(T newValue) {
        if (newValue instanceof Long) {
            oConfiguration.set("Below.Teleport.From", (Long) newValue);
        } else if (newValue instanceof Integer) {
            oConfiguration.set("Below.Teleport.From", (Integer) newValue);
        } else if (newValue instanceof Short) {
            oConfiguration.set("Below.Teleport.From", (Short) newValue);
        } else if (newValue instanceof Byte) {
            oConfiguration.set("Below.Teleport.From", (Byte) newValue);
        } else {
            oConfiguration.set("Below.Teleport.From", null);
        }
        cacheBelowTeleportFrom();
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Below.Teleport.To">

    private short sBelowTeleportTo = (short) 24;

    private void cacheBelowTeleportTo() {
        this.sBelowTeleportTo = (short) oConfiguration.getInt("Below.Teleport.To");
    }

    public short getBelowTeleportTo() {
        return sBelowTeleportTo;
    }

    public <T> void setBelowTeleportTo(T newValue) {
        if (newValue instanceof Long) {
            oConfiguration.set("Below.Teleport.To", (Long) newValue);
        } else if (newValue instanceof Integer) {
            oConfiguration.set("Below.Teleport.To", (Integer) newValue);
        } else if (newValue instanceof Short) {
            oConfiguration.set("Below.Teleport.To", (Short) newValue);
        } else if (newValue instanceof Byte) {
            oConfiguration.set("Below.Teleport.To", (Byte) newValue);
        } else {
            oConfiguration.set("Below.Teleport.To", null);
        }
        cacheBelowTeleportTo();
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Below.Teleport.Safe">

    private boolean tBelowTeleportSafe = false;

    private void cacheBelowTeleportSafe() {
        this.tBelowTeleportSafe = oConfiguration.getBoolean("Below.Teleport.Safe");
    }

    public boolean getBelowTeleportSafe() {
        return tBelowTeleportSafe;
    }

    public <T> void setBelowTeleportSafe(T newState) {
        if (newState instanceof Boolean) {
            oConfiguration.set("Below.Teleport.Safe", (Boolean) newState);
        } else {
            oConfiguration.set("Below.Teleport.Safe", null);
        }
        cacheBelowTeleportSafe();
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Below.Teleport.PreventFalldamage">

    private boolean tBelowTeleportPreventFalldamage = false;

    private void cacheBelowTeleportPreventFalldamage() {
        this.tBelowTeleportPreventFalldamage = oConfiguration.getBoolean("Below.Teleport.PreventFalldamage");
    }

    public boolean getBelowTeleportPreventFalldamage() {
        return tBelowTeleportPreventFalldamage;
    }

    public <T> void setBelowTeleportPreventFalldamage(T newState) {
        if (newState instanceof Boolean) {
            oConfiguration.set("Below.Teleport.PreventFalldamage", (Boolean) newState);
        } else {
            oConfiguration.set("Below.Teleport.PreventFalldamage", null);
        }
        cacheBelowTeleportPreventFalldamage();
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Below.Teleport.EntityFilter">

    private String stBelowTeleportEntityFilter = "(Painting|EnderDragon|Lightning|Weather|ComplexEntityPart)";
    private EnumSet<EntityType> oBelowTeleportEntityFilterSet = EnumSet.noneOf(EntityType.class);

    private void cacheBelowTeleportEntityFilter() {
        this.stBelowTeleportEntityFilter = oConfiguration.getString("Below.Teleport.EntityFilter");

        if (this.stBelowTeleportEntityFilter != null && !this.stBelowTeleportEntityFilter.isEmpty()) {
            for (EntityType type : EntityType.values()) {
                if (type.getEntityClass() != null && type.getEntityClass().getSimpleName().matches(this.stBelowTeleportEntityFilter) /* Match by Class Name */) {
                    this.oBelowTeleportEntityFilterSet.add(type);
                } else {
                    this.oBelowTeleportEntityFilterSet.remove(type);
                }
            }
        } else {
            this.oBelowTeleportEntityFilterSet.clear();
        }
    }

    public String getBelowTeleportEntityFilter() {
        return stBelowTeleportEntityFilter;
    }

    public <T> void setBelowTeleportEntityFilter(T newString) {
        if (newString instanceof String) {
            oConfiguration.set("Below.Teleport.EntityFilter", (String) newString);
        } else {
            oConfiguration.set("Below.Teleport.EntityFilter", null);
        }
        cacheBelowTeleportEntityFilter();
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Below.Overlap.Enabled">

    private boolean tBelowOverlapEnabled = false;

    private void cacheBelowOverlapEnabled() {
        this.tBelowOverlapEnabled = oConfiguration.getBoolean("Below.Overlap.Enabled");
    }

    public boolean getBelowOverlapEnabled() {
        return tBelowOverlapEnabled;
    }

    public <T> void setBelowOverlapEnabled(T newState) {
        if (newState instanceof Boolean) {
            oConfiguration.set("Below.Overlap.Enabled", (Boolean) newState);
        } else {
            oConfiguration.set("Below.Overlap.Enabled", null);
        }
        cacheBelowOverlapEnabled();
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Below.Overlap.From">

    private short sBelowOverlapFrom = (short) 255;

    private void cacheBelowOverlapFrom() {
        this.sBelowOverlapFrom = (short) oConfiguration.getInt("Below.Overlap.From");
    }

    public short getBelowOverlapFrom() {
        return sBelowOverlapFrom;
    }

    public <T> void setBelowOverlapFrom(T newValue) {
        if (newValue instanceof Long) {
            oConfiguration.set("Below.Overlap.From", (Long) newValue);
        } else if (newValue instanceof Integer) {
            oConfiguration.set("Below.Overlap.From", (Integer) newValue);
        } else if (newValue instanceof Short) {
            oConfiguration.set("Below.Overlap.From", (Short) newValue);
        } else if (newValue instanceof Byte) {
            oConfiguration.set("Below.Overlap.From", (Byte) newValue);
        } else {
            oConfiguration.set("Below.Overlap.From", null);
        }
        cacheBelowOverlapFrom();
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Below.Overlap.Layers">

    private short sBelowOverlapLayers = (short) 32;

    private void cacheBelowOverlapLayers() {
        this.sBelowOverlapLayers = (short) oConfiguration.getInt("Below.Overlap.Layers");
    }

    public short getBelowOverlapLayers() {
        return sBelowOverlapLayers;
    }

    public <T> void setBelowOverlapLayers(T newValue) {
        if (newValue instanceof Long) {
            oConfiguration.set("Below.Overlap.Layers", (Long) newValue);
        } else if (newValue instanceof Integer) {
            oConfiguration.set("Below.Overlap.Layers", (Integer) newValue);
        } else if (newValue instanceof Short) {
            oConfiguration.set("Below.Overlap.Layers", (Short) newValue);
        } else if (newValue instanceof Byte) {
            oConfiguration.set("Below.Overlap.Layers", (Byte) newValue);
        } else {
            oConfiguration.set("Below.Overlap.Layers", null);
        }
        cacheBelowOverlapLayers();
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Below.Overlap.To">

    private short sBelowOverlapTo = (short) 0;

    private void cacheBelowOverlapTo() {
        this.sBelowOverlapTo = (short) oConfiguration.getInt("Below.Overlap.To");
    }

    public short getBelowOverlapTo() {
        return sBelowOverlapTo;
    }

    public <T> void setBelowOverlapTo(T newValue) {
        if (newValue instanceof Long) {
            oConfiguration.set("Below.Overlap.To", (Long) newValue);
        } else if (newValue instanceof Integer) {
            oConfiguration.set("Below.Overlap.To", (Integer) newValue);
        } else if (newValue instanceof Short) {
            oConfiguration.set("Below.Overlap.To", (Short) newValue);
        } else if (newValue instanceof Byte) {
            oConfiguration.set("Below.Overlap.To", (Byte) newValue);
        } else {
            oConfiguration.set("Below.Overlap.To", null);
        }
        cacheBelowOverlapTo();
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Below.Overlap.SourceFilter">

    private String stBelowOverlapSourceFilter = "";
    private boolean[] tBelowOverlapSourceFilterArray = new boolean[256];

    private void cacheBelowOverlapSourceFilter() {
        this.stBelowOverlapSourceFilter = oConfiguration.getString("Below.Overlap.SourceFilter");
        if (this.stBelowOverlapSourceFilter != null && !this.stBelowOverlapSourceFilter.isEmpty()) {
            for (Material material : Material.values()) {
                /* Match by ID */
                tBelowOverlapSourceFilterArray[material.getId()] = String.valueOf(material.getId()).matches(this.stBelowOverlapSourceFilter);
            }
        } else {
            for (int n = 0; n < 256; n++) {
                tBelowOverlapSourceFilterArray[n] = false;
            }
        }
    }

    public String getBelowOverlapSourceFilter() {
        return stBelowOverlapSourceFilter;
    }

    public <T> void setBelowOverlapSourceFilter(T newValue) {
        if (newValue instanceof String) {
            oConfiguration.set("Below.Overlap.SourceFilter", (String) newValue);
        } else {
            oConfiguration.set("Below.Overlap.SourceFilter", null);
        }
        cacheBelowOverlapSourceFilter();
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Below.Overlap.TargetFilter">

    private String stBelowOverlapTargetFilter = "";
    private boolean[] tBelowOverlapTargetFilterArray = new boolean[256];

    private void cacheBelowOverlapTargetFilter() {
        this.stBelowOverlapTargetFilter = oConfiguration.getString("Below.Overlap.TargetFilter");
        if (this.stBelowOverlapTargetFilter != null && !this.stBelowOverlapTargetFilter.isEmpty()) {
            for (Material material : Material.values()) {
                /* Match by ID */
                tBelowOverlapTargetFilterArray[material.getId()] = String.valueOf(material.getId()).matches(this.stBelowOverlapTargetFilter);
            }
        } else {
            for (int n = 0; n < 256; n++) {
                tBelowOverlapTargetFilterArray[n] = false;
            }
        }
    }

    public String getBelowOverlapTargetFilter() {
        return stBelowOverlapTargetFilter;
    }

    public <T> void setBelowOverlapTargetFilter(T newValue) {
        if (newValue instanceof String) {
            oConfiguration.set("Below.Overlap.TargetFilter", (String) newValue);
        } else {
            oConfiguration.set("Below.Overlap.TargetFilter", null);
        }
        cacheBelowOverlapTargetFilter();
    }
    //</editor-fold>
    //</editor-fold>
}
