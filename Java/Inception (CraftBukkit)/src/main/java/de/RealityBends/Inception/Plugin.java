/* Authors: Xaymar
 * Copyright: 2012-2013 (c) Inception Plugin Team.
 * License: CC BY-SA 3.0
 *      Inception by Inception Plugin Team is licensed under a
 *      Creative Commons Attribution-ShareAlike 3.0 Unported
 *      License.
 */
package de.RealityBends.Inception;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import de.RealityBends.Inception.World.Handler;
import de.RealityBends.Inception.World.OverlapEvents;
import de.RealityBends.Inception.World.OverlapDelayBlocks;
import de.RealityBends.Inception.World.Cache;
import de.RealityBends.Inception.World.CacheQueries;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import javax.xml.bind.DatatypeConverter;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class Plugin
        extends JavaPlugin
        implements Listener {

    //<editor-fold defaultstate="collapsed" desc="Variables">
    private File oDataDirectory;
    private File oWorldDirectory;
    private HashMap<World, Handler> oWorldHandlerMap;
    private FixedMetadataValue oNoFallDamageMetadata = new FixedMetadataValue(this, true);
    private boolean[] tOverlappingDelayedActionArr = new boolean[256];
    private PluginMetrics oPluginMetrics;
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Enable and Disable">
    @Override
    public void onEnable() {
        getLogger().info("[Core] Enabling...");
        
        // Create directory references and create nonexisting folders.
        oDataDirectory = this.getDataFolder();
        if (!oDataDirectory.exists()) {
            getLogger().fine("Plugin directory is missing, creating...");
            if (!oDataDirectory.mkdirs()) {
                getLogger().severe("Failed to create plugin directory.");
            }
        }

        oWorldDirectory = new File(oDataDirectory + File.separator + "per-world");
        if (!oWorldDirectory.exists()) {
            getLogger().fine("Per-world directory is missing, creating...");
            if (!oWorldDirectory.mkdirs()) {
                getLogger().severe("Failed to create per-world directory.");
            }
        }

        // Create configuration file references and load the configuration.
        oPluginConfigurationFile = new File(oDataDirectory + File.separator + "config.yml");
        oPluginConfiguration = new YamlConfiguration();
        oDefaultConfigurationFile = new File(oDataDirectory + File.separator + "default.yml");
        oDefaultConfiguration = new YamlConfiguration();
        try {
            loadConfiguration();
        } catch (IOException | InvalidConfigurationException ex) {
            getLogger().log(Level.SEVERE, "Failed to load configuration:", ex);
        }
        try {
            loadDefaultConfiguration();
        } catch (IOException | InvalidConfigurationException ex) {
            getLogger().log(Level.SEVERE, "Failed to load default configuration:", ex);
        }

        // Register our global event handler
        Bukkit.getPluginManager().registerEvents(this, this);

        // Create and fill the array of blocks that need delayed action.
        for (OverlapDelayBlocks oDCB : OverlapDelayBlocks.values()) {
            tOverlappingDelayedActionArr[oDCB.getTypeId()] = true;
        }

        // Enable the Cache Database.
        if (tCacheEnabled) {
            enableCache();
        }

        // Create a World <-> WorldHandler HashMap for later use.
        oWorldHandlerMap = new HashMap<>();
        for (World oWorld : getServer().getWorlds()) {
            Handler oNewHandler = new Handler(this, oWorld);
            oWorldHandlerMap.put(oWorld, oNewHandler);
            oNewHandler.onEnable();
        }
        
        // Initialize Plugin Metrics
        oPluginMetrics = new PluginMetrics(this);
        oPluginMetrics.onEnable();
        
        getLogger().info("[Core] Done.");
    }

    @Override
    public void onDisable() {
        getLogger().info("[Core] Disabling...");
        
        // Deinitialize Plugin Metrics
        oPluginMetrics.onDisable();
        oPluginMetrics = null;
        
        // Unregister global event handler.
        org.bukkit.event.HandlerList.unregisterAll((Listener) this);

        // Delete and unload world handlers and destroy the list of world handlers.
        for (World oWorld : getServer().getWorlds()) {
            oWorldHandlerMap.remove(oWorld).onDisable();
        }
        oWorldHandlerMap = null;

        // Disable the OverlapEventsCache Database.
        disableCache();

        // Cancel any remaining tasks.
        getServer().getScheduler().cancelTasks(this);

        // Null remaining references.
        oDefaultConfiguration = null;
        oDefaultConfigurationFile = null;
        oPluginConfiguration = null;
        oPluginConfigurationFile = null;
        oWorldDirectory = null;
        oDataDirectory = null;

        getLogger().info("[Core] Done.");
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Plugin Configuration">
    private File oPluginConfigurationFile;
    private YamlConfiguration oPluginConfiguration;

    //<editor-fold defaultstate="collapsed" desc="Loading and Saving">
    private void loadConfiguration()
            throws IOException, InvalidConfigurationException {
        if (oPluginConfigurationFile.exists()) {
            oPluginConfiguration.load(oPluginConfigurationFile);
        }

        tGeneralPredictPosition = oPluginConfiguration.getBoolean("General.PredictPosition", tGeneralPredictPosition);
        iGeneralTaskWaitTime = oPluginConfiguration.getInt("General.TaskWaitTime", iGeneralTaskWaitTime);
        tCacheEnabled = oPluginConfiguration.getBoolean("Cache.Enabled", tCacheEnabled);
        stCacheFile = oPluginConfiguration.getString("Cache.File", stCacheFile);
        iCacheRetryLimit = oPluginConfiguration.getInt("Cache.RetryLimit", iCacheRetryLimit);
        for (CacheQueries oQueryType : CacheQueries.values()) {
            oCacheQueryMap.put(oQueryType, oPluginConfiguration.getString("Cache.Query." + oQueryType.getName(), oQueryType.getDefaultQuery()));
        }

        if (!oPluginConfigurationFile.exists()) {
            saveConfiguration();
        }
    }

    private void saveConfiguration()
            throws IOException {
        oPluginConfiguration.options().header("Authors: Xaymar\n"
                                              + "Copyright: 2012-2013 (c) Inception Plugin Team.\n"
                                              + "License: CC BY-SA 3.0\n"
                                              + "     Inception by Inception Plugin Team is licensed under a\n"
                                              + "     Creative Commons Attribution-ShareAlike 3.0 Unported\n"
                                              + "     License.\n"
                                              + "\n"
                                              + "Node                     Explanation\n"
                                              + "-------------------------------------------------------------------------------\n"
                                              + "General:                 Container for general options.\n"
                                              + "  PredictPosition:       Predict position of entities TaskWaitTime ticks ahead, so that Inception can run on high TaskWaitTime?\n"
                                              + "  TaskWaitTime:          How long should the task wait before reprocessing?\n"
                                              + "Cache:                   Things related to caching.\n"
                                              + "  Enabled:               Enable caching of actions?\n"
                                              + "  File:                  Where should we store the cache?\n"
                                              + "  RetryLimit:            How often should we retry to place a block if it failed?\n"
                                              + "  Query:                 Queries used in caching actions.\n"
                                              + "    CreateWorld:         Query to create the table for a single world.\n"
                                              + "    DeleteWorld:         Query to delete the table for a single world.\n"
                                              + "    PlaceEvent:          Query to add a cached event.\n"
                                              + "    GetEvents:           Query to get all events in a single chunk.\n"
                                              + "    RemoveEvent:         Query to remove a cached event that has been processed.\n");

        oPluginConfiguration.set("General.PredictPosition", tGeneralPredictPosition);
        oPluginConfiguration.set("General.TaskWaitTime", iGeneralTaskWaitTime);
        oPluginConfiguration.set("Cache.Enabled", tCacheEnabled);
        oPluginConfiguration.set("Cache.File", stCacheFile);
        oPluginConfiguration.set("Cache.RetryLimit", iCacheRetryLimit);
        for (CacheQueries oQueryType : CacheQueries.values()) {
            oPluginConfiguration.set("Cache.Query." + oQueryType.getName(), oQueryType.getDefaultQuery());
        }

        oPluginConfiguration.save(oPluginConfigurationFile);
    }

    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="General.PredictPosition">
    private boolean tGeneralPredictPosition = true;

    public boolean isGeneralPredictPositionEnabled() {
        return tGeneralPredictPosition;
    }

    public void setGeneralPredictPositionEnabled(boolean ptGeneralPredictPosition) {
        tGeneralPredictPosition = ptGeneralPredictPosition;
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="General.TaskWaitTime">

    private int iGeneralTaskWaitTime = 5;

    public int getGeneralTaskWaitTime() {
        return iGeneralTaskWaitTime;
    }

    public void setGeneralTaskWaitTime(int piGeneralTaskWaitTime) {
        iGeneralTaskWaitTime = piGeneralTaskWaitTime;
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Cache.Enabled">

    private boolean tCacheEnabled = true;

    public boolean getCacheEnabled() {
        return tCacheEnabled;
    }

    public void setCacheEnabled(boolean ptCacheEnabled) {
        tCacheEnabled = ptCacheEnabled;
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Cache.File">

    private String stCacheFile = "." + File.separator + "Cache.db";

    public String getCacheFile() {
        return stCacheFile;
    }

    public void setCacheFile(String pstCacheFile) {
        stCacheFile = pstCacheFile;
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Cache.RetryLimit">

    private int iCacheRetryLimit = 5;

    public int getCacheRetryLimit() {
        return iCacheRetryLimit;
    }

    public void setCacheRetryLimit(int piCacheRetryLimit) {
        iCacheRetryLimit = piCacheRetryLimit;
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Cache.Query.*">

    private EnumMap<CacheQueries, String> oCacheQueryMap = new EnumMap<>(CacheQueries.class);

    public String getCacheQuery(CacheQueries poQueryType) {
        return oCacheQueryMap.get(poQueryType);
    }

    public void setCacheQuery(CacheQueries poQueryType, String pstQuery) {
        oCacheQueryMap.put(poQueryType, pstQuery);
    }
    //</editor-fold>
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Default Configuration">
    private File oDefaultConfigurationFile;
    private YamlConfiguration oDefaultConfiguration;

    public void loadDefaultConfiguration()
            throws IOException, InvalidConfigurationException {
        if (oDefaultConfigurationFile.exists()) {
            oDefaultConfiguration.load(oDefaultConfigurationFile);
            initDefaultConfiguration();
        } else {
            saveDefaultConfiguration();
        }
    }

    private void initDefaultConfiguration() {
        oDefaultConfiguration.set("World.Enabled", oDefaultConfiguration.getBoolean("World.Enabled", false));
        oDefaultConfiguration.set("World.SynchronizeWith", oDefaultConfiguration.getString("World.SynchronizeWith", ""));
        for (OverlapEvents oBlockEvent : OverlapEvents.values()) {
            oDefaultConfiguration.set("World.OverlapEvents." + oBlockEvent.getName(), oDefaultConfiguration.getBoolean("World.OverlapEvents." + oBlockEvent.getName(), false));
        }
        oDefaultConfiguration.set("Above.World", oDefaultConfiguration.getString("Above.World", ""));
        oDefaultConfiguration.set("Above.Teleport.Enabled", oDefaultConfiguration.getBoolean("Above.Teleport.Enabled", false));
        oDefaultConfiguration.set("Above.Teleport.From", oDefaultConfiguration.getInt("Above.Teleport.From", 247));
        oDefaultConfiguration.set("Above.Teleport.To", oDefaultConfiguration.getInt("Above.Teleport.To", 24));
        oDefaultConfiguration.set("Above.Teleport.Safe", oDefaultConfiguration.getBoolean("Above.Teleport.Safe", false));
        oDefaultConfiguration.set("Above.Teleport.Platform", oDefaultConfiguration.getBoolean("Above.Teleport.Platform", false));
        oDefaultConfiguration.set("Above.Teleport.EntityFilter", oDefaultConfiguration.getString("Above.Teleport.EntityFilter", "(Painting|EnderDragon|Lightning|Weather|ComplexEntityPart)"));
        oDefaultConfiguration.set("Above.Overlap.Enabled", oDefaultConfiguration.getBoolean("Above.Overlap.Enabled", false));
        oDefaultConfiguration.set("Above.Overlap.From", oDefaultConfiguration.getInt("Above.Overlap.From", 255));
        oDefaultConfiguration.set("Above.Overlap.To", oDefaultConfiguration.getInt("Above.Overlap.To", 0));
        oDefaultConfiguration.set("Above.Overlap.Layers", oDefaultConfiguration.getInt("Above.Overlap.Layers", 32));
        oDefaultConfiguration.set("Above.Overlap.SourceFilter", oDefaultConfiguration.getString("Above.Overlap.SourceFilter", ""));
        oDefaultConfiguration.set("Above.Overlap.TargetFilter", oDefaultConfiguration.getString("Above.Overlap.TargetFilter", ""));
        oDefaultConfiguration.set("Below.World", oDefaultConfiguration.getString("Below.World", ""));
        oDefaultConfiguration.set("Below.Teleport.Enabled", oDefaultConfiguration.getBoolean("Below.Teleport.Enabled", false));
        oDefaultConfiguration.set("Below.Teleport.From", oDefaultConfiguration.getInt("Below.Teleport.From", 8));
        oDefaultConfiguration.set("Below.Teleport.To", oDefaultConfiguration.getInt("Below.Teleport.To", 231));
        oDefaultConfiguration.set("Below.Teleport.Safe", oDefaultConfiguration.getBoolean("Below.Teleport.Safe", false));
        oDefaultConfiguration.set("Below.Teleport.PreventFalldamage", oDefaultConfiguration.getBoolean("Below.Teleport.PreventFalldamage", false));
        oDefaultConfiguration.set("Below.Teleport.EntityFilter", oDefaultConfiguration.getString("Below.Teleport.EntityFilter", "(Painting|EnderDragon|Lightning|Weather|ComplexEntityPart)"));
        oDefaultConfiguration.set("Below.Overlap.Enabled", oDefaultConfiguration.getBoolean("Below.Overlap.Enabled", false));
        oDefaultConfiguration.set("Below.Overlap.From", oDefaultConfiguration.getInt("Below.Overlap.From", 0));
        oDefaultConfiguration.set("Below.Overlap.To", oDefaultConfiguration.getInt("Below.Overlap.To", 255));
        oDefaultConfiguration.set("Below.Overlap.Layers", oDefaultConfiguration.getInt("Below.Overlap.Layers", 32));
        oDefaultConfiguration.set("Below.Overlap.SourceFilter", oDefaultConfiguration.getString("Below.Overlap.SourceFilter", ""));
        oDefaultConfiguration.set("Below.Overlap.TargetFilter", oDefaultConfiguration.getString("Below.Overlap.TargetFilter", ""));
    }

    public void saveDefaultConfiguration()
            throws IOException {
        oDefaultConfiguration.options().header("Authors: Xaymar\n"
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
                                               + "    BlockPlace:          Trigger when a block is placed.\n"
                                               + "    BlockBreak:          Trigger when a block is broken.\n"
                                               + "    BlockBurn:           Trigger when a block burns away.\n"
                                               + "    BlockFade:           Trigger when a block fades away.\n"
                                               + "    BlockForm:           Trigger when a block forms.\n"
                                               + "    BlockGrow:           Trigger when a block grows.\n"
                                               + "    BlockSpread:         Trigger when a block spreads.\n"
                                               + "    BlockFromTo:         Trigger when a block 'moves' (liquids, buggy).\n"
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
                                               + "    TargetFilter:        A Regular Expression that matches all unreplacable materials while Overlapping. Matches by ID.\n");

        initDefaultConfiguration();

        oDefaultConfiguration.save(oDefaultConfigurationFile);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Cache Database">
    protected boolean tCacheAvailable = false;
    private Cache oCacheDatabase;
    private HashMap<World, EnumMap<CacheQueries, PreparedStatement>> oCachePreparedQueryMap;

    private void enableCache() {
        if (oCacheDatabase == null) {
            getLogger().fine("[Cache] Enabling...");

            String stRealCacheFile = stCacheFile;
            if (stCacheFile.substring(0, 2).equals("." + File.separator)) {
                stRealCacheFile = oDataDirectory.getAbsolutePath() + File.separator + stCacheFile.substring(2);
            }

            try {
                oCacheDatabase = new Cache(new File(stRealCacheFile));
            } catch (ClassNotFoundException ex) {
                getLogger().log(Level.SEVERE, "[Cache] No SQLite JDBC Driver available:", ex);
                return;
            }

            try {
                oCacheDatabase.open();
                tCacheAvailable = true;

                // Create a World <-> EnumMap<QueryTypes, PreparedStatement> Map for later use.
                oCachePreparedQueryMap = new HashMap<>();
                for (World oWorld : Bukkit.getWorlds()) {
                    cacheInitializeWorld(oWorld);
                }
            } catch (SQLException ex) {
                getLogger().severe("[Cache] Unexpected error while opening Database, see log file.");
                getLogger().finest("[Cache]   " + ex.getMessage());
            }
        }

        getLogger().info("[Cache] Enabled.");
    }

    private void disableCache() {
        if (oCacheDatabase != null) {
            getLogger().fine("[Cache] Disabling...");

            tCacheAvailable = false;
            oCachePreparedQueryMap.clear();
            oCachePreparedQueryMap = null;
            try {
                oCacheDatabase.close();
            } catch (SQLException ex) {
                getLogger().log(Level.SEVERE, "[Cache] Unexpected error while closing Database:", ex);
            }
            oCacheDatabase = null;

            getLogger().info("[Cache] Disabled.");
        }
    }

    //<editor-fold defaultstate="collapsed" desc="Prepared Statements">
    private void cacheInitializeWorld(World poWorld) {
        if (tCacheEnabled && tCacheAvailable) {
            String stEscapedWorldName = Utility.escapeForSQL(poWorld.getName());

            // CreateWorld
            try {
                oCacheDatabase.execute(oCacheQueryMap.get(CacheQueries.CreateWorld).replace("{0}", stEscapedWorldName));
            } catch (SQLException ex) {
                getLogger().warning(" Failed to create Table, see log.");
                getLogger().finest("[Cache] [" + poWorld.getName() + "]   " + ex.getMessage());
            }

            EnumMap<CacheQueries, PreparedStatement> oCachePreparedStatementMap = new EnumMap<>(CacheQueries.class);
            for (CacheQueries oQueryType : CacheQueries.values()) {
                if (oQueryType.isPreparable()) {
                    String stProperQuery = oCacheQueryMap.get(oQueryType).replace("{0}", stEscapedWorldName);

                    try {
                        oCachePreparedStatementMap.put(oQueryType, oCacheDatabase.prepareStatement(stProperQuery));
                    } catch (SQLException ex) {
                        getLogger().warning("[Cache] [" + poWorld.getName() + "] Failed to create " + oQueryType.getName() + " Query, see log.");
                        getLogger().finest("[Cache] [" + poWorld.getName() + "]   " + ex.getMessage());
                    }
                }
            }
            oCachePreparedQueryMap.put(poWorld, oCachePreparedStatementMap);
        }
    }

    private PreparedStatement cacheGetPreparedStatement(World poWorld, CacheQueries oQueryType) {
        if (tCacheEnabled && tCacheAvailable) {
            EnumMap<CacheQueries, PreparedStatement> oCachePreparedStatementMap = oCachePreparedQueryMap.get(poWorld);

            if (oCachePreparedStatementMap != null) {
                return oCachePreparedStatementMap.get(oQueryType);
            }
        }

        return null;
    }

    private void cacheDeinitializeWorld(World poWorld) {
        if (tCacheEnabled && tCacheAvailable) {
            if (oCachePreparedQueryMap.containsKey(poWorld)) {
                EnumMap<CacheQueries, PreparedStatement> oCachePreparedStatementMap = oCachePreparedQueryMap.get(poWorld);
                for (Map.Entry<CacheQueries, PreparedStatement> oMapEntry : oCachePreparedStatementMap.entrySet()) {
                    try {
                        oMapEntry.getValue().close();
                    } catch (SQLException ex) {
                        getLogger().warning("[Cache] [" + poWorld.getName() + "] Failed to close " + oMapEntry.getKey().getName() + " Query, see log.");
                        getLogger().finest("[Cache] [" + poWorld.getName() + "]   " + ex.getMessage());
                    }
                }

                oCachePreparedQueryMap.remove(poWorld);
            }
        }
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Queue Events">

    public boolean cacheEvent(World poWorld, int piWorldX, int piWorldZ, short psWorldY, short psTypeId, byte pbData) {
        if (tCacheEnabled && tCacheAvailable) {
            try {
                PreparedStatement oWorldPS = cacheGetPreparedStatement(poWorld, CacheQueries.AddEvent);
                if (oWorldPS != null) {
                    oWorldPS.setInt(1, (int) Math.floor(piWorldX / 16.0d));
                    oWorldPS.setInt(2, (int) Math.floor(piWorldZ / 16.0d));
                    oWorldPS.setByte(3, (byte) (piWorldX & 15));
                    oWorldPS.setByte(4, (byte) (piWorldZ & 15));
                    oWorldPS.setShort(5, psWorldY);
                    oWorldPS.setByte(6, (byte) 0); // 0 = Block Event, 1 = Sign Event, 2 = Inventory Event
                    oWorldPS.setString(7, String.valueOf(psTypeId) + "," + String.valueOf(pbData));
                    return oWorldPS.execute();
                } else {
                    getLogger().log(Level.SEVERE, "[Cache] Failed to queue Event: No PreparedStatement available.");
                }
            } catch (SQLException ex) {
                getLogger().warning("[Cache] Failed to cache Event.");
                getLogger().finest("[Cache] [" + poWorld.getName() + "]   " + ex.getMessage());
            }
        }
        return false;
    }

    public boolean cacheEvent(World poWorld, int piWorldX, int piWorldZ, short psWorldY, String pstSignLine0, String pstSignLine1, String pstSignLine2, String pstSignLine3) {
        if (tCacheEnabled && tCacheAvailable) {
            try {
                StringBuilder oSignData = new StringBuilder();
                oSignData.append(DatatypeConverter.printBase64Binary(pstSignLine0.getBytes())).append(",");
                oSignData.append(DatatypeConverter.printBase64Binary(pstSignLine1.getBytes())).append(",");
                oSignData.append(DatatypeConverter.printBase64Binary(pstSignLine2.getBytes())).append(",");
                oSignData.append(DatatypeConverter.printBase64Binary(pstSignLine3.getBytes()));

                PreparedStatement oWorldPS = cacheGetPreparedStatement(poWorld, CacheQueries.AddEvent);
                oWorldPS.setInt(1, (int) Math.floor(piWorldX / 16.0d));
                oWorldPS.setInt(2, (int) Math.floor(piWorldZ / 16.0d));
                oWorldPS.setByte(3, (byte) (piWorldX & 15));
                oWorldPS.setByte(4, (byte) (piWorldZ & 15));
                oWorldPS.setShort(5, psWorldY);
                oWorldPS.setByte(6, (byte) 1); // 0 = Block Event, 1 = Sign Event, 2 = Inventory Event
                oWorldPS.setString(7, oSignData.toString());
                return oWorldPS.execute();
            } catch (SQLException ex) {
                getLogger().warning("[Cache] Failed to cache Event.");
                getLogger().finest("[Cache] [" + poWorld.getName() + "]   " + ex.getMessage());
            }
        }
        return false;
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Event Workers">

    class BlockEventWorker
            implements Runnable {

        private JavaPlugin oPlugin;
        private World oWorld;
        private Chunk oChunk;
        private byte bX, bZ, bData;
        private short sY, sTypeId;
        private int iTries = 1, iMaxTries = 1;

        public BlockEventWorker(JavaPlugin poPlugin, World poWorld, Chunk poChunk, byte pbX, byte pbZ, short psY, short psTypeId, byte pbData, int piMaxTries) {
            oPlugin = poPlugin;
            oWorld = poWorld;
            oChunk = poChunk;
            bX = pbX;
            bZ = pbZ;
            sY = psY;
            sTypeId = psTypeId;
            bData = pbData;
            iMaxTries = piMaxTries;
        }

        @Override
        public void run() {
            Block oBlock = oChunk.getBlock(bX, sY, bZ);
            oBlock.setTypeIdAndData(sTypeId, bData, false);

            // Retry iMaxTries times.
            if (iTries < iMaxTries && (oBlock.getTypeId() != sTypeId || oBlock.getData() != bData)) {
                iTries++; // Increase try counter.
                Bukkit.getScheduler().scheduleSyncDelayedTask(oPlugin, this, 1);
            }
        }

    }

    class SignEventWorker
            implements Runnable {

        private JavaPlugin oPlugin;
        private World oWorld;
        private Chunk oChunk;
        private byte bX, bZ;
        private short sY;
        private String stLine0, stLine1, stLine2, stLine3;
        private int iTries = 1, iMaxTries = 1;

        public SignEventWorker(JavaPlugin poPlugin, World poWorld, Chunk poChunk, byte pbX, byte pbZ, short psY, String pstLine0, String pstLine1, String pstLine2, String pstLine3, int piMaxTries) {
            oPlugin = poPlugin;
            oWorld = poWorld;
            oChunk = poChunk;
            bX = pbX;
            bZ = pbZ;
            sY = psY;
            stLine0 = pstLine0;
            stLine1 = pstLine1;
            stLine2 = pstLine2;
            stLine3 = pstLine3;
            iMaxTries = piMaxTries;
        }

        @Override
        public void run() {
            BlockState oBlockState = oChunk.getBlock(bX, sY, bZ).getState();
            if (oBlockState instanceof Sign) {
                Sign oSign = (Sign) oBlockState;

                oSign.setLine(0, stLine0);
                oSign.setLine(1, stLine1);
                oSign.setLine(2, stLine2);
                oSign.setLine(3, stLine3);

                // Retry iMaxTries times.
                if (iTries < iMaxTries && (stLine0.equals(oSign.getLine(0)) && stLine1.equals(oSign.getLine(1)) && stLine2.equals(oSign.getLine(2)) && stLine3.equals(oSign.getLine(3)))) {
                    iTries++; // Increase try counter.
                    Bukkit.getScheduler().scheduleSyncDelayedTask(oPlugin, this, 1);
                }
            }
        }

    }
    //</editor-fold>
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Event Handlers">
    // Handle Inception command.
    @Override
    public boolean onCommand(CommandSender oSender, Command oCommand, String stLabel, String[] args) {
        Utility.SendMessage(oSender, "Running v" + getDescription().getVersion());
        return true;
    }

    // Install a new WorldHandler when a world is loaded.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldLoad(WorldLoadEvent oEvent) {
        if (!oWorldHandlerMap.containsKey(oEvent.getWorld())) {
            World oWorld = oEvent.getWorld();

            // Initialize the caching for this world.
            if (tCacheEnabled && tCacheAvailable) {
                cacheInitializeWorld(oWorld);
            }

            // Create and enable a new WorldHandler.
            Handler oNewHandler = new Handler(this, oWorld);
            oNewHandler.onEnable();

            // Put it into the World <-> WorldHandler map.
            oWorldHandlerMap.put(oWorld, oNewHandler);
        }
    }

    // Uninstall the installed WorldHandler when a world is unloaded.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent oEvent) {
        if (oWorldHandlerMap.containsKey(oEvent.getWorld())) {
            // Deinitialize the caching for this world.
            cacheDeinitializeWorld(oEvent.getWorld());

            // Disable and remove the existing WorldHandler.
            oWorldHandlerMap.get(oEvent.getWorld()).onDisable();
            oWorldHandlerMap.remove(oEvent.getWorld());
        }

    }

    // Find and execute cached Actions for this Chunk.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent oEvent) {
        PreparedStatement oGetEventsPS = cacheGetPreparedStatement(oEvent.getWorld(), CacheQueries.GetEvents);
        final PreparedStatement oRemoveEventPS = cacheGetPreparedStatement(oEvent.getWorld(), CacheQueries.RemoveEvent);

        if (oGetEventsPS != null && oRemoveEventPS != null) {
            World oWorld = oEvent.getWorld();
            Chunk oChunk = oEvent.getChunk();

            // Retrieve cached events.
            try {
                oGetEventsPS.setInt(1, oChunk.getX());
                oGetEventsPS.setInt(2, oChunk.getZ());
                ResultSet oCacheResult = oGetEventsPS.executeQuery();
                if (oCacheResult != null) {
                    while (oCacheResult.next()) {
                        byte bX = oCacheResult.getByte("X"), bZ = oCacheResult.getByte("Z");
                        short sY = oCacheResult.getShort("Y");
                        byte bType = oCacheResult.getByte("Type");
                        String[] stData = oCacheResult.getString("Data").split(",");

                        switch (bType) {
                            case 0: // Block Event
                                Bukkit.getScheduler().scheduleSyncDelayedTask(this, new BlockEventWorker(this, oWorld, oChunk, bX, bZ, sY, Short.valueOf(stData[0]), Byte.valueOf(stData[1]), iCacheRetryLimit), 1);
                                break;
                            case 1: // Sign Event
                                Bukkit.getScheduler().scheduleSyncDelayedTask(this, new SignEventWorker(this, oWorld, oChunk, bX, bZ, sY, DatatypeConverter.parseBase64Binary(stData[0]).toString(), DatatypeConverter.parseBase64Binary(stData[1]).toString(), DatatypeConverter.parseBase64Binary(stData[2]).toString(), DatatypeConverter.parseBase64Binary(stData[3]).toString(), iCacheRetryLimit), 1);
                                break;
                            case 2: // Inventory Event
                                break;
                        }
                    }
                    oCacheResult.close();
                }
            } catch (SQLException ex) {
                getLogger().log(Level.SEVERE, "[Cache] <" + oWorld.getName() + "> Failed to retrieve cached events:", ex);
            }
        }
    }

    // Prevent fall damage for some entities when the metadata is given and make sure other plugins can override this.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent oEvent) {
        if (oEvent.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (oEvent.getEntityType().isAlive()) {
                if (oEvent.getEntity().hasMetadata("takeFallDamage")) {
                    oEvent.setCancelled(true);
                    oEvent.getEntity().removeMetadata("takeFallDamage", this);
                }
            }
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Accessor Functions">
    public File getWorldDirectory() {
        return oWorldDirectory;
    }

    public YamlConfiguration getDefaultConfiguration() {
        return oDefaultConfiguration;
    }

    public HashMap<World, Handler> getWorldHandlerMap() {
        return oWorldHandlerMap;
    }

    public FixedMetadataValue getNoFalldamageMetadata() {
        return oNoFallDamageMetadata;
    }

    public boolean[] getOverlappingDelayedActionArray() {
        return tOverlappingDelayedActionArr;
    }
    //</editor-fold>
}