package engineer.skyouo.plugins.naturerevive.spigot.managers.features;

import engineer.skyouo.plugins.naturerevive.spigot.NatureReviveComponentLogger;
import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import engineer.skyouo.plugins.naturerevive.spigot.events.ElytraPlacementEvent;
import engineer.skyouo.plugins.naturerevive.spigot.integration.IntegrationUtil;
import engineer.skyouo.plugins.naturerevive.spigot.integration.land.ILandPluginIntegration;
import engineer.skyouo.plugins.naturerevive.spigot.managers.FaweImplRegeneration;
import engineer.skyouo.plugins.naturerevive.spigot.structs.BlockDataChangeWithPos;
import engineer.skyouo.plugins.naturerevive.spigot.structs.BukkitPositionInfo;
import engineer.skyouo.plugins.naturerevive.spigot.util.ScheduleUtil;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin.instance;

public class ElytraRegeneration {
    private static int elyAmount = 0;

    public static boolean isEndShip(List<ILandPluginIntegration> integrations, Chunk chunk, ChunkSnapshot snapshot) {
        if (!chunk.getWorld().getEnvironment().equals(World.Environment.THE_END)) {
            return false;
        }

        Biome biome = chunk.getBlock(0, 0, 0).getBiome();
        if (biome == Biome.THE_END) {
            return false;
        }

        // todo - refactor bad naming style
        int c = 0;
        int p = 0;
        int pl = 0;
        int ps = 0;

        List<Location> l = new ArrayList<>();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 256; y++) {
                    Material block1 = snapshot.getBlockType(x, y, z);
                    if (block1 == Material.CHEST) {
                        c++;
                        l.add(getLocationFromBlockType(snapshot, x, y, z));
                    } else if (block1 == Material.PURPUR_BLOCK) {
                        p++;
                    } else if (block1 == Material.PURPUR_PILLAR) {
                        pl++;
                    } else if (block1 == Material.PURPUR_STAIRS) {
                        ps++;
                    }
                }
            }
        }

        // 可能為剛好被切割的船頭，開始進行鄰近區快判斷
        if (c == 1) {
            World world = chunk.getWorld();

            NatureReviveComponentLogger.debug("Possibilities of chunk (%d, %d) is end ship is detected, NatureRevive will perform checking.",
                    TextColor.fromHexString("#AAAAAA"),
                    chunk.getX(), chunk.getZ());

            Directional directional = (Directional) l.get(0).getBlock().getBlockData();
            BlockFace blockFace = directional.getFacing();

            List<Chunk> possibleShipChunks = new ArrayList<>();
            if (blockFace.equals(BlockFace.WEST) || blockFace.equals(BlockFace.EAST)) {
                // 面相X方位 相對位置為Z
                possibleShipChunks.add(world.getChunkAt(chunk.getX(), chunk.getZ() + 1));
                possibleShipChunks.add(world.getChunkAt(chunk.getX(), chunk.getZ() - 1));
            } else if (blockFace.equals(BlockFace.SOUTH) || blockFace.equals(BlockFace.NORTH)) {
                // 面相Z方位 相對位置為X
                possibleShipChunks.add(world.getChunkAt(chunk.getX() + 1, chunk.getZ()));
                possibleShipChunks.add(world.getChunkAt(chunk.getX() - 1, chunk.getZ()));
            }
            // 檢查要檢查的區塊是否包含領地
            // todo: Folia edging case - the nearby chunk is in different region
            for (Chunk chunk1 : possibleShipChunks) {
                if (!integrations.isEmpty() && integrations.stream().anyMatch(i -> i.checkHasLand(chunk1))) {
                    NatureReviveComponentLogger.debug("Nearby chunks at (%d, %d) contain lands which claimed by players, abort.",
                            TextColor.fromHexString("#AAAAAA"),
                            chunk1.getX(), chunk1.getZ());

                    return false;
                } else {
                    ScheduleUtil.REGION.runTask(instance, chunk1, () -> {
                        if (NatureRevivePlugin.readonlyConfig.regenerationEngine.equalsIgnoreCase("fawe")) {
                            World world1 = chunk1.getWorld();
                            int cx1 = chunk1.getX();
                            int cz1 = chunk1.getZ();
                            ScheduleUtil.GLOBAL.runTaskAsynchronously(instance, () ->
                                FaweImplRegeneration.regenerate(world1, cx1, cz1, true, () -> postOtherChunkCheck(chunk1, blockFace, l)));
                        } else {
                            chunk.getWorld().regenerateChunk(chunk1.getX(), chunk1.getZ());
                            postOtherChunkCheck(chunk1, blockFace, l);
                        }
                    });

                    return true;
                }
            }
            return false;
        }

        // 正常判斷法
        if (c != 2 || p < 14 || pl < 2 || ps < 2) {
            return false;
        }

        if (!checkChestNearbyLocation(l.get(0), l.get(1))) {
            return false;
        }

        // 放置鞘翅 (sync)
        if (elyAmount >= NatureRevivePlugin.readonlyConfig.maxElytraPerDay){
            NatureReviveComponentLogger.debug(
                    "Exceed the elytra regenerated limit (%d), will not regen any elytra until tomorrow.",
                    TextColor.fromHexString("#AAAAAA"), elyAmount
            );
            BukkitPositionInfo positionInfo = new BukkitPositionInfo(chunk.getBlock(0,0,0).getLocation(), System.currentTimeMillis() + NatureRevivePlugin.readonlyConfig.elytraExceedLimitOffsetDuration);
            NatureRevivePlugin.databaseConfig.set(positionInfo);
            return false;
        }

        Directional directional = (Directional) l.get(0).getBlock().getBlockData();
        regenerateElytraWithFlame(l.get(0), l.get(1), directional.getFacing());
        elyAmount++;
        return true;
    }

    private static void postOtherChunkCheck(Chunk chunk, BlockFace blockFace, List<Location> l) {
        ChunkSnapshot chunkSnapshot1 = chunk.getChunkSnapshot();
        int chest = 0;
        List<Location> chests = new ArrayList<>();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 256; y++) {
                    Material block1 = chunkSnapshot1.getBlockType(x, y, z);
                    if (block1 == Material.CHEST) {
                        chest++;
                        chests.add(getLocationFromBlockType(chunkSnapshot1, x, y, z));
                    }
                }
            }
        }
        if (chest != 1) {
            return;
        }

        if (!checkChestNearbyLocation(l.get(0), chests.get(0))) {
            return;
        }

        if (elyAmount >= NatureRevivePlugin.readonlyConfig.maxElytraPerDay){
            NatureReviveComponentLogger.debug(
                    "Exceed the elytra regenerated limit (%d), will not regen any elytra until tomorrow.",
                    TextColor.fromHexString("#AAAAAA"), elyAmount
            );
            BukkitPositionInfo positionInfo = new BukkitPositionInfo(chunk.getBlock(0,0,0).getLocation(), System.currentTimeMillis() + NatureRevivePlugin.readonlyConfig.elytraExceedLimitOffsetDuration);
            NatureRevivePlugin.databaseConfig.set(positionInfo);
            return;
        }

        regenerateElytraWithFlame(l.get(0), chests.get(0), blockFace);
        elyAmount++;
    }

    private static void regenerateElytraWithFlame(Location chest1, Location chest2, BlockFace elytraFace) {
        Location location = getNewFlameLoc(chest1, chest2).add(0, 1, 0);

        ScheduleUtil.REGION.runTask(instance, location, () -> {
            Block block = location.getBlock();
            for (Entity entity : block.getWorld().getNearbyEntities(block.getLocation(), 1, 1, 1)) {
                if (entity instanceof ItemFrame) {
                    ((ItemFrame) entity).setItem(new ItemStack(Material.AIR));
                    entity.remove();
                    break;
                }
            }
            location.getBlock().setType(Material.AIR);
            // coreProtectAPI.logRemoval(readonlyConfig.coreProtectUserName, location, Material.AIR, location.getBlock().getBlockData());
            ItemFrame itemFrame = location.getWorld().spawn(location, ItemFrame.class);
            itemFrame.setFacingDirection(elytraFace, true);
            itemFrame.setItem(new ItemStack(Material.ELYTRA));

            if (IntegrationUtil.hasValidLoggingIntegration())
                NatureRevivePlugin.blockDataChangeWithPos.add(new BlockDataChangeWithPos(location, block.getBlockData(), location.getBlock().getBlockData(), BlockDataChangeWithPos.Type.REPLACE));

            // coreProtectAPI.logPlacement(readonlyConfig.coreProtectUserName, location, Material.ITEM_FRAME, location.getBlock().getBlockData());

            NatureReviveComponentLogger.debug("Regenerated elytra located at [world = %s, x = %f, y = %f, z = %f].",
                    TextColor.fromHexString("#AAAAAA"),
                    location.getWorld().getName(), location.getX(), location.getY(), location.getZ());

            Bukkit.getServer().getPluginManager().callEvent(new ElytraPlacementEvent(location, LocalDateTime.now()));
        });
    }


    private static boolean checkChestNearbyLocation(Location loc1, Location loc2) {
        if (loc1.getY() != loc2.getY()) {
            return false;
        }

        if (loc1.distance(loc2) != 2.0) {
            return false;
        }

        Directional directional1 = (Directional) loc1.getBlock().getBlockData();
        Directional directional2 = (Directional) loc2.getBlock().getBlockData();
        return directional1.getFacing() == directional2.getFacing();
    }

    private static Location getLocationFromBlockType(ChunkSnapshot chunkSnapshot, int x, int y, int z) {
        World world = Bukkit.getWorld(chunkSnapshot.getWorldName());
        int worldX = (chunkSnapshot.getX() << 4) + x;
        int worldZ = (chunkSnapshot.getZ() << 4) + z;
        return new Location(world, worldX, y, worldZ);
    }

    private static Location getNewFlameLoc(Location loc1, Location loc2) {
        if (loc1.getX() == loc2.getX()) {
            double Z = (loc1.getZ() + loc2.getZ()) / 2;
            return new Location(loc1.getWorld(), loc1.getX(), loc1.getY(), Z);
        } else {
            double X = (loc1.getX() + loc2.getX()) / 2;
            return new Location(loc1.getWorld(), X, loc1.getY(), loc1.getZ());
        }
    }

    private static LocalDateTime dateTime = LocalDateTime.now();
    public static boolean checkResetLimitTime() {
        if (dateTime.getDayOfMonth() != LocalDateTime.now().getDayOfMonth()) {
            dateTime = LocalDateTime.now();
            elyAmount = 0;
            return true;
        }

        return false;
    }
}
