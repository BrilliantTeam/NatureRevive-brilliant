package engineer.skyouo.plugins.naturerevive.spigot.nms;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import engineer.skyouo.plugins.naturerevive.common.INMSWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftBlockStates;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.List;

public class NMSHandler26_1_2 implements INMSWrapper {
    @Override
    public List<String> getCompatibleNMSVersion() {
        return List.of("26.1", "26.1.1", "26.1.2");
    }

    @Override
    public String getNbtAsString(World world, BlockState blockState) {
        ServerLevel level = ((CraftWorld) world).getHandle();
        return level.getBlockEntity(new BlockPos(blockState.getX(), blockState.getY(), blockState.getZ()))
                .saveWithFullMetadata(level.registryAccess()).toString();
    }

    @Override
    public void setBlockNMS(World world, int x, int y, int z, BlockData data) {
        ((CraftWorld) world).getHandle().setBlock(
                new BlockPos(x, y, z), ((CraftBlockData) data).getState(), 3
        );
    }

    @Override
    public void loadTileEntity(World world, int x, int y, int z, String nbt) {
        ServerLevel level = ((CraftWorld) world).getHandle();
        BlockEntity entity = level.getBlockEntity(new BlockPos(x, y, z));
        if (entity == null)
            throw new IllegalStateException("Expect block entity not to be null. At world = %s, x = %d, y = %d, z = %d".formatted(world.getName(), x, y, z));

        try (ProblemReporter.ScopedCollector scopedCollector = new ProblemReporter.ScopedCollector(entity.problemPath(), MinecraftServer.LOGGER)) {
            entity.loadCustomOnly(TagValueInput.create(scopedCollector, level.registryAccess(), TagParser.parseCompoundFully(nbt)));
        } catch (CommandSyntaxException | NullPointerException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void createTileEntity(World world, int x, int y, int z, BlockData data, String nbt) {
        setBlockNMS(world, x, y, z, data);
        loadTileEntity(world, x, y, z, nbt);
    }

    @Override
    public double[] getRecentTps() {
        return MinecraftServer.getServer().getTPS();
    }

    @Override
    public double getLuckForPlayer(Player player) {
        return ((CraftPlayer) player).getHandle().getLuck();
    }

    @Override
    public BlockState convertBlockDataToBlockState(BlockData blockData) {
        return CraftBlockStates.getBlockState(((CraftBlockData) blockData).getState(), null);
    }

    @Override
    public int getWorldMinHeight(World world) {
        return ((CraftWorld) world).getHandle().getMinY();
    }

    Material[] oreBlocks = new Material[] {
            Material.COAL_ORE, Material.COPPER_ORE, Material.IRON_ORE, Material.GOLD_ORE, Material.DIAMOND_ORE, Material.EMERALD_ORE, Material.LAPIS_ORE, Material.REDSTONE_ORE, Material.DEEPSLATE_COAL_ORE, Material.DEEPSLATE_COPPER_ORE, Material.DEEPSLATE_IRON_ORE, Material.DEEPSLATE_GOLD_ORE, Material.DEEPSLATE_DIAMOND_ORE, Material.DEEPSLATE_EMERALD_ORE, Material.DEEPSLATE_LAPIS_ORE, Material.DEEPSLATE_REDSTONE_ORE, Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE, Material.ANCIENT_DEBRIS
    };

    @Override
    public Material[] getOreBlocks() {
        return oreBlocks;
    }
}
