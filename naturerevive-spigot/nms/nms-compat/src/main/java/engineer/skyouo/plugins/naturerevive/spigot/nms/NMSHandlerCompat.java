package engineer.skyouo.plugins.naturerevive.spigot.nms;

import engineer.skyouo.plugins.naturerevive.common.INMSWrapper;
import engineer.skyouo.plugins.naturerevive.common.VersionUtil;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class NMSHandlerCompat implements INMSWrapper {
    public static List<Material> materials = new ArrayList<>();
    static {
        for (Material item : Material.values()) {
            if (item.name().endsWith("ORE")) {
                if (item.name().startsWith("DEEPSLATE")) {
                    if (VersionUtil.isAtLeast(1, 17, 0)) materials.add(item);
                } else materials.add(item);
            } else {
                if (item.name().equals("ANCIENT_DEBRIS")) {
                    if (VersionUtil.isAtLeast(1, 16, 0)) materials.add(item);
                }
            }
        }
    }

    @Override
    public List<String> getCompatibleNMSVersion() {
        return List.of("*");
    }

    @Override
    public String getNbtAsString(World world, BlockState blockState) {
        try {
            Class<?> craftWorld = CompatUtil.getClassFromName(CompatUtil.getCraftBukkitClassName("CraftWorld"));
            Class<?> blockPos = CompatUtil.getClassFromName("net.minecraft.core.BlockPos");

            Object blockPosInst = CompatUtil.invokeConstructor(blockPos, new Class[]{ int.class, int.class, int.class },
                    new Object[]{ blockState.getX(), blockState.getY(), blockState.getZ() });
            Object cbWorldInst = CompatUtil.invokeFunction(world, craftWorld, "getHandle");

            Object blockEntity = CompatUtil.invokeFunction(cbWorldInst, "getBlockEntity", new Class<?>[]{ blockPos }, new Object[] { blockPosInst });
            Object nbtTagData = CompatUtil.invokeFunction(blockEntity, "saveWithFullMetadata");
            Object nbtString = CompatUtil.invokeFunction(nbtTagData, "getAsString");

            return (String) nbtString;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public void setBlockNMS(World world, int x, int y, int z, BlockData data) {
        try {
            Class<?> craftWorld = CompatUtil.getClassFromName(CompatUtil.getCraftBukkitClassName("CraftWorld"));
            Class<?> blockPos = CompatUtil.getClassFromName("net.minecraft.core.BlockPos");
            Class<?> craftBlockData = CompatUtil.getClassFromName(CompatUtil.getCraftBukkitClassName("block.data.CraftBlockData"));
            Class<?> blockState = CompatUtil.getClassFromName("net.minecraft.world.level.block.state.BlockState");

            Object blockPosInst = CompatUtil.invokeConstructor(blockPos, new Class[]{ int.class, int.class, int.class },
                    new Object[]{ x, y, z });
            Object craftData = CompatUtil.invokeFunction(data, craftBlockData, "getState");

            Object cbWorldInst = CompatUtil.invokeFunction(world, craftWorld, "getHandle");
            CompatUtil.invokeFunction(cbWorldInst, craftWorld, "setBlock",
                    new Class[]{ blockPos, blockState, int.class }, new Object[]{
                            blockPosInst, craftData, 3
                    });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void loadTileEntity(World world, int x, int y, int z, String nbt) {
        try {
            Class<?> craftWorld = CompatUtil.getClassFromName(CompatUtil.getCraftBukkitClassName("CraftWorld"));
            Class<?> blockPos = CompatUtil.getClassFromName("net.minecraft.core.BlockPos");
            Class<?> tagParser = CompatUtil.getClassFromName("net.minecraft.nbt.TagParser");
            Class<?> compoundTag = CompatUtil.getClassFromName("net.minecraft.nbt.CompoundTag");

            Object blockPosInst = CompatUtil.invokeConstructor(blockPos, new Class[]{ int.class, int.class, int.class },
                    new Object[]{ x, y, z });
            Object cbWorldInst = CompatUtil.invokeFunction(world, craftWorld, "getHandle");

            Object blockEntity = CompatUtil.invokeFunction(cbWorldInst, "getBlockEntity", new Class<?>[]{ blockPos }, new Object[] { blockPosInst });
            Object tag = CompatUtil.invokeFunction(null, tagParser, "parseTag", new Class[]{ String.class }, new Object[]{ Object.class });

            CompatUtil.invokeFunction(blockEntity, "load", new Class[]{ compoundTag }, new Object[]{ tag });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void createTileEntity(World world, int x, int y, int z, BlockData data, String nbt) {
        setBlockNMS(world, x, y, z, data);
        loadTileEntity(world, x, y, z, nbt);
    }

    @Override
    public double[] getRecentTps() {
        try {
            Class<?> mcServer = CompatUtil.getClassFromName("net.minecraft.server.MinecraftServer");
            Object server = CompatUtil.invokeFunction(null, mcServer, "getServer");

            return (double[]) CompatUtil.getParameter(server, "recentTps");
        } catch (Exception ex) {
            ex.printStackTrace();
            return new double[]{ 20, 20, 20 };
        }
    }

    @Override
    public double getLuckForPlayer(Player player) {
        try {
            Class<?> craftPlayer = CompatUtil.getClassFromName(CompatUtil.getCraftBukkitClassName("entity.CraftPlayer"));
            Object nmsPlayer = CompatUtil.invokeFunction(player, craftPlayer, "getHandle");

            return (double) CompatUtil.invokeFunction(nmsPlayer, "getLuck");
        } catch (Exception ex) {
            ex.printStackTrace();
            return 1.0;
        }
    }

    @Override
    public BlockState convertBlockDataToBlockState(BlockData blockData) {
        try {
            Class<?> craftBlockStates = CompatUtil.getClassFromName(CompatUtil.getCraftBukkitClassName("block.CraftBlockStates"));
            Class<?> compoundTag = CompatUtil.getClassFromName("net.minecraft.nbt.CompoundTag");
            Class<?> craftBlockData = CompatUtil.getClassFromName(CompatUtil.getCraftBukkitClassName("block.data.CraftBlockData"));
            Class<?> blockState = CompatUtil.getClassFromName("net.minecraft.world.level.block.state.BlockState");

            Object craftData = CompatUtil.invokeFunction(blockData, craftBlockData, "getState");
            Object result = CompatUtil.invokeFunction(null, craftBlockStates, "getBlockState",
                    new Class[]{ blockState, compoundTag }, new Object[]{ craftData, null });

            return (BlockState) result;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public int getWorldMinHeight(World world) {
        try {
            Class<?> craftWorld = CompatUtil.getClassFromName(CompatUtil.getCraftBukkitClassName("CraftWorld"));
            Class<?> serverLevel = CompatUtil.getClassFromName("net.minecraft.world.level.LevelReader");

            Object cbWorldInst = CompatUtil.invokeFunction(world, craftWorld, "getHandle");

            return (int) CompatUtil.invokeFunction(cbWorldInst, serverLevel, "getMinBuildHeight");
        } catch (Exception ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    @Override
    public Material[] getOreBlocks() {
        return materials.toArray(new Material[] {});
    }
}