/* Authors: Xaymar
 * Copyright: 2013 (c) RealityBends
 * License: This file is part of Project Kube.
 * 
 *      This Source Code Form is subject to the terms of the Mozilla Public
 *      License, v. 2.0. If a copy of the MPL was not distributed with this
 *      file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.RealityBends.ExtraGenerators.World;

import de.RealityBends.ExtraGenerators.Generators.Empty;
import java.io.File;
import java.io.IOException;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public class Configuration {

    private File oFile;
    private YamlConfiguration oConfiguration;
    private long lTSCreated, lTSModified;

    //<editor-fold defaultstate="collapsed" desc="Constructor">
    public Configuration(File poFile) {
        this.oFile = poFile;
        this.oConfiguration = new YamlConfiguration();
        this.lTSCreated = System.currentTimeMillis();
        this.lTSModified = this.lTSCreated;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Loading and Saving">
    public void load() throws IOException, InvalidConfigurationException {
        if (oFile.exists()) {
            oConfiguration.load(oFile);
        } else {
            initialize();
            save();
        }
    }

    public void save() throws IOException {
        save(false);
    }
    
    public void save(boolean ptForce) throws IOException {
        if ((this.lTSModified > this.lTSCreated) || (ptForce)) {
            oConfiguration.save(oFile);
        }
    }

    public void initialize() {
        oConfiguration.options().header("Authors: Xaymar\n"
                + "Copyright: 2012-2013 (c) Inception Plugin Team.\n"
                + "License: CC BY-SA 3.0\n"
                + "     Inception by Inception Plugin Team is licensed under a\n"
                + "     Creative Commons Attribution-ShareAlike 3.0 Unported\n"
                + "     License.");
        
        setWorldGenerator("Empty");
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="World.Generator">
    private String stWorldGenerator = null;

    private void cacheWorldGenerator() {
        this.stWorldGenerator = oConfiguration.getString("World.Generator");
    }

    public String getWorldGenerator() {
        return stWorldGenerator;
    }

    public <T> void setWorldGenerator(T newGenerator) {
        if (newGenerator instanceof Empty) {
            oConfiguration.set("World.Generator", ((Empty)newGenerator).getClass().getName());
        } else if (newGenerator instanceof String) {
            oConfiguration.set("World.Generator", (String)newGenerator);
        } else {
            oConfiguration.set("World.Generator", null);
        }
        cacheWorldGenerator();
    }
    //</editor-fold>
}
