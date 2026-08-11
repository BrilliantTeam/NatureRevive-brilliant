package engineer.skyouo.plugins.naturerevive.spigot.managers.features;

import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import engineer.skyouo.plugins.naturerevive.spigot.util.ScheduleUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.boss.DragonBattle;

public final class EndPodiumRegeneration {
    private EndPodiumRegeneration() {
    }

    public static void restoreIfAffected(World world, int chunkX, int chunkZ) {
        if (world.getEnvironment() != World.Environment.THE_END || !isPodiumChunk(chunkX, chunkZ)) return;

        ScheduleUtil.REGION.runTask(NatureRevivePlugin.instance, new Location(world, 0, 64, 0),
                () -> restore(world));
    }

    private static boolean isPodiumChunk(int chunkX, int chunkZ) {
        return chunkX >= -1 && chunkX <= 0 && chunkZ >= -1 && chunkZ <= 0;
    }

    private static void restore(World world) {
        DragonBattle battle = world.getEnderDragonBattle();
        if (battle == null) return;

        boolean active = battle.hasBeenPreviouslyKilled();
        if (battle.generateEndPortal(active)) return;

        Location origin = battle.getEndPortalLocation();
        if (origin != null) placePodium(world, origin.getBlockX(), origin.getBlockY(), origin.getBlockZ(), active);
    }

    private static void placePodium(World world, int originX, int originY, int originZ, boolean active) {
        for (int x = originX - 4; x <= originX + 4; x++) {
            for (int y = originY - 1; y <= originY + 32; y++) {
                for (int z = originZ - 4; z <= originZ + 4; z++) {
                    int dx = x - originX;
                    int dz = z - originZ;
                    int distanceSquared = dx * dx + dz * dz;
                    boolean insidePortal = distanceSquared < 2.5 * 2.5;
                    if (!insidePortal && distanceSquared >= 3.5 * 3.5) continue;

                    Block block = world.getBlockAt(x, y, z);
                    if (y < originY) set(block, insidePortal ? Material.BEDROCK : Material.END_STONE);
                    else if (y > originY) set(block, Material.AIR);
                    else if (!insidePortal) set(block, Material.BEDROCK);
                    else set(block, active ? Material.END_PORTAL : Material.AIR);
                }
            }
        }

        for (int y = 0; y < 4; y++) set(world.getBlockAt(originX, originY + y, originZ), Material.BEDROCK);
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Block torch = world.getBlockAt(originX + face.getModX(), originY + 2, originZ + face.getModZ());
            torch.setType(Material.WALL_TORCH, false);
            Directional data = (Directional) torch.getBlockData();
            data.setFacing(face);
            torch.setBlockData(data, false);
        }
    }

    private static void set(Block block, Material material) {
        if (block.getType() != material) block.setType(material, false);
    }
}
