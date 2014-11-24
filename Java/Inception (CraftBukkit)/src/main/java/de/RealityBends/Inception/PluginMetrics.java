/* Authors: Xaymar
 * Copyright: 2012-2013 (c) Inception Plugin Team.
 * License: CC BY-SA 3.0
 *      Inception by Inception Plugin Team is licensed under a
 *      Creative Commons Attribution-ShareAlike 3.0 Unported
 *      License.
 */
package de.RealityBends.Inception;

import de.RealityBends.Inception.World.Handler;
import java.io.IOException;
import java.util.logging.Level;
import org.mcstats.Metrics;
import org.mcstats.Metrics.Graph;
import org.mcstats.Metrics.Plotter;

public class PluginMetrics {

    private Plugin oPlugin;
    private Metrics oMetrics;
    private Graph gModulesEnabled;
    private Graph gWorldStatus;

    public PluginMetrics(Plugin poPlugin) {
        oPlugin = poPlugin;
        try {
            oMetrics = new Metrics(oPlugin);
        } catch (Exception ex) {
            oPlugin.getLogger().warning("[Metrics] Unable to initialize.");
            oPlugin.getLogger().log(Level.FINEST, ex.getMessage(), ex);
        }
    }

    public void onEnable() {
        final Plugin foPlugin = oPlugin;
        oPlugin.getLogger().info("[Metrics] Starting Metrics...");

        // Track which modules are enabled.
        gModulesEnabled = oMetrics.createGraph("Modules");
        gModulesEnabled.addPlotter(new Plotter("Cache") {
            @Override
            public int getValue() {
                return foPlugin.getCacheEnabled() ? 1 : 0;
            }

        });
        gModulesEnabled.addPlotter(new Plotter("Overlap") {
            @Override
            public int getValue() {
                boolean tOverlapEnabled = false;

                for (Handler oHandler : foPlugin.getWorldHandlerMap().values()) {
                    if (oHandler.getWorldEnabled() && ((!oHandler.getAboveWorld().isEmpty() && oHandler.getAboveOverlapEnabled()) || (!oHandler.getBelowWorld().isEmpty() && oHandler.getBelowOverlapEnabled()))) {
                        tOverlapEnabled = true;
                        break;
                    }
                }

                return tOverlapEnabled ? 1 : 0;
            }
        });
        gModulesEnabled.addPlotter(new Plotter("Teleport") {
            @Override
            public int getValue() {
                boolean tTeleportEnabled = false;

                for (Handler oHandler : foPlugin.getWorldHandlerMap().values()) {
                    if (oHandler.getWorldEnabled() && ((!oHandler.getAboveWorld().isEmpty() && oHandler.getAboveTeleportEnabled()) || (!oHandler.getBelowWorld().isEmpty() && oHandler.getBelowTeleportEnabled()))) {
                        tTeleportEnabled = true;
                        break;
                    }
                }

                return tTeleportEnabled ? 1 : 0;
            }
        });
        gModulesEnabled.addPlotter(new Plotter("Time Synchronization") {
            @Override
            public int getValue() {
                boolean tTimeSyncEnabled = false;

                for (Handler oHandler : foPlugin.getWorldHandlerMap().values()) {
                    if (oHandler.getWorldEnabled() && !oHandler.getWorldSynchronizeWith().isEmpty()) {
                        tTimeSyncEnabled = true;
                        break;
                    }
                }

                return tTimeSyncEnabled ? 1 : 0;
            }
        });

        // Track how many worlds are enabled and disabled.
        gWorldStatus = oMetrics.createGraph("Worlds");
        gWorldStatus.addPlotter(new Plotter("Enabled") {
            @Override
            public int getValue() {
                int iWorlds = 0;
                for (Handler oHandler : foPlugin.getWorldHandlerMap().values()) {
                    iWorlds += oHandler.getWorldEnabled() ? 1 : 0;
                }
                return iWorlds;
            }

        });
        gWorldStatus.addPlotter(new Plotter("Disabled") {
            @Override
            public int getValue() {
                int iWorlds = 0;
                for (Handler oHandler : foPlugin.getWorldHandlerMap().values()) {
                    iWorlds += oHandler.getWorldEnabled() ? 0 : 1;
                }
                return iWorlds;
            }

        });

        oMetrics.start();
        oPlugin.getLogger().info("[Metrics] Done.");
    }

    public void onDisable() {
        try {
            oMetrics.disable();
        } catch (IOException ex) {
            oPlugin.getLogger().warning("[Metrics] Unable to deinitialize.");
            oPlugin.getLogger().log(Level.FINEST, ex.getMessage(), ex);
        }
        oMetrics = null;
    }

}
