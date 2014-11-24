/* Authors: Xaymar
 * Copyright: 2012-2013 (c) Inception Plugin Team.
 * License: CC BY-SA 3.0
 *      Inception by Inception Plugin Team is licensed under a
 *      Creative Commons Attribution-ShareAlike 3.0 Unported
 *      License.
 */
package de.RealityBends.Inception.World;

import de.RealityBends.Inception.World.OverlapEvents;
import de.RealityBends.Inception.World.Handler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.plugin.EventExecutor;

public class OverlapEventExecutor
        implements EventExecutor {

    private World oWorld;
    private OverlapEvents oOverlapEvent;
    private short sBoundsBelow, sBoundsAbove;
    
    // Reflection
    private Class cOverlapEventClass;
    private Method rmWorldHandler_onOverlapEvent;

    public OverlapEventExecutor(World oWorld, OverlapEvents oOverlapEvent, short sBoundsAbove, short sBoundsBelow) {
        this.oWorld = oWorld;
        this.oOverlapEvent = oOverlapEvent;
        this.sBoundsAbove = sBoundsAbove;
        this.sBoundsBelow = sBoundsBelow;
        
        // Reflection
        this.cOverlapEventClass = oOverlapEvent.getEventClass();
        try {
            this.rmWorldHandler_onOverlapEvent = Handler.class.getMethod("onOverlapEvent", cOverlapEventClass);
        } catch (Throwable ex) {
            Logger.getLogger(OverlapEventExecutor.class.getName()).log(Level.SEVERE, "Event is not handled by WorldHandler! Open a ticket immediately!", ex);
        }
        
    }

    @Override
    public void execute(Listener listener, Event event)
            throws EventException {
        try {
            // Check if this Executor should even consider this event.
            if (cOverlapEventClass.isInstance(event)) {
                // Check if the event is cancellable, and if it is, if it has been cancelled.
                boolean tIsCancellable = Cancellable.class.isInstance(event);
                if (!tIsCancellable || (tIsCancellable && !Cancellable.class.cast(event).isCancelled())) {
                    // Check if the event was raised in the world we were assigned to.
                    BlockEvent oBlockEvent = (BlockEvent) cOverlapEventClass.cast(event);
                    Block oBlock = oBlockEvent.getBlock();
                    Location oLocation = oBlock.getLocation();
                    if (oBlock.getWorld() == oWorld && (oLocation.getBlockY() < sBoundsBelow || oLocation.getBlockY() > sBoundsAbove)) {
                        rmWorldHandler_onOverlapEvent.invoke(listener, event);
                    }
                }
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new EventException(ex);
        }
    }

}