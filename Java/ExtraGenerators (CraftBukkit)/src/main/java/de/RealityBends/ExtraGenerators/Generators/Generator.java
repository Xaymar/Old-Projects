/* Authors: Xaymar
 * Copyright: 2013 (c) RealityBends
 * License: This file is part of Project Kube.
 * 
 *      This Source Code Form is subject to the terms of the Mozilla Public
 *      License, v. 2.0. If a copy of the MPL was not distributed with this
 *      file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.RealityBends.ExtraGenerators.Generators;

import java.util.Random;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

public abstract class Generator extends ChunkGenerator {
    //<editor-fold defaultstate="collapsed" desc="Generator Name (Static)">

    private static String stName = "Base Generator";

    public static String getName() {
        return stName;
    }

    //<editor-fold defaultstate="collapsed" desc="Constructor">
    public Generator() {
        this(null);
    }
    
    public Generator(ConfigurationSection oConfiguration) {
        if (oConfiguration != null) {
            
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Default Populators">
    @Override
    public java.util.List<BlockPopulator> getDefaultPopulators(World oWorld) {
        return super.getDefaultPopulators(oWorld);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Generation: Spawn Location">
    @Override
    public Location getFixedSpawnLocation(World oWorld, Random oRandom) {
        return super.getFixedSpawnLocation(oWorld, oRandom);
    }

    @Override
    public boolean canSpawn(World oWorld, int iX, int iZ) {
        return super.canSpawn(oWorld, iZ, iZ);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Generation: Block Sections">
    @Override
    public byte[][] generateBlockSections(World poWorld, Random poRandom, int piX, int piZ, BiomeGrid poBiomeGrid) {
        return super.generateBlockSections(poWorld, poRandom, piZ, piZ, poBiomeGrid);
    }

    @Override
    public short[][] generateExtBlockSections(World poWorld, Random poRandom, int piX, int piZ, BiomeGrid poBiomeGrid) {
        return super.generateExtBlockSections(poWorld, poRandom, piZ, piZ, poBiomeGrid);
    }
    //</editor-fold>
}
