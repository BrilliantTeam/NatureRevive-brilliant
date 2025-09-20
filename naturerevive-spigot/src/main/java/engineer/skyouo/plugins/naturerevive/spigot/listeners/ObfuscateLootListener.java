package engineer.skyouo.plugins.naturerevive.spigot.listeners;

import com.google.common.collect.Lists;
import engineer.skyouo.plugins.naturerevive.spigot.NatureReviveComponentLogger;
import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import engineer.skyouo.plugins.naturerevive.spigot.constants.OreBlocksCompat;
import engineer.skyouo.plugins.naturerevive.spigot.events.LootChestRegenEvent;
import it.unimi.dsi.fastutil.objects.ObjectDoubleImmutablePair;
import it.unimi.dsi.fastutil.objects.ObjectDoublePair;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTables;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

import static engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin.nmsWrapper;

public class ObfuscateLootListener implements Listener {
    private static final Random secureRandom = new SecureRandom();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractionEvent(PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;

        Block clickedBlock = event.getClickedBlock();

        if (clickedBlock.getType().equals(Material.CHEST) || clickedBlock.getType().equals(Material.CHEST_MINECART) || clickedBlock.getType().equals(Material.TRAPPED_CHEST)) {
            Chest craftChest = (Chest) event.getClickedBlock().getState();
            if (craftChest.getLootTable() == null) {
                return;
            }

            if (craftChest.getLootTable() != null) {
                NatureReviveComponentLogger.debug("Lootable Chest re-seeded as %s.", TextColor.fromHexString("#AAAAAA"),
                        craftChest.getLootTable());

                craftChest.setSeed(secureRandom.nextLong());

                if (!NatureRevivePlugin.readonlyConfig.adaptiveLootChestReplacement) {
                    craftChest.getBlockInventory().clear();

                    craftChest.getLootTable().fillInventory(craftChest.getBlockInventory(), secureRandom,
                            new LootContext.Builder(craftChest.getLocation())
                                    .luck(
                                            (float) NatureRevivePlugin.nmsWrapper.getLuckForPlayer(event.getPlayer())
                                    )
                                    .killer(
                                            event.getPlayer()
                                    ).build()
                    );
                }

                ChunkRelatedEventListener.flagChunk(event.getClickedBlock().getLocation());

                NatureReviveComponentLogger.debug("&7Lootable Chest reseeded, seed = %d.",
                        TextColor.fromHexString("#AAAAAA"), craftChest.getSeed());

                Bukkit.getServer().getPluginManager().callEvent(new LootChestRegenEvent(
                        event.getPlayer(), event.getClickedBlock().getLocation(), LocalDateTime.now()
                ));
            }
        }
    }

    // This method should be called after new chunk generated to obfuscate the chunk.
    /**
     * @deprecated - Will refactor into "IRegenOreEngineIntegration" in future version.
     * @param chunk
     */
    public static void randomizeChunkOre(Chunk chunk) {
        if (chunk.getWorld().getEnvironment().equals(World.Environment.THE_END)) return; // Does not need since no ore
        Location location = new Location(chunk.getWorld(), chunk.getX() << 4, 256, chunk.getZ() << 4);
        List<ObjectDoublePair<Location>> pairList = new ArrayList<>();
        List<Block> oreList = new ArrayList<>();

        if (chunk.getWorld().getEnvironment().equals(World.Environment.NETHER)) {
            int surface = findSurface(location);
            if (surface < 140) surface = 140;

            for (int x = 0; x < 16; x++) {
                for (int y = nmsWrapper.getWorldMinHeight(chunk.getWorld()) + 1; y <= surface; y++) {
                    for (int z = 0; z < 16; z++) {
                        Block block = chunk.getBlock(x, y, z);
                        if (OreBlocksCompat.contains(block.getType())) {
                            oreList.add(block);
                        }
                    }
                }

                for (int y = chunk.getWorld().getMaxHeight() - 1; y >= surface; y--) {
                    calculateExpectation(chunk, pairList, oreList, x, y);
                }
            }
        } else {
            int surface = findSurface(location);
            if (NatureRevivePlugin.readonlyConfig.saferOreObfuscation) surface = 40;

            for (int x = 0; x < 16; x++) {
                for (int y = nmsWrapper.getWorldMinHeight(chunk.getWorld()); y <= surface; y++) {
                    calculateExpectation(chunk, pairList, oreList, x, y);
                }
            }
        }

        pairList.sort(Comparator.comparingDouble(ObjectDoublePair::secondDouble));
        Lists.reverse(pairList);

        for (int i = 0; i < oreList.size(); i++) {
            if ((i + 1) > pairList.size()) {
                NatureReviveComponentLogger.debug(
                        "&7Cannot fully obfuscate ores at chunk[x = %d, z = %d, world = %s] (ores count: %d, replaced count: %d)!",
                        TextColor.fromHexString("#AAAAAA"),
                        chunk.getX(), chunk.getZ(), chunk.getWorld().getName(), oreList.size(), pairList.size()
                );
                break;
            }

            Location loc = pairList.get(i).first();

            Block ore = oreList.get(i);
            Block replaced = chunk.getWorld().getBlockAt(loc);

            // BlockData blockData = replaced.getBlockData().clone();

            BlockData target = chunk.getWorld().getEnvironment().equals(World.Environment.NETHER) ? Material.NETHERRACK.createBlockData() :
                    ore.getY() >= 0 ?
                    Material.STONE.createBlockData() :
                    Material.DEEPSLATE.createBlockData();

            NatureRevivePlugin.nmsWrapper.setBlockNMS(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), ore.getBlockData());
            NatureRevivePlugin.nmsWrapper.setBlockNMS(ore.getWorld(), ore.getX(), ore.getY(), ore.getZ(), target);

            NatureReviveComponentLogger.debug("Swap %d,%d,%d (%s) to %d,%d,%d (%s)",
                    TextColor.fromHexString("#AAAAAA"),
                    ore.getX(), ore.getY(), ore.getZ(), ore.getType(),
                    replaced.getX(), replaced.getY(), replaced.getZ(), target.getMaterial());
        }

