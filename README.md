# NatureRevive - brilliant | Resource Regeneration, Say Goodbye to the Resource World!
## Game Version: 1.17 - 26.1.2    
- Chinese  Introduction: [https://forum.gamer.com.tw/C.php?bsn=18673&snA=205249](https://forum.gamer.com.tw/C.php?bsn=18673&snA=205249)    
- Spigot: https://www.spigotmc.org/resources/naturerevive-brilliant.136155    
- Hangar: https://hangar.papermc.io/RICE0707/NatureRevive-Brilliant    
- Modrinth: https://modrinth.com/plugin/naturerevive-brilliant    

[Made for Brilliant Server.](https://discord.gg/9c287zPpUZ)

## 📃 License

**This plugin is open-source under the AGPL v3.0 license.**

## 🔴 Dependencies

**CoreProtect (Optional), Residence (Optional), GriefPrevention (Optional), FastAsyncWorldEdit (Optional, Required for 1.21+)**

## 🖌 Commands

```
/nr forceregenall - Forcefully regenerate all expired chunks

/nr regenchunk <bukkit/fawe> - Forcefully regenerate the chunk you are currently in (bukkit will be automatically disabled in 1.21+)

/nr reload - Reload the plugin

/nr pause - Pause the resource regeneration process

/nr resume - Resume the resource regeneration process

/nr migrate <yaml/sqlite/mysql> - Migrate the database to the specified database type

/nr debug - Debug messages, do not use unless necessary

```

## 🔓 Permissions

```
naturerevive.forceregenall - Permission to use /nr forceregenall

naturerevive.regenthischunk - Permission to use /nr regenchunk

naturerevive.togglerevive - Permission to use /nr pause and /nr resume

naturerevive.reloadreviveconfig - Permission to use /nr reload

naturerevive.navmigrate - Permission to use /nr migrate

naturerevive.debug - Permission to use /nr debug

```