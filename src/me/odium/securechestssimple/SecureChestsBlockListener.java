package me.odium.securechestssimple;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;

public class SecureChestsBlockListener implements Listener {


  public SecureChests plugin;

  public SecureChestsBlockListener(SecureChests instance) {
    plugin = instance;
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onBlockPlace(final BlockPlaceEvent event) {

    Block b=event.getBlock();
    if(b.getType() == Material.CHEST) { //make sure block click is a chest.

      Player player = event.getPlayer();

      Location chestloc = b.getLocation();

      //START double chest detection
      Location ccN = b.getLocation();
      Location ccE = b.getLocation();
      Location ccS = b.getLocation();
      Location ccW = b.getLocation();

      ccN = ccN.subtract(0,0,1);
      ccE = ccE.subtract(1,0,0);
      ccS = ccS.add(0,0,1);
      ccW = ccW.add(1,0,0);

      if (ccN.getBlock().getType() == Material.CHEST) {
        chestloc = chestloc.subtract(0, 0, 1);
      } else if (ccE.getBlock().getType() == Material.CHEST) {
        chestloc = chestloc.subtract(1, 0, 0);
      }
      //END double chest detection

      Boolean chestChange = false;
      if (ccN.getBlock().getType() == Material.CHEST) {
        chestloc = chestloc.subtract(0,0,1);
      } else if (ccE.getBlock().getType() == Material.CHEST) {
        chestloc = chestloc.subtract(1, 0, 0);
      } else if (ccS.getBlock().getType() == Material.CHEST) {
        chestloc = chestloc.add(0, 0, 1);
        chestChange = true;
      } else if (ccW.getBlock().getType() == Material.CHEST) {
        chestloc = chestloc.add(1, 0, 0);
        chestChange = true;
      }


      //create the YAML string location
      String yamlNewLoc = b.getLocation().getWorld().getName() + "." + b.getLocation().getBlockX() + "_" + b.getLocation().getBlockY() + "_" + b.getLocation().getBlockZ();
      String yamlOldLoc = chestloc.getWorld().getName() + "." + chestloc.getBlockX() + "_" + chestloc.getBlockY() + "_" + chestloc.getBlockZ();

      //get owner name if any
      String lockname = plugin.getStorageConfig().getString(yamlOldLoc.concat(".owner"));
      if(lockname == null)
        return;


      if(lockname.equalsIgnoreCase(player.getName())) {
        plugin.displayMessage(player, "Chest lock extended.");
        if (chestChange) {
          plugin.getStorageConfig().set(yamlOldLoc, null);
          plugin.getStorageConfig().set(yamlNewLoc+".owner", lockname);
        }

        plugin.saveStorageConfig();
      } else {
        plugin.displayMessage(player, "Unable to modify chest owned by ".concat(lockname));
        event.setCancelled(true);
      }
      // IF PLACING HOPPER
    } else if (b.getType() == Material.HOPPER) {
      Player player = event.getPlayer();      
      Location Chest_Loc_UP = b.getLocation().add(0, 1, 0);      
      Location Chest_Loc_DOWN = b.getLocation().subtract(0, 1, 0);

      // IF BLOCK ABOVE IS IN BLOCKLIST     
      if(SecureChests.BLOCK_LIST.containsKey(Chest_Loc_UP.getBlock().getType()) && plugin.blockStatus.get(Chest_Loc_UP.getBlock().getType())) {
        plugin.log.info("block above is container");
        String Chest_Loc_Locked = Chest_Loc_UP.getWorld().getName() + "." + Chest_Loc_UP.getBlockX() + "_" + Chest_Loc_UP.getBlockY() + "_" + Chest_Loc_UP.getBlockZ();
        String lockname = plugin.getStorageConfig().getString(Chest_Loc_Locked.concat(".owner"));
        // IF CHEST IS LOCKED        
        if (lockname != null && !lockname.equalsIgnoreCase(player.getName())) {
          plugin.log.info("block above is locked");
          String blockName = SecureChests.BLOCK_LIST.get(Chest_Loc_UP.getBlock().getType());
          event.setCancelled(true);
          plugin.displayMessage(player, "Cannot place hopper beneath "+lockname+"'s locked "+blockName);
          return;          
        }


      } else if(SecureChests.BLOCK_LIST.containsKey(Chest_Loc_DOWN.getBlock().getType()) && plugin.blockStatus.get(Chest_Loc_DOWN.getBlock().getType())) {
        plugin.log.info("block below is container");
        String Chest_Loc_Locked = Chest_Loc_DOWN.getWorld().getName() + "." + Chest_Loc_DOWN.getBlockX() + "_" + Chest_Loc_DOWN.getBlockY() + "_" + Chest_Loc_DOWN.getBlockZ();
        String lockname = plugin.getStorageConfig().getString(Chest_Loc_Locked.concat(".owner"));
        // IF CHEST IS LOCKED        
        if (lockname != null && !lockname.equalsIgnoreCase(player.getName())) {
          plugin.log.info("block below is locked");
          String blockName = SecureChests.BLOCK_LIST.get(Chest_Loc_DOWN.getBlock().getType());
          event.setCancelled(true);
          plugin.displayMessage(player, "Cannot place hopper above "+lockname+"'s locked "+blockName);
          return;          
        }

      }
    }
  }

  @EventHandler(priority = EventPriority.LOW)	
  public void onBlockBreak(final BlockBreakEvent event) {
    //  DECLARATIONS
    Block b = event.getBlock();
    Player player = event.getPlayer();
    String blockName = null;


// IF BLOCK IS IN WATCHLIST & ENABLED IN CONFIG || IF BLOCK ABOVE IS A WOODENDOOR || IF BLOCK ABOVE IS AN ANVIL
    if((SecureChests.BLOCK_LIST.containsKey(b.getType()) && plugin.blockStatus.get(b.getType())) 
        || (b.getLocation().add(0,1,0).getBlock().getType() == Material.WOODEN_DOOR && plugin.blockStatus.get(Material.WOODEN_DOOR))
        || (b.getLocation().add(0,1,0).getBlock().getType() == Material.ANVIL && plugin.blockStatus.get(Material.ANVIL))) {  
  
      Location blockLoc = b.getLocation();


      if(b.getType() == Material.CHEST) { //do double chest location corrections
        Location ccN = b.getLocation();
        Location ccE = b.getLocation();
        Location ccS = b.getLocation();
        Location ccW = b.getLocation();

        ccN = ccN.subtract(0,0,1);
        ccE = ccE.subtract(1,0,0);
        ccS = ccS.add(0,0,1);
        ccW = ccW.add(1,0,0);

        //Boolean dchest = false;
        if (ccN.getBlock().getType() == Material.CHEST) {
          blockLoc = blockLoc.subtract(0, 0, 1);
          //    dchest = true;
        } else if (ccE.getBlock().getType() == Material.CHEST) {
          blockLoc = blockLoc.subtract(1, 0, 0);
          //    dchest = true;
        } else if (ccS.getBlock().getType() == Material.CHEST) {
          //    dchest = true;
        } else if (ccW.getBlock().getType() == Material.CHEST) {
          //    dchest = true;
        }
      } //END Chest location corrections.
      
      // START DOOR/ANVIL CORRECTIONS      
      // ANVIL CORRECTION      
      if (b.getLocation().add(0,1,0).getBlock().getType() == Material.ANVIL) { // if block above the effected block is AN ANVIL
        blockLoc = blockLoc.add(0,1,0); // RAISE BLOCKLOCATION UP 1
        blockName = "anvil";


      // DOOR CORRECTION
      // IF BLOCK IS NOT A DOOR, BUT BLOCK ABOVE IS A DOOR
      } else if (b.getLocation().add(0,1,0).getBlock().getType() == Material.WOODEN_DOOR && b.getType() != Material.WOODEN_DOOR) { 
        blockLoc = blockLoc.add(0,1,0); // RAISE BLOCKLOCATION UP 1
        blockName = "door";


      } else if(b.getType() == Material.WOODEN_DOOR) { // IF THE BLOCK CLICK IS A DOOR
        if (b.getRelative(BlockFace.DOWN).getType() == Material.WOODEN_DOOR) { // IF THE BLOCK CLICK IS TOP HALF OF DOOR
          blockLoc = blockLoc.subtract(0,1,0); // LOWER THE BLOCKLOCATION 1 DOWN (BOTTOM HALF OF DOOR)
        }
        blockName = "door";
      }
      // End Door Corrections

      //If neither of the above blocknames apply, Set the blockname as per the BlockList 
      if (blockName != "door" || blockName != "anvil") {
        blockName = SecureChests.BLOCK_LIST.get(b.getType());
      }
      
      Lock lock = new Lock(plugin);
      lock.setLocation(blockLoc);

      if(lock.isLocked()) {
        //The block has a locked status. lets now get the owner
        String owner = lock.getOwner();
        Integer access = lock.getAccess(player);
        if(access == 1) { //it's yours yay!
          lock.unlock();
          plugin.displayMessage(player, "Removed lock on " + blockName + ".");
          return;
        } else if (player.hasPermission("securechests.bypass.break")) { //you have the break bypass.
          lock.unlock();
          plugin.displayMessage(player, "Breaking " + owner + "'s "+ blockName +".");
          return;
        } else {
          plugin.displayMessage(player, "Can not break " + blockName + " owned by " + owner + ".");
          event.setCancelled(true);
          return;
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.LOW)  
  public void onPaintingBreakEntity(final HangingBreakByEntityEvent event) {    
    // IF A PLAYER BROKE AN ITEM FRAME 
    if (event.getRemover().getType().equals(EntityType.PLAYER) && event.getEntity().getType().equals(EntityType.ITEM_FRAME)) {
      Player player;
      player = (Player) event.getRemover();

      Lock lock = new Lock(plugin);
      Location blockLoc = event.getEntity().getLocation();
      String blockName = "Item Frame";
      lock.setLocation(blockLoc);

      //get the current /sc command status:
      // 0/null=none
      // 1= lock
      // 2= unlock
      // 3= add to chest access list
      // 4= remove from chest access list
      // 5= add to deny list
      // 6= lock for other (perms already checked).

      Integer cmdStatus = plugin.scCmd.remove(player);
      String otherPlayer = plugin.scAList.remove(player);
      if (cmdStatus == null) {
        cmdStatus = 0;
      }

      if(lock.isLocked()) {
        event.setCancelled(true);
        //The block has a locked status. lets now get the owner
        String owner = lock.getOwner();
        Integer access = lock.getAccess(player);
        if(access == 1) { //it's yours yay!
          if (cmdStatus == 2) {//unlock and stop from further interacton.
            lock.unlock();
            plugin.displayMessage(player, blockName + " Unlocked.");
            event.setCancelled(true); 
          } else if (cmdStatus == 3) {
            if(lock.addToAccessList(otherPlayer))
              plugin.displayMessage(player, otherPlayer + " added to " + blockName + "'s access list.");
            else
              plugin.displayMessage(player, "Player " + otherPlayer + " already on " + blockName + "'s access list.");
            event.setCancelled(true);
            return;
          } else if (cmdStatus == 4) {
            if(lock.removeFromAccessList(otherPlayer))
              plugin.displayMessage(player, otherPlayer + " removed from " + blockName + "'s access list.");
            else
              plugin.displayMessage(player, "Unable to find " + otherPlayer + " on " + blockName + "'s access list.");
            event.setCancelled(true);
            return;
          } else if (cmdStatus == 5) {
            if(lock.addToDenyList(otherPlayer))
              plugin.displayMessage(player, otherPlayer + " Added to " + blockName + "'s deny list.");
            else
              plugin.displayMessage(player, "Player " + otherPlayer + " already on " + blockName + "'s deny list.");
            event.setCancelled(true);
            return;
          } else { // no commands to run just open the chest
            plugin.displayMessage(player, "You own this " + blockName + ".");
            return;
          }
        } else if (access == 2) { //your on the allow list
          if (cmdStatus != 0) { //Trying to run a command on someone else's chest! NO NO!
            plugin.displayMessage(player, "Unable to run command on "+blockName+" owned by: "+owner);
            event.setCancelled(true);
          } else { //No command to run allow them access
            plugin.displayMessage(player, "You have access to " + owner + "'s "+ blockName +".");
          }
          return;
        } else if (access == 3) {
          if (cmdStatus == 2 && player.hasPermission("securechests.bypass.unlock")) {
            plugin.displayMessage(player, "Bypassing and unlocking "+blockName+" owned by "+owner+".");
            lock.unlock();
            event.setCancelled(true);
          } else {
            plugin.displayMessage(player, "bypassing lock owned by " + owner + ".");
          } 
          return;

        } else {
          plugin.displayMessage(player, "Can not break " + blockName + " owned by " + owner + ".");
          event.setCancelled(true);
          return;
        }
      } else if (cmdStatus == 1) {
        event.setCancelled(true);
        lock.lock(player.getName());
        plugin.displayMessage(player, "Locking " + blockName + ".");
        event.setCancelled(true);
      } else if (cmdStatus == 6) {
        event.setCancelled(true);
        lock.lock(otherPlayer);
        plugin.displayMessage(player, "Locking " + blockName + " for " + otherPlayer + ".");
      }
    }
  }//end onPlayerInteract();

  // Disallow a locked painting breaking for any other reason (block behind missing, or explosion)  
  @EventHandler(priority = EventPriority.LOW)  
  public void onPaintingBreak(final HangingBreakEvent event) {
    if (event.getEntity().getType().equals(EntityType.ITEM_FRAME)) {

      Lock lock = new Lock(plugin);
      Location blockLoc = event.getEntity().getLocation();
      lock.setLocation(blockLoc);

      if(lock.isLocked()) {
        event.setCancelled(true);
      }
    }
  }

}