        oreList.clear();
        pairList.clear();
    }

    private static void calculateExpectation(Chunk chunk, List<ObjectDoublePair<Location>> pairList, List<Block> oreList, int x, int y) {
        for (int z = 0; z < 16; z++) {
            Block block = chunk.getBlock(x, y, z);
            if (block.getType().equals(Material.AIR) || block.getType().equals(Material.WATER) || block.getType().equals(Material.LAVA))
                continue;

            if (block.getType().equals(Material.BEDROCK) || block.getType().equals(Material.END_PORTAL))
                continue;

            if (NatureRevivePlugin.readonlyConfig.saferOreObfuscation && (
                    (
                            chunk.getWorld().getEnvironment().equals(World.Environment.NORMAL) &&
                                    !block.getType().equals(Material.STONE) &&
                                    !block.getType().equals(OreBlocksCompat.getSpecialMaterial("DEEPSLATE"))
                    ) || (
                            chunk.getWorld().getEnvironment().equals(World.Environment.NETHER) &&
                                    !block.getType().equals(Material.NETHERRACK)
                    )
            ))
                continue;

            if (OreBlocksCompat.contains(block.getType()) && !oreList.contains(block)) {
                oreList.add(block);
            } else {
                // double expectation = perlinNoise.noise((chunk.getX() << 4) + x, y, (chunk.getZ() << 4) + z);

                if (getIfRelativeIs(block, Material.AIR)) continue;

                pairList.add(
                        new ObjectDoubleImmutablePair<>(
                                new Location(chunk.getWorld(), (chunk.getX() << 4) + x, y, (chunk.getZ() << 4) + z),
                                secureRandom.nextDouble() * 10
                        )
                );
            }
        }
    }

    // This method is not guaranteed to work in THE_END or THE_NETHER
    private static int findSurface(Location top) {
        Location result = top.clone();

        if (!top.getWorld().getEnvironment().equals(World.Environment.NETHER)) {
            int minHeight = nmsWrapper.getWorldMinHeight(top.getWorld());

            while (!result.getBlock().getType().equals(Material.STONE) && !result.getBlock().getType().equals(OreBlocksCompat.getSpecialMaterial("DEEPSLATE")) &&
                    result.getBlockY() > minHeight) {
                result.add(0, -1, 0);
            }
        } else {
            top.setY(80);

            int maxHeight = top.getWorld().getMaxHeight();

            while (!result.getBlock().getType().equals(Material.NETHERRACK) && !result.getBlock().getType().equals(Material.BEDROCK) &&
                    result.getBlockY() < maxHeight) {
                result.add(0, 1, 0);
            }
        }

        return result.getBlockY();
    }

    private static final BlockFace[] blockFaces = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};

    private static boolean getIfRelativeIs(Block block, Material target) {
        for (BlockFace face : blockFaces) {
            if (block.getRelative(face).getType() == target) return true;
        }

        return false;
    }
}
