/* Authors: Xaymar
 * Copyright: 2012-2013 (c) Inception Plugin Team.
 * License: CC BY-SA 3.0
 *      Inception by Inception Plugin Team is licensed under a
 *      Creative Commons Attribution-ShareAlike 3.0 Unported
 *      License.
 */
package de.RealityBends.Inception.World;

import org.bukkit.Material;

public enum OverlapDelayBlocks {
    Torch(Material.TORCH.getId()),
    Ladder(Material.LADDER.getId()),
    RedstoneTorch(Material.REDSTONE_TORCH_ON.getId()),
    RedstoneRepeater(Material.DIODE_BLOCK_OFF.getId()),
    RedstoneComparator(Material.REDSTONE_COMPARATOR_OFF.getId()),
    Lever(Material.LEVER.getId()),
    WoodButton(Material.WOOD_BUTTON.getId()),
    StoneButton(Material.STONE_BUTTON.getId()),
    Dispenser(Material.DISPENSER.getId()),
    Dropper(Material.DROPPER.getId()),
    Hopper(Material.HOPPER.getId()),
    Piston(Material.PISTON_BASE.getId()),
    StickyPiston(Material.PISTON_STICKY_BASE.getId()),
    Furnace(Material.FURNACE.getId()),
    Chest(Material.CHEST.getId()),
    TrappedChest(Material.TRAPPED_CHEST.getId()),
    EnderChest(Material.ENDER_CHEST.getId()),
    StoneSlab(Material.STEP.getId()),
    WoodedSlab(Material.WOOD_STEP.getId()),
    OakWoodStairs(Material.WOOD_STAIRS.getId()),
    CobblestoneStairs(Material.COBBLESTONE_STAIRS.getId()),
    BrickStairs(Material.BRICK_STAIRS.getId()),
    StoneBrickStairs(Material.SMOOTH_STAIRS.getId()),
    NetherBrickStairs(Material.NETHER_BRICK_STAIRS.getId()),
    SandstoneStairs(Material.SANDSTONE_STAIRS.getId()),
    SpruceWoodStairs(Material.SPRUCE_WOOD_STAIRS.getId()),
    JungleWoodStairs(Material.JUNGLE_WOOD_STAIRS.getId()),
    QuartzStairs(Material.QUARTZ_STAIRS.getId()),
    Vines(Material.VINE.getId()),
    TripwireHook(Material.TRIPWIRE_HOOK.getId()),
    WallSign(Material.WALL_SIGN.getId()),
    SignPost(Material.SIGN_POST.getId()),
    Skull(Material.SKULL.getId()),
    Pumpkin(Material.PUMPKIN.getId()),
    JackOLantern(Material.JACK_O_LANTERN.getId()),
    HugeBrownMushroom(Material.HUGE_MUSHROOM_1.getId()),
    HugeRedMushroom(Material.HUGE_MUSHROOM_2.getId()),
    WoodenDoor(Material.WOODEN_DOOR.getId()),
    IronDoor(Material.IRON_DOOR_BLOCK.getId()),
    TrapDoor(Material.TRAP_DOOR.getId()),
    FenceGate(Material.FENCE_GATE.getId()),
    Jukebox(Material.JUKEBOX.getId());
    
    private int sTypeId;
    
    private OverlapDelayBlocks(int sTypeId) {
        this.sTypeId = sTypeId;
    }
    
    public int getTypeId() {
        return sTypeId;
    }
}
