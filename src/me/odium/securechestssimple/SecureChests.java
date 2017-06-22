package me.odium.securechestssimple;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.FileConfigurationOptions;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SecureChests extends JavaPlugin {

  //ClassListeners
  private final SecureChestsPlayerListener playerListener = new SecureChestsPlayerListener(this);
  private final SecureChestsBlockListener blockListener = new SecureChestsBlockListener(this);
  private final SecureChestsRedstoneListener redstoneListener = new SecureChestsRedstoneListener(this);
  private final SecureChestsExplosionListener explosionListener = new SecureChestsExplosionListener(this);

  //Define the logger  
  public Logger log = Logger.getLogger("Minecraft");



  public static final Map<Material, String> BLOCK_LIST = createMap();

  private static Map<Material, String> createMap() {
    Map<Material, String> result = new HashMap<Material, String>();
    result.put(Material.DISPENSER, "dispenser");
    result.put(Material.CHEST, "chest");
    result.put(Material.FURNACE, "furnace");
    result.put(Material.BURNING_FURNACE, "furnace");
    result.put(Material.WOODEN_DOOR, "door");    
    result.put(Material.TRAP_DOOR, "trapdoor");
    result.put(Material.FENCE_GATE, "fence gate");
    result.put(Material.BREWING_STAND, "potion stand");
    result.put(Material.ENDER_CHEST, "enderchest");
    result.put(Material.ITEM_FRAME, "frame");
    result.put(Material.HOPPER, "hopper");
    result.put(Material.DROPPER, "dropper");
    result.put(Material.ANVIL, "anvil");
    result.put(Material.TRAPPED_CHEST, "trapped chest");
    return Collections.unmodifiableMap(result);	
  }

  public Map<Material, Boolean> blockStatus = new HashMap<Material, Boolean>();
  public Map<Player, Integer> scCmd = new HashMap<Player, Integer>();
  public Map<Player, String> scAList = new HashMap<Player, String>();	
  public Map<Integer, Boolean> blockExplosion = new HashMap<Integer, Boolean>();



  //Custom Config  
  private FileConfiguration StorageConfig = null;
  private File StorageConfigFile = null;

  public void reloadStorageConfig() {
    if (StorageConfigFile == null) {
      StorageConfigFile = new File(getDataFolder(), "Storage.yml");
    }
    StorageConfig = YamlConfiguration.loadConfiguration(StorageConfigFile);
    // Look for defaults in the jar
    InputStream defConfigStream = getResource("Storage.yml");
    if (defConfigStream != null) {
      YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
      StorageConfig.setDefaults(defConfig);
    }
  }

  public FileConfiguration getStorageConfig() {
    if (StorageConfig == null) {
      reloadStorageConfig();
    }
    return StorageConfig;
  }

  public void saveStorageConfig() {
    if (StorageConfig == null || StorageConfigFile == null) {
      return;
    }
    try {
      StorageConfig.save(StorageConfigFile);
    } catch (IOException ex) {
      Logger.getLogger(JavaPlugin.class.getName()).log(Level.SEVERE, "Could not save config to " + StorageConfigFile, ex);
    }
  }
  // End Custom Config

  
  //Custom Config  
  private FileConfiguration AListConfig = null;
  private File AListConfigFile = null;

  public void reloadAListConfig() {
    if (AListConfigFile == null) {
      AListConfigFile = new File(getDataFolder(), "AList.yml");
    }
    AListConfig = YamlConfiguration.loadConfiguration(AListConfigFile);
    // Look for defaults in the jar
    InputStream defConfigStream = getResource("AList.yml");
    if (defConfigStream != null) {
      YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
      AListConfig.setDefaults(defConfig);
    }
  }

  public FileConfiguration getAListConfig() {
    if (AListConfig == null) {
      reloadAListConfig();
    }
    return AListConfig;
  }

  public void saveAListConfig() {
    if (AListConfig == null || AListConfigFile == null) {
      return;
    }
    try {
      AListConfig.save(AListConfigFile);
    } catch (IOException ex) {
      Logger.getLogger(JavaPlugin.class.getName()).log(Level.SEVERE, "Could not save config to " + AListConfigFile, ex);
    }
  }
  // End Custom Config
  
  
  

  public void displayMessage(Player player, String Message) {
    player.sendMessage(ChatColor.DARK_BLUE + "[Secure Chests]" + ChatColor.WHITE + " " + Message);
  }


  public void displayHelp(CommandSender sender) {
    sender.sendMessage(ChatColor.GOLD + "[ "+ChatColor.GREEN+"SecureChests-Simple " + getDescription().getVersion() + ChatColor.GOLD+" ]");
    if (sender.hasPermission("securechests.lock")) {
      sender.sendMessage(ChatColor.GOLD + " /lock" + ChatColor.WHITE + " - Lock containers/doors etc");
      sender.sendMessage(ChatColor.GOLD + " /unlock" + ChatColor.WHITE + " - Unlock your containers/doors etc");
      sender.sendMessage(ChatColor.GOLD + " /sc access" + ChatColor.WHITE + " - Check a block's access list");
      sender.sendMessage(ChatColor.GOLD + " /sc add player" + ChatColor.WHITE + " - Add user to access list");
      sender.sendMessage(ChatColor.GOLD + " /sc deny player" + ChatColor.WHITE + " - Add user to deny list");
      sender.sendMessage(ChatColor.GOLD + " /sc remove player" + ChatColor.WHITE + " - Remove user from access list");
      sender.sendMessage(ChatColor.GOLD + " /sc gadd player" + ChatColor.WHITE + " - Add user to your global access list");
      sender.sendMessage(ChatColor.GOLD + " /sc gremove player" + ChatColor.WHITE + " - Remove user from global access list");
    } else {
      sender.sendMessage(ChatColor.RED + "You don't have permission");
    }
    if (sender.hasPermission("securechests.reload")) {
      sender.sendMessage(ChatColor.GOLD + " /sc reload" + ChatColor.WHITE + " - reload config files");
    }
  }

  public void onEnable() {
    PluginManager pm = this.getServer().getPluginManager();
    pm.registerEvents(blockListener, this);
    pm.registerEvents(playerListener, this);
    pm.registerEvents(redstoneListener, this);
    pm.registerEvents(explosionListener, this);
    try{     // if config.yml is missing from package, create your own.
      FileConfiguration config = getConfig();
      if(!config.contains("Furnace")){
        config.set("Furnace", true);
      }
      if(!config.contains("Door")){
        config.set("Door", false);
      }
      if(!config.contains("Chest")){
        config.set("Chest", true);
      }    
      if(!config.contains("Dispenser")){
        config.set("Dispenser", false);
      }  
      if(!config.contains("Trapdoor")){
        config.set("Trapdoor", false);
      }  
      if(!config.contains("Potion")){
        config.set("Potion", true);
      }  
      if(!config.contains("Gate")){
        config.set("Gate", true);
      }  
      if(!config.contains("EnderChest")){
        config.set("EnderChest", true);
      }  
      if(!config.contains("Frame")){
        config.set("Frame", true);
      }  
      if(!config.contains("Hopper")){
        config.set("Hopper", true);
      }  
      if(!config.contains("Dropper")){
        config.set("Dropper", true);
      }  
      if(!config.contains("Anvil")){
        config.set("Anvil", true);
      }  
      if(!config.contains("TrappedChest")){
        config.set("TrappedChest", true);
      }  
      saveConfig();
    }catch(Exception e1){
      e1.printStackTrace();
    } // END TRY    
    // load / create config
    FileConfiguration cfg = getConfig();
    FileConfigurationOptions cfgOptions = cfg.options();
    cfgOptions.copyDefaults(true);
    cfgOptions.copyHeader(true);
    saveConfig();  
 // Load Custom Config
    FileConfiguration ccfg = getAListConfig();
    FileConfigurationOptions ccfgOptions = ccfg.options();
    ccfgOptions.copyDefaults(true);
    ccfgOptions.copyHeader(true);
    saveAListConfig();
    
    blockStatus.clear(); // clear the enable/disabled for all block during initialization.
    // Get current active block
    for (Material key : BLOCK_LIST.keySet() ) { //Start with all block default to disabled.
      blockStatus.put(key, false);
    }

    if(getConfig().getBoolean("Chest"))
      blockStatus.put(Material.CHEST, true);
    if(getConfig().getBoolean("Furnace")) {
      blockStatus.put(Material.FURNACE, true);
      blockStatus.put(Material.BURNING_FURNACE, true);
    }
    if(getConfig().getBoolean("Door"))
      blockStatus.put(Material.WOODEN_DOOR, true);
    if(getConfig().getBoolean("Dispenser"))
      blockStatus.put(Material.DISPENSER, true);
    if(getConfig().getBoolean("Trapdoor"))
      blockStatus.put(Material.TRAP_DOOR, true);
    if(getConfig().getBoolean("Gate"))
      blockStatus.put(Material.FENCE_GATE, true);
    if(getConfig().getBoolean("Potion"))
      blockStatus.put(Material.BREWING_STAND, true);
    if(getConfig().getBoolean("EnderChest"))
      blockStatus.put(Material.ENDER_CHEST, true);
    if(getConfig().getBoolean("Frame"))
      blockStatus.put(Material.ITEM_FRAME, true);
    if(getConfig().getBoolean("Hopper"))
      blockStatus.put(Material.HOPPER, true);
    if(getConfig().getBoolean("Dropper"))
      blockStatus.put(Material.DROPPER, true);
    if(getConfig().getBoolean("Anvil"))
      blockStatus.put(Material.ANVIL, true);
    if(getConfig().getBoolean("TrappedChest"))
      blockStatus.put(Material.TRAPPED_CHEST, true);
    // log initilization and continue
    log.info("[" + getDescription().getName() + "] " + getDescription().getVersion() + " enabled.");    
  }

  private void reloadPlugin() {
    reloadAListConfig();
    reloadStorageConfig();
    reloadConfig();
    blockStatus.clear(); // clear the enable/disabled for all block during initialization.
    // Get current active block
    for (Material key : BLOCK_LIST.keySet() ) { //Start with all block default to disabled.
      blockStatus.put(key, false);
    }

    if(getConfig().getBoolean("Chest"))
      blockStatus.put(Material.CHEST, true);
    if(getConfig().getBoolean("Furnace")) {
      blockStatus.put(Material.FURNACE, true);
      blockStatus.put(Material.BURNING_FURNACE, true);
    }
    if(getConfig().getBoolean("Door"))
      blockStatus.put(Material.WOODEN_DOOR, true);
    if(getConfig().getBoolean("Dispenser"))
      blockStatus.put(Material.DISPENSER, true);
    if(getConfig().getBoolean("Trapdoor"))
      blockStatus.put(Material.TRAP_DOOR, true);
    if(getConfig().getBoolean("Gate"))
      blockStatus.put(Material.FENCE_GATE, true);
    if(getConfig().getBoolean("Potion"))
      blockStatus.put(Material.BREWING_STAND, true);
    if(getConfig().getBoolean("EnderChest"))
      blockStatus.put(Material.ENDER_CHEST, true);
    if(getConfig().getBoolean("Frame"))
      blockStatus.put(Material.ITEM_FRAME, true);
    if(getConfig().getBoolean("Hopper"))
      blockStatus.put(Material.HOPPER, true);
    if(getConfig().getBoolean("Dropper"))
      blockStatus.put(Material.DROPPER, true);
    if(getConfig().getBoolean("Anvil"))
      blockStatus.put(Material.ANVIL, true);
    if(getConfig().getBoolean("TrappedChest"))
      blockStatus.put(Material.TRAPPED_CHEST, true);
    log.info("[" + getDescription().getName() + "] Reload complete");
  }

  public void onDisable() {
    saveStorageConfig();
    saveAListConfig();
    saveConfig();
    log.info("[" + getDescription().getName() + "] " + getDescription().getVersion() + " Disabled."); 
  }

  // will return :
  // 1. exact name if online
  // 2. partial name if online
  // 3. if neither are true then return same name given
  public String myGetPlayerName(String name) { 
    Player caddPlayer = getServer().getPlayerExact(name);
    String pName;
    if(caddPlayer == null) {
      caddPlayer = getServer().getPlayer(name);
      if(caddPlayer == null) {
        pName = name;
      } else {
        pName = caddPlayer.getName();
      }
    } else {
      pName = caddPlayer.getName();
    }
    return pName;
  }
  
  public void purgeLocks() {
    Boolean itemframeDetection = false;
    int n = 0;
    
    Set <String> locations = this.getStorageConfig().getKeys(true);
    for (String loc : locations) {        
      if (loc.contains(".") && !loc.contains("owner")) {
        String[] locSplit = loc.split("\\.");
        World world = Bukkit.getWorld(locSplit[0]);                  

        String[] coords = locSplit[1].split("\\_");
        int x = Integer.parseInt(coords[0]);
        int y = Integer.parseInt(coords[1]);
        int z = Integer.parseInt(coords[2]);

        Location lockLocation = new Location(world, x, y, z);
        Material lockBlockType = lockLocation.getBlock().getType(); 

        Entity[] chunkEnts = lockLocation.getBlock().getChunk().getEntities();
        for (Entity ent : chunkEnts) {

          World entworldName = ent.getWorld();
          int entX = ent.getLocation().getBlockX();
          int entY = ent.getLocation().getBlockY();
          int entZ = ent.getLocation().getBlockZ();
          Location entityLocation = new Location(entworldName, entX, entY, entZ);

          if (entityLocation.equals(lockLocation)) {
            itemframeDetection = true;
            //log.info("ITEMFRAMDETECTION: TRUE");
          }  
        }

        if( (SecureChests.BLOCK_LIST.containsKey(lockBlockType) && this.blockStatus.get(lockBlockType)) || itemframeDetection == true) {
          itemframeDetection = false;
        } else {
          n++;
          log.info("##### Broken Lock Found #####");
          log.info("BlockType: "+lockBlockType.toString());
          log.info("BlockLocation: "+world.getName()+", "+x+", "+y+", "+z);
          log.info("Block type is not protected by securechests");                
          this.getStorageConfig().set(locSplit[0]+"."+locSplit[1], null);
          log.info("---- Lock Removed ----");                       
          log.info(" ");
        }
      }
    }
    if (n == 0) {      
      log.info("No Broken Locks Found!");
    } else {
      log.info(n+" Broken Locks Removed."); 
      this.saveStorageConfig();  
    }    
  }
  //END PURGE COMMAND
  

  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    Player player = null;
    if (sender instanceof Player) {
      player = (Player) sender;
    }
    
    if (cmd.getName().equalsIgnoreCase("lock")){ 
      if (player == null) {
        sender.sendMessage("this command can only be run by a player.");
        
      } else {
        if(args.length == 1) {
          if (sender.hasPermission("securechests.bypass.lock")) {
            String pName = myGetPlayerName(args[0]);
            String uuidString = Bukkit.getOfflinePlayer(args[0]).getUniqueId().toString();
            displayMessage(player, "Now interact with a container/door to lock it for "+pName+".");
            scAList.put(player, uuidString);
            scCmd.put(player, 6);
          } else {
            displayMessage(player, "You dont have permission to lock other's Blocks!");
          }
        } else if (args.length == 0 && sender.hasPermission("securechests.lock")) {
          displayMessage(player, "Now interact with a container/door to lock it.");
          scCmd.put(player, 1);
        } else if(args[0].equalsIgnoreCase("reload") && args.length == 1) {
          reloadPlugin();
        } else {
          displayMessage(player, "You dont have permission to lock this!");
        }
      }
      return true;
    } else if (cmd.getName().equalsIgnoreCase("unlock")) {
      if (player == null) {
        sender.sendMessage("this command can only be run by a player.");
        return true;
      } else {
        if (sender.hasPermission("securechests.lock")) {
          displayMessage(player, "Now interact with a container/door to unlock it.");
          scCmd.put(player, 2);
        } else {
          displayMessage(player, "You dont have permission to lock your Blocks!");
          return true;
        }
      }
    } else if (cmd.getName().equalsIgnoreCase("sc") || cmd.getName().equalsIgnoreCase("securechests") || cmd.getName().equalsIgnoreCase("securechest")) {
      if (player == null) {
        if(args.length == 0 || args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?")) { //get help menu
          displayHelp(sender);
        } else if(args[0].equalsIgnoreCase("reload") && args.length == 1) {
          reloadPlugin();
        } else if(args[0].equalsIgnoreCase("purge") && args.length == 1) {
          purgeLocks();
        } else {
          sender.sendMessage("this command can only be run by a player.");
          return true;
        }
      } else {
        if(args.length == 0 || args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?")) { //get help menu
          displayHelp(sender);
        } else if (args[0].equalsIgnoreCase("lock") && args.length == 1) { // Code to activate locking mode.
          if (sender.hasPermission("securechests.lock")) {
            displayMessage(player, "Now interact with a container/door to lock it.");
            scCmd.put(player, 1);
          } else {
            displayMessage(player, "You dont have permission to lock your chests.");
            return true;
          }
        } else if(args[0].equalsIgnoreCase("lock") && args.length == 2) {
          if (sender.hasPermission("securechests.bypass.lock")) {
            String pName = myGetPlayerName(args[1]);
            String uuidString = Bukkit.getOfflinePlayer(args[1]).getUniqueId().toString();
            displayMessage(player, "Now interact with a container/door to lock it for " + pName);
            scAList.put(player, uuidString);
            scCmd.put(player, 6);				 
          } else {					
            displayMessage(player, "You dont have permission to lock other's Blocks!");
          }
        } else if (args[0].equalsIgnoreCase("unlock")) { // UNLOCK!
          if (sender.hasPermission("securechests.lock")) {
            displayMessage(player, "Now interact with a container/door to unlock it.");
            scCmd.put(player, 2);
          } else {
            displayMessage(player, "You dont have permission to lock your Blocks!");
          }
        } else if (args[0].equalsIgnoreCase("add")) {  //Add player to chest access list.
          if (sender.hasPermission("securechests.lock")) {
            if (args.length != 2) {
              displayMessage(player, "Correct command usage: /sc add username");
            } else {
              String pName = myGetPlayerName(args[1]);
              String uuidString = Bukkit.getOfflinePlayer(args[1]).getUniqueId().toString();
              displayMessage(player, "will add user " + pName + " to the next owned block you interact with.");
              scAList.put(player, uuidString);
              scCmd.put(player, 3);
            }
          } 
        } else if (args[0].equalsIgnoreCase("remove")) { // Remove player from chest access list
          if (sender.hasPermission("securechests.lock")) {
            if (args.length != 2) {
              displayMessage(player, "Correct command usage: /sc remove username");
            } else {
              String pName = myGetPlayerName(args[1]);
              String uuidString = Bukkit.getOfflinePlayer(args[1]).getUniqueId().toString();
              displayMessage(player, "will remove user " + pName + " from the next owned block you interact with.");
              scAList.put(player, uuidString);
              scCmd.put(player, 4);
            }
          } else {
            displayMessage(player, "You dont have permission.");
          }
        } else if (args[0].equalsIgnoreCase("deny")) { // Remove player from chest access list
          if (sender.hasPermission("securechests.lock")) {
            if (args.length != 2) {
              displayMessage(player, "Correct command usage: /sc deny username");
            } else {
              String pName = myGetPlayerName(args[1]);
              String uuidString = Bukkit.getOfflinePlayer(args[1]).getUniqueId().toString();
              displayMessage(player, "will add user " + pName + " to the deny list of the next owned block you interact with.");
              scAList.put(player , uuidString);
              scCmd.put(player, 5);
            }
          } else {
            displayMessage(player, "You dont have permission.");
          }
        } else if (args[0].equalsIgnoreCase("gadd")) { //Add to global access list!
          if (sender.hasPermission("securechests.lock")) {
            if (args.length != 2) {
              displayMessage(player, "Correct command usage: /sc gadd username");
            } else {
              String pName = myGetPlayerName(args[1]).toLowerCase();
              String uuidString = Bukkit.getOfflinePlayer(args[1]).getUniqueId().toString();

              if (!getAListConfig().getBoolean(player.getUniqueId()+"." + uuidString)){
                displayMessage(player, "Adding " + pName + " to your global allow list.");
                getAListConfig().set(player.getUniqueId()+"." + uuidString, true);
                saveAListConfig();
              } else {
                displayMessage(player, "Player "+pName+" already in access list.");
              }
            }
          } else {
            displayMessage(player, "You dont have permission");
          }
        } else if (args[0].equalsIgnoreCase("gremove")) { //Add to global access list!
          if (sender.hasPermission("securechests.lock")) {
            if (args.length != 2) {
              displayMessage(player, "Correct command usage: /sc gremove username");
            } else {
              String pName = myGetPlayerName(args[1]).toLowerCase();
              String uuidString = Bukkit.getOfflinePlayer(args[0]).getUniqueId().toString();
              
              if (!getAListConfig().getBoolean(player.getUniqueId()+"." + uuidString)){
                displayMessage(player, "Player " + pName + " Not on your global access list");
              } else {
                getAListConfig().set(player.getUniqueId()+"." + uuidString, null);
                saveAListConfig();
                displayMessage(player, "Player "+pName+" removed from your global access list.");
              }
            }
          } else {
            displayMessage(player, "You dont have permission");
          }
        } else if (args[0].equalsIgnoreCase("reload") && player.hasPermission("securechests.reload")) {
          reloadPlugin();
          displayMessage(player, "Config Reloaded.");
        } else if (args[0].equalsIgnoreCase("access")) {
          displayMessage(player, "Now interact with a container/door to check it's access list");
          scCmd.put(player, 7);

          // PURGE COMMAND
        } else if (args[0].equalsIgnoreCase("purge")) {          
          displayMessage(player, "This command must be run from console!");
          return true;
        } else {
          displayMessage(player, "Unknown command. type \"/sc help\" for command list.");
        }
      }//End command checks!
    }
    return false;
  }
}
