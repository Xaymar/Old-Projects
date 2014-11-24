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

public class Empty extends Generator {
    private static String stName = "Empty";

    //<editor-fold defaultstate="collapsed" desc="Generation: Spawn Location">
    @Override
    public Location getFixedSpawnLocation(World oWorld, Random oRandom) {
        return new Location(oWorld, 0, 128, 0);
    }

    @Override
    public boolean canSpawn(World oWorld, int iX, int iZ) {
        return true;
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
