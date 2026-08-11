package engineer.skyouo.plugins.naturerevive.spigot.managers.features;

import engineer.skyouo.plugins.naturerevive.spigot.managers.ChunkRegeneration;
import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin.nmsWrapper;

public class StructureRegeneration {
    public static void savingMovableStructure(Chunk chunk, ChunkSnapshot oldChunkSnapshot) {
        if (NatureRevivePlugin.readonlyConfig.inPlaceRestoreEndPodium
                && chunk.getWorld().getEnvironment().equals(World.Environment.THE_END) && isInSpecialChunks(chunk))
            return;

        Map<Location, BlockData> perversedBlocks = new HashMap<>();
        for (int x = 0; x < 16; x++) {
            for (int y = nmsWrapper.getWorldMinHeight(chunk.getWorld()); y < chunk.getWorld().getMaxHeight(); y++) {
                for (int z = 0; z < 16; z++) {
                    Material blockType = oldChunkSnapshot.getBlockType(x, y, z);
                    Location originLocation = new Location(chunk.getWorld(), (chunk.getX() << 4) + x, y, (chunk.getZ() << 4) + z);

                    if ((blockType.equals(Material.END_PORTAL) || blockType.equals(Material.END_GATEWAY)) && !perversedBlocks.containsKey(originLocation)) {

                        for (int i = -2; i <= 2; i++)
                            for (int j = -2; j <= 2; j++)
                                for (int k = -2; k <= 2; k++) {
                                    Location neighborLocation = originLocation.clone().add(i, j, k);
                                    if (isNotInTheChunk(chunk, neighborLocation)) continue;

                                    int[] xyz = convertLocationToInChunkXYZ(neighborLocation);

                                    Material targetType = oldChunkSnapshot.getBlockType(xyz[0], xyz[1], xyz[2]);

                                    if (targetType.equals(Material.END_PORTAL) || targetType.equals(Material.END_GATEWAY) || targetType.equals(Material.BEDROCK))
                                        perversedBlocks.put(neighborLocation, oldChunkSnapshot.getBlockData(xyz[0], xyz[1], xyz[2]));
                                }
                    }/* else if (blockType.equals(Material.NETHER_PORTAL) && !perversedBlocks.containsKey(originLocation)) {
                        for (int i = -1; i <= 1; i++)
                            for (int j = -1; j <= 1; j++)
                                for (int k = -1; k <= 1; k++) {
                                    Location neighborLocation = originLocation.clone().add(i, j, k);
                                    if (isNotInTheChunk(chunk, neighborLocation)) continue;

                                    int[] xyz = convertLocationToInChunkXYZ(neighborLocation);

                                    Material targetType = oldChunkSnapshot.getBlockType(xyz[0], xyz[1], xyz[2]);

                                    if (targetType.equals(Material.NETHER_PORTAL) || targetType.equals(Material.OBSIDIAN))
                                        perversedBlocks.put(neighborLocation, oldChunkSnapshot.getBlockData(xyz[0], xyz[1], xyz[2]));
                                }
                    }*/ else if (blockType.equals(Material.BEDROCK)) {
                        if (!chunk.getWorld().getEnvironment().equals(World.Environment.THE_END))
                            continue;

                        if (isInSpecialChunks(chunk)) {
                            perversedBlocks.put(originLocation, oldChunkSnapshot.getBlockData(x, y, z));

                            Location neighborLocation = originLocation.clone().add(0, 1, 0);
                            perversedBlocks.put(neighborLocation, oldChunkSnapshot.getBlockData(x, y + 1, z));
                            continue;
                        }

                        for (int i = -2; i <= 2; i++)
                            for (int j = -2; j <= 2; j++)
                                for (int k = -2; k <= 2; k++) {
                                    Location neighborLocation = originLocation.clone().add(i, j, k);
                                    Material neighborBlockType = neighborLocation.getBlock().getType();

                                    if (perversedBlocks.containsKey(neighborLocation) || (neighborBlockType.equals(Material.END_GATEWAY)) || neighborBlockType.equals(Material.END_PORTAL)) {
                                        perversedBlocks.put(originLocation, oldChunkSnapshot.getBlockData(x, y, z));
                                        break;
                                    }
                                }
                    } else if (blockType.equals(Material.OBSIDIAN)) {
                        for (int i = -1; i <= 1; i++)
                            for (int j = -1; j <= 1; j++)
                                for (int k = -1; k <= 1; k++) {
                                    Location neighborLocation = originLocation.clone().add(i, j, k);
                                    if (isNotInTheChunk(chunk, neighborLocation)) continue;

                                    if (perversedBlocks.containsKey(neighborLocation)) {
                                        perversedBlocks.put(originLocation, oldChunkSnapshot.getBlockData(x, y, z));
                                        break;
                                    }
                                }
                    } else if (blockType.equals(Material.DRAGON_EGG)) {
                        Location neighborLocation = originLocation.clone().add(0, -1, 0);
                        if (perversedBlocks.containsKey(neighborLocation)) {
                            perversedBlocks.put(originLocation, oldChunkSnapshot.getBlockData(x, y, z));
                        }
                    } else if (blockType.equals(Material.WALL_TORCH)) {
                        if (!chunk.getWorld().getEnvironment().equals(World.Environment.THE_END))
                            continue;

                        if (isInSpecialChunks(chunk)) {
                            perversedBlocks.put(originLocation, oldChunkSnapshot.getBlockData(x, y, z));
                            continue;
                        }

                        for (int i = -1; i <= 1; i++)
                            for (int k = -1; k <= 1; k++) {
                                Location neighborLocation = originLocation.clone().add(i, 0, k);
                                if (isNotInTheChunk(chunk, neighborLocation)) continue;

                                if (perversedBlocks.containsKey(neighborLocation)) {
                                    perversedBlocks.put(originLocation, oldChunkSnapshot.getBlockData(x, y, z));
                                    break;
                                }
                            }
                    }
                }
            }
        }

        ChunkRegeneration.setBlocksSynchronous(perversedBlocks, Collections.EMPTY_LIST);
    }

    private static boolean isNotInTheChunk(Chunk chunk, Location location) {
        return location.getChunk().getX() != chunk.getX() || location.getChunk().getZ() != chunk.getZ();
    }

    // The method is hardcoded to detect the end gateway.
    private static boolean isInSpecialChunks(Chunk chunk) {
        return (chunk.getX() == 0 || chunk.getX() == -1) && (chunk.getZ() == 0 || chunk.getZ() == -1);
    }

    private static int[] convertLocationToInChunkXYZ(Location location) {
        return new int[] { (location.getBlockX() - ((location.getBlockX() >> 4) << 4)), location.getBlockY(), (location.getBlockZ() - ((location.getBlockZ() >> 4) << 4)) };
    };
}
