package engineer.skyouo.plugins.naturerevive.spigot.config.adapters;

import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import engineer.skyouo.plugins.naturerevive.spigot.config.DatabaseConfig;
import engineer.skyouo.plugins.naturerevive.spigot.structs.BukkitPositionInfo;
import engineer.skyouo.plugins.naturerevive.spigot.structs.SQLCommand;
import org.bukkit.Location;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SQLiteDatabaseAdapter implements DatabaseConfig, SQLDatabaseAdapter {
    private Connection connection = null;
    private Map<Location, BukkitPositionInfo> cache = new ConcurrentHashMap<>();

    public SQLiteDatabaseAdapter() {
        new File("plugins/NatureRevive").mkdirs();

        File databaseFile = new File(NatureRevivePlugin.instance.getDataFolder(), String.format("%s.db", NatureRevivePlugin.readonlyConfig.databaseName));

        if (!databaseFile.exists()) {
            try {
                databaseFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            Class.forName("org.sqlite.JDBC");

            String url = "jdbc:sqlite:" + databaseFile;
            connection = DriverManager.getConnection(url);

            connection.createStatement()
                    .execute(String.format("CREATE TABLE IF NOT EXISTS %s (X INTEGER NOT NULL, Z INTEGER NOT NULL, TTL INTEGER, WORLDNAME VARCHAR(255) NOT NULL, PRIMARY KEY (X, Z, WORLDNAME));", NatureRevivePlugin.readonlyConfig.databaseTableName));

            for (BukkitPositionInfo positionInfo : values()) { // Build up cache
                cache.put(positionInfo.getLocation(), positionInfo);
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            close();
        }
    }

    public void set(BukkitPositionInfo positionInfo) {
        NatureRevivePlugin.sqlCommandQueue.add(new SQLCommand(positionInfo, SQLCommand.Type.INSERT));

        cache.put(positionInfo.getLocation(), positionInfo);
    }

    public void unset(BukkitPositionInfo positionInfo) {
        NatureRevivePlugin.sqlCommandQueue.add(new SQLCommand(positionInfo, SQLCommand.Type.DELETE));

        cache.remove(positionInfo.getLocation());
    }

    public BukkitPositionInfo get(Location location) {
        Location posLocation = new BukkitPositionInfo(location, 0).getLocation();

        if (cache.containsKey(posLocation))
            return cache.get(posLocation);

        /*

        try {
            ResultSet resultSet = connection.createStatement()
                    .executeQuery("SELECT * FROM locations WHERE X = " + chunkPos.chunkX + " AND Z = " + chunkPos.chunkZ +  " AND WORLDNAME = '" + location.getWorld().getName() + "';");

            if (resultSet.isClosed())
                return null;

            String worldName = resultSet.getString("WORLDNAME");

            PositionInfo positionInfo = new PositionInfo(resultSet.getInt("X"), resultSet.getInt("Z"), resultSet.getLong("TTL"), worldName);
            resultSet.close();
            return positionInfo;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }

         */ // NCT skyouo - do not perform further lookup

        return null;
    }

    public BukkitPositionInfo get(BukkitPositionInfo positionInfo) {

        if (cache.containsKey(positionInfo.getLocation()))
            return cache.get(positionInfo.getLocation());

        /*

        try {
            ResultSet resultSet = connection.createStatement()
                    .executeQuery("SELECT * FROM locations WHERE X = " + chunkPos.chunkX + " AND Z = " + chunkPos.chunkZ +  " AND WORLDNAME = '" + positionInfo.getLocation().getWorld().getName() + "';");

            if (resultSet.isClosed())
                return null;

            String worldName = resultSet.getString("WORLDNAME");

            PositionInfo positionInfoResult = new PositionInfo(resultSet.getInt("X"), resultSet.getInt("Z"), resultSet.getLong("TTL"), worldName);
            resultSet.close();

            return positionInfoResult;

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }

         */ // NCT skyouo - do not perform further lookup.

        return null;
    }

    public List<BukkitPositionInfo> values() {
        List<BukkitPositionInfo> positionInfos = new ArrayList<>();

        if (!cache.isEmpty())
            return new ArrayList<>(cache.values());

        try {
            ResultSet resultSet = connection.createStatement()
                    .executeQuery("SELECT * FROM locations;");

            if (resultSet.isClosed())
                return positionInfos;

            while (resultSet.next()) {
                positionInfos.add(
                        new BukkitPositionInfo(resultSet.getString("WORLDNAME"), resultSet.getInt("X"), resultSet.getInt("Z"), resultSet.getLong("TTL"))
                );
            }

            resultSet.close();
            return positionInfos;
        } catch (SQLException e) {
            e.printStackTrace();
            return positionInfos;
        }
    }

    public void save() {
    }

    @Override
    public void massUpdate(Set<BukkitPositionInfo> positionInfoSet) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("UPDATE " + NatureRevivePlugin.readonlyConfig.databaseTableName + " SET TTL = ? WHERE X = ? AND Z = ? AND WORLDNAME = ?;");
            for (BukkitPositionInfo positionInfo : positionInfoSet) {
                cache.put(positionInfo.getLocation(), positionInfo);

                preparedStatement.setLong(1, positionInfo.getTTL());
                preparedStatement.setInt(2, positionInfo.getX());
                preparedStatement.setInt(3, positionInfo.getZ());
                preparedStatement.setString(4, positionInfo.getWorldName());
                preparedStatement.addBatch();
            }

            preparedStatement.executeBatch();
            preparedStatement.clearBatch();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void massInsert(Set<BukkitPositionInfo> positionInfoSet) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO " + NatureRevivePlugin.readonlyConfig.databaseTableName + " (X, Z, TTL, WORLDNAME) VALUES (?, ?, ?, ?);");
            for (BukkitPositionInfo positionInfo : positionInfoSet) {
                cache.put(positionInfo.getLocation(), positionInfo);

                preparedStatement.setInt(1, positionInfo.getX());
                preparedStatement.setInt(2, positionInfo.getZ());
                preparedStatement.setLong(3, positionInfo.getTTL());
                preparedStatement.setString(4, positionInfo.getWorldName());
                preparedStatement.addBatch();
            }

            preparedStatement.executeBatch();
            preparedStatement.clearBatch();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean massExecute(List<SQLCommand> sqlCommandList) {
        try {
            PreparedStatement preparedStatementInsert = connection.prepareStatement("INSERT INTO " + NatureRevivePlugin.readonlyConfig.databaseTableName + " (X, Z, TTL, WORLDNAME) VALUES (?, ?, ?, ?) ON CONFLICT(X, Z, WORLDNAME) DO UPDATE SET TTL = excluded.TTL;");
            PreparedStatement preparedStatementUpdate = connection.prepareStatement("UPDATE " + NatureRevivePlugin.readonlyConfig.databaseTableName + " SET TTL = ? WHERE X = ? AND Z = ? AND WORLDNAME = ?;");
            PreparedStatement preparedStatementDelete = connection.prepareStatement("DELETE FROM " + NatureRevivePlugin.readonlyConfig.databaseTableName + " WHERE X = ? AND Z = ? AND WORLDNAME = ?;");

            for (SQLCommand sqlCommand : sqlCommandList) {
                if (sqlCommand.getType().equals(SQLCommand.Type.INSERT)) {
                    preparedStatementInsert.setInt(1, sqlCommand.getBukkitPositionInfo().getX());
                    preparedStatementInsert.setInt(2, sqlCommand.getBukkitPositionInfo().getZ());
                    preparedStatementInsert.setLong(3, sqlCommand.getBukkitPositionInfo().getTTL());
                    preparedStatementInsert.setString(4, sqlCommand.getBukkitPositionInfo().getWorldName());
                    preparedStatementInsert.execute();
                    preparedStatementInsert.clearParameters();
                } else if (sqlCommand.getType().equals(SQLCommand.Type.UPDATE)) {
                    preparedStatementUpdate.setLong(1, sqlCommand.getBukkitPositionInfo().getTTL());
                    preparedStatementUpdate.setInt(2, sqlCommand.getBukkitPositionInfo().getX());
                    preparedStatementUpdate.setInt(3, sqlCommand.getBukkitPositionInfo().getZ());
                    preparedStatementUpdate.setString(4, sqlCommand.getBukkitPositionInfo().getWorldName());
                    preparedStatementUpdate.execute();
                    preparedStatementUpdate.clearParameters();
                } else if (sqlCommand.getType().equals(SQLCommand.Type.DELETE)) {
                    preparedStatementDelete.setInt(1, sqlCommand.getBukkitPositionInfo().getX());
                    preparedStatementDelete.setInt(2, sqlCommand.getBukkitPositionInfo().getZ());
                    preparedStatementDelete.setString(3, sqlCommand.getBukkitPositionInfo().getWorldName());
                    preparedStatementDelete.execute();
                    preparedStatementDelete.clearParameters();
                }
            }

            preparedStatementInsert.close();
            preparedStatementUpdate.close();
            preparedStatementDelete.close();

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }

            cache.clear();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}