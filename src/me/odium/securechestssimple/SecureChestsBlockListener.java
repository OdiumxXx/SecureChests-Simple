package me.odium.securechestssimple;

import org.bukkit.Location;
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
    if(b.getTypeId() == 54) { //make sure block click is a chest.

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

      if (ccN.getBlock().getTypeId() == 54) {
        chestloc = chestloc.subtract(0, 0, 1);
      } else if (ccE.getBlock().getTypeId() == 54) {
        chestloc = chestloc.subtract(1, 0, 0);
      }
      //END double chest detection

      Boolean chestChange = false;
      if (ccN.getBlock().getTypeId() == 54) {
        chestloc = chestloc.subtract(0,0,1);
      } else if (ccE.getBlock().getTypeId() == 54) {
        chestloc = chestloc.subtract(1, 0, 0);
      } else if (ccS.getBlock().getTypeId() == 54) {
        chestloc = chestloc.add(0, 0, 1);
        chestChange = true;
      } else if (ccW.getBlock().getTypeId() == 54) {
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
    }
  }

  @EventHandler(priority = EventPriority.LOW)	
  public void onBlockBreak(final BlockBreakEvent event) {

    Block b = event.getBlock();
    Player player = event.getPlayer();

    //START NEW CODE

    if((SecureChests.BLOCK_LIST.containsKey(b.getTypeId()) && plugin.blockStatus.get(b.getTypeId())) || (b.getLocation().add(0,1,0).getBlock().getTypeId() == 64 && plugin.blockStatus.get(64))) {//check to see if block clicked is on the watch list and is enabled.

      Location blockLoc = b.getLocation();


      if(b.getTypeId() == 54) { //do double chest location corrections
        Location ccN = b.getLocation();
        Location ccE = b.getLocation();
        Location ccS = b.getLocation();
        Location ccW = b.getLocation();

        ccN = ccN.subtract(0,0,1);
        ccE = ccE.subtract(1,0,0);
        ccS = ccS.add(0,0,1);
        ccW = ccW.add(1,0,0);

        //Boolean dchest = false;
        if (ccN.getBlock().getTypeId() == 54) {
          blockLoc = blockLoc.subtract(0, 0, 1);
          //    dchest = true;
        } else if (ccE.getBlock().getTypeId() == 54) {
          blockLoc = blockLoc.subtract(1, 0, 0);
          //    dchest = true;
        } else if (ccS.getBlock().getTypeId() == 54) {
          //    dchest = true;
        } else if (ccW.getBlock().getTypeId() == 54) {
          //    dchest = true;
        }
      } //END Chest location corrections.


      //Start Door Corrections
      if (b.getLocation().add(0,1,0).getBlock().getTypeId() == 64) { // if block above the effected block is a door
        blockLoc = blockLoc.add(0,1,0);
      }

      else if(b.getTypeId() == 64) { //make sure block click is a DOOR
        if (b.getRelative(BlockFace.DOWN).getTypeId() == 64) {
            blockLoc = blockLoc.subtract(0,1,0);
        }
    }
    // End Door Corrections
      
      



      String blockName = SecureChests.BLOCK_LIST.get(b.getTypeId());
      //get name AFTER position corrections!

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
          plugin.displayMessage(player, "Can not open " + blockName + " owned by " + owner + ".");
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
