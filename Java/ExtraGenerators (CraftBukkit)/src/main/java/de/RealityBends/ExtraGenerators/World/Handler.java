/* Authors: Xaymar
 * Copyright: 2013 (c) RealityBends
 * License: This file is part of Project Kube.
 * 
 *      This Source Code Form is subject to the terms of the Mozilla Public
 *      License, v. 2.0. If a copy of the MPL was not distributed with this
 *      file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.RealityBends.ExtraGenerators.World;

import de.RealityBends.ExtraGenerators.Generators.Generator;
import de.RealityBends.ExtraGenerators.Plugin;
import de.RealityBends.ExtraGenerators.Populators.Populator;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.generator.ChunkGenerator;

public class Handler {
    private Plugin oPlugin;
    private World oWorld;
    private Configuration oConfiguration;
    private Generator oGenerator;
    private LinkedList<Populator> oPopulator;
    
    //<editor-fold defaultstate="collapsed" desc="Constructor">
    public Handler(Plugin poPlugin, org.bukkit.World poWorld) {
        oPlugin = poPlugin;
        oWorld = poWorld;
        oConfiguration = new Configuration(new File(poWorld.getWorldFolder() + File.separator + "ExtraGenerators.yml"));
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Enabling and Disabling">
    public void onEnable() {
        // Try and load Configuration.
        try {
            oConfiguration.load();
        } catch (IOException | InvalidConfigurationException ex) {
            oPlugin.getLogger().severe("Failed to load configuration: " + ex.toString() + ".");
        }
    }
    
    public void onDisable() {
        // Destroy Generator instance.
        
        // Try and save Configuration.
        try {
            oConfiguration.save();
        } catch (IOException ex) {
            oPlugin.getLogger().severe("Failed to save configuration: " + ex.toString() + ".");
        }
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="World Generator">
    public Generator getGenerator() {
        return oGenerator;
    }
    //</editor-fold>
}
