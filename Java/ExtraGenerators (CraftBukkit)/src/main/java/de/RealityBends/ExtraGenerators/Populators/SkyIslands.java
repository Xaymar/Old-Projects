/* Authors: Xaymar
 * Copyright: 2013 (c) RealityBends
 * License: This file is part of Project Kube.
 * 
 *      This Source Code Form is subject to the terms of the Mozilla Public
 *      License, v. 2.0. If a copy of the MPL was not distributed with this
 *      file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.RealityBends.ExtraGenerators.Populators;

import java.util.Random;
import net.minecraft.server.v1_6_R2.ChunkSection;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_6_R2.CraftChunk;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.util.noise.NoiseGenerator;
import org.bukkit.util.noise.SimplexNoiseGenerator;

public class SkyIslands extends BlockPopulator {

    public double dThreshold = 0.7;
    public double d1XZ = 0.01;
    public double d2XZ = 0.005;
    public double d3XZ = 0.01;

    @Override
    public void populate(World oWorld, Random oRandom, Chunk oChunk) {
        NoiseGenerator oSNG = new SimplexNoiseGenerator(oWorld);

        // Get all Chunk Sections for speed reasons.
        ChunkSnapshot oChunkSnapshot = oChunk.getChunkSnapshot();
        net.minecraft.server.v1_6_R2.Chunk oMCChunk = ((CraftChunk) oChunk).getHandle();
        ChunkSection[] oChunkSectionArr = oMCChunk.i();
        boolean tInitializeLighting = false; // We may have to generate a ChunkSection.

        for (byte bX = 0; bX < 16; bX++) {
            for (byte bZ = 0; bZ < 16; bZ++) {
                double dNoiseX = (oChunk.getX() << 4) | bX;
                double dNoiseZ = (oChunk.getZ() << 4) | bZ;
                double dNoise = oSNG.noise(dNoiseX * d1XZ, dNoiseZ * d1XZ)
                        + oSNG.noise(dNoiseX * d2XZ, dNoiseZ * d2XZ)
                        - oSNG.noise(dNoiseX * d3XZ, dNoiseZ * d3XZ);

                // Do we hit or pass through the threshold?
                if (dNoise > dThreshold) {
                    // Get Highest Block Y at XZ
                    int iHighestBlockY = oChunkSnapshot.getHighestBlockYAt(bX, bZ); 
                    //int iHeight = (int) ((dNoise - 0.7) * 64 * (0.75 + oSNG.noise(dNoiseX * 0.03, dNoiseZ * 0.03) / 2));
                    int iHeight = (int) ((dNoise - dThreshold) * 32);

                    for (int iPosY = iHighestBlockY; iPosY >= (iHighestBlockY - iHeight); iPosY--) {
                        int iOffsetY = iPosY + 64;
                        ChunkSection oChunkSection = oChunkSectionArr[iOffsetY >> 4];
                        if (oChunkSection == null) {
                            oChunkSection = oChunkSectionArr[iOffsetY >> 4] = new ChunkSection(iOffsetY >> 4 << 4, !oMCChunk.world.worldProvider.f);
                            tInitializeLighting = true;
                        }

                        oChunkSection.setTypeId(bX, iOffsetY & 15, bZ, oMCChunk.getTypeId(bX, iPosY, bZ));
                        oMCChunk.f(bX, iPosY, bZ);
                        oMCChunk.f(bX, iOffsetY, bZ);
                    }
                }
            }
        }

        if (tInitializeLighting) {
            oMCChunk.initLighting();
        }
    }
}
