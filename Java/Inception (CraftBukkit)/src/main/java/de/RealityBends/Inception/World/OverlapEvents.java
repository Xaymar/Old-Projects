/* Authors: Xaymar
 * Copyright: 2012-2013 (c) Inception Plugin Team.
 * License: CC BY-SA 3.0
 *      Inception by Inception Plugin Team is licensed under a
 *      Creative Commons Attribution-ShareAlike 3.0 Unported
 *      License.
 */
package de.RealityBends.Inception.World;

import org.bukkit.event.Event;
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

public enum OverlapEvents {

    BlockPlace("BlockPlace", BlockPlaceEvent.class),
    BlockBreak("BlockBreak", BlockBreakEvent.class),
    BlockBurn("BlockBurn", BlockBurnEvent.class),
    BlockFade("BlockFade", BlockFadeEvent.class),
    BlockForm("BlockForm", BlockFormEvent.class),
    BlockGrow("BlockGrow", BlockGrowEvent.class),
    BlockSpread("BlockSpread", BlockSpreadEvent.class),
    BlockFromTo("BlockFromTo", BlockFromToEvent.class),
    BlockPhysics("BlockPhysics", BlockPhysicsEvent.class),
    LeavesDecay("LeavesDecay", LeavesDecayEvent.class),
    SignChange("SignChange", SignChangeEvent.class);
    
    // Enum Values
    private String stName;
    private Class<? extends Event> rcEventClass;

    private OverlapEvents(String stName) {
        this(stName, null);
    }
    
    private OverlapEvents(String stName, Class<? extends Event> rcEventClass) {
        this.stName = stName;
        this.rcEventClass = rcEventClass;
    }

    public String getName() {
        return stName;
    }

    public Class<? extends Event> getEventClass() {
        return rcEventClass;
    }
}
