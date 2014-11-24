/* Authors: Xaymar
 * Copyright: 2012-2013 (c) Inception Plugin Team.
 * License: CC BY-SA 3.0
 *      Inception by Inception Plugin Team is licensed under a
 *      Creative Commons Attribution-ShareAlike 3.0 Unported
 *      License.
 */
package de.RealityBends.Inception;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.TypeConstraintException;
import net.minecraft.server.v1_6_R2.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_6_R2.CraftServer;
import org.bukkit.craftbukkit.v1_6_R2.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.craftbukkit.v1_6_R2.entity.CraftEntity;
import org.bukkit.entity.EntityType;

public class Utility {
    // <editor-fold defaultstate="collapsed" desc="Array Operations">

    public static String[] ArraySplit(String split, String delimiter) {
        return split.split(delimiter);
    }

    public static String ArrayCombine(String[] array, String delimiter) {
        String output = "";
        for (String word : array) {
            output += (output.isEmpty() ? "" : delimiter) + word;
        }
        return output;
    }
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="String Operations">

    public static String[] SmartSplit(String[] args) {
        return SmartSplit(ArrayCombine(args, " "));
    }

    public static String[] SmartSplit(String text) {
        ArrayList<String> list = new ArrayList<>();
        Matcher match = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'").matcher(text);
        while (match.find()) {
            list.add(match.group(1) != null ? match.group(1) : match.group(2) != null ? match.group(2) : match.group());
        }
        return list.toArray(new String[list.size()]);
    }

    public static String[] ReparseArguments(String[] args) {
        return SmartSplit(args);
    }

    public static String Substitude(String On, String[] What, String[] With) {
        if (What.length != With.length) {
            throw new java.lang.ArrayIndexOutOfBoundsException();
        }

        String Out = On;

        for (int count = 0; count < What.length; count++) {
            if (What[count].contains(",")) {
                String[] WhatArgs = What[count].split(",");
                for (String arg : WhatArgs) {
                    Out = Out.replaceAll(arg, With[count]);
                }
            } else {
                Out = Out.replaceAll(What[count], With[count]);
            }
        }

        return Out;
    }

    public static String Colorize(String On) {
        return ChatColor.translateAlternateColorCodes("&".charAt(0), On);
    }
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Player Operations">

    public static <T> CommandSender SenderFromName(T player) {
        if (player instanceof CommandSender) {
            return (CommandSender) player;
        } else if (player instanceof Player) {
            return (Player) player;
        } else if (player instanceof String) {
            return Bukkit.getPlayerExact((String) player);
        } else {
            throw new TypeConstraintException("'player' must be CommandSender, Player or String");
        }
    }

    public static <T> void SendMessage(final T reciever, final String msg, final Object... args) {
        SendMessage(true, reciever, msg, args);
    }

    public static <T> void SendMessage(final boolean prefix, final T reciever, final String msg, final Object... args) {
        SendMessage(true, "[Inception] ", reciever, msg, args);
    }

    public static <T> void SendMessage(final boolean addprefix, final String prefix, final T reciever, final String msg, final Object... args) {
        if (reciever != null) {
            if (reciever instanceof List) {
                for (Object entry : (List<?>) reciever) {
                    SendMessage(addprefix, prefix, entry, msg, args);
                }
            } else {
                for (String line : String.format(msg, args).split("\n")) {
                    Utility.SenderFromName(reciever).sendMessage(Utility.Colorize(
                            (addprefix ? prefix + " " + line : line)));
                }
            }
        }
    }
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Entity Operations">

    public static void EntityTeleportEx(final Entity poEntity, final Location poLocation, final Plugin poPlugin) {
        final net.minecraft.server.v1_6_R2.Entity oMCEntity = ((CraftEntity) poEntity).getHandle();
        
        // Check if we are teleporting a player, since they need special handling.
        if (poEntity.getType() == EntityType.PLAYER) { //Players need a packet for teleportation first.
            poEntity.teleport(poLocation);
        } else {
            WorldServer oWorldSource = (WorldServer) oMCEntity.world;
            WorldServer oWorldTarget = ((CraftWorld) poLocation.getWorld()).getHandle();
            
            // Change world that has this entity
            oWorldSource.removeEntity(oMCEntity);
            oWorldTarget.addEntity(oMCEntity);
            
            // Check if the entity is actually tracked in the other world.
            if (oWorldTarget.getTracker().trackedEntities.b(oMCEntity.id) == false) {
                oWorldSource.getTracker().untrackEntity(oMCEntity);
                oWorldTarget.getTracker().track(oMCEntity);
            }
            
            // Update the Entity
            oMCEntity.world = ((CraftWorld) poLocation.getWorld()).getHandle();
            oMCEntity.setLocation(poLocation.getX(), poLocation.getY(), poLocation.getZ(), poLocation.getYaw(), poLocation.getPitch());

            //((CraftEntity) poEntity).getHandle().teleportTo(poLocation, false);
        }

        //Teleport the passenger.
        if (poEntity.getPassenger() != null) { //The teleporting entity is being driven.
            
            final Entity oPassenger = poEntity.getPassenger();

            poEntity.eject(); //Eject the passenger.
            oPassenger.leaveVehicle(); //Leave the vehicle.

            //Delay teleporting of passenger by one tick each.
            Bukkit.getScheduler().scheduleSyncDelayedTask(poPlugin, new Runnable() {
                @Override
                public void run() {
                    Utility.EntityTeleportEx(oPassenger, poLocation, poPlugin);

                    poEntity.setPassenger(oPassenger);
                    oMCEntity.passenger = ((CraftEntity) oPassenger).getHandle();
                    ((CraftEntity) oPassenger).getHandle().vehicle = oMCEntity;
                }

            }, 1);
        }
    }
    // </editor-fold>
    //<editor-fold defaultstate="collapsed" desc="SQL Operations">

    public static String escapeForSQL(String pstInput) {
        return pstInput.replaceAll("\\(\\[^a-zA-Z_\\-0-9\\]\\)", "\\$1");
    }
    //</editor-fold>
}
