package me.odium.securechestssimple;

import java.util.Iterator;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class SecureChestsPlayerListener implements Listener{

  public SecureChests plugin;

  public SecureChestsPlayerListener(SecureChests instance) {
    plugin = instance;
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerInteract(final PlayerInteractEvent event) {    
    //make sure we are dealing with a block and not clicking on air or an entity
    if (!(event.getAction().equals(Action.LEFT_CLICK_BLOCK) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK))) {            
      return;
    }

    Block b = event.getClickedBlock();
    Player player = event.getPlayer();

    //START NEW CODE

    if(SecureChests.BLOCK_LIST.containsKey(b.getType()) && plugin.blockStatus.get(b.getType())) {//check to see if block clicked is on the watch list and is enabled.
      Location blockLoc = b.getLocation();
      String blockName = SecureChests.BLOCK_LIST.get(b.getType());

      // IF Chest
      if(b.getType() == Material.CHEST) {
        // Do Chest Location Corrections
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
        // End Chest Corrections
        // Start Door Corrections
      } else if(b.getType() == Material.WOODEN_DOOR) { //make sure block click is a DOOR
        if (b.getRelative(BlockFace.DOWN).getType() == Material.WOODEN_DOOR) {
          blockLoc = blockLoc.subtract(0,1,0);
        }
      }      

      // End Door Corrections

      Lock lock = new Lock(plugin);
      lock.setLocation(blockLoc);        	

      // ACCESS
      // 1= return positive you own this chest
      // 2= return positive you're on one of the access lists
      // 3= return positive. you have bypass ability.
      
      //get the current /sc command status:
      // 0/null=none
      // 1= lock
      // 2= unlock
      // 3= add to chest access list
      // 4= remove from chest access list
      // 5= add to deny list
      // 6= lock for other (perms already checked).
      // 7= check access list
      Integer cmdStatus = plugin.scCmd.remove(player);
      String otherPlayer = plugin.scAList.remove(player);
      if (cmdStatus == null) {
        cmdStatus = 0;
      }

      if(lock.isLocked()) {
        //The block has a locked status. lets now get the owner
        String owner = lock.getOwner();
        Integer access = lock.getAccess(player);
        if(access == 1) { //return positive you own this chest
          if (cmdStatus == 2) {//unlock and stop from further interaction.
            lock.unlock();
            plugin.displayMessage(player, blockName + " unlocked.");
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
          } else if (cmdStatus == 7) {        			  
            Set<String> AccessList = plugin.getStorageConfig().getConfigurationSection(blockLoc.getWorld().getName() + "." + blockLoc.getBlockX() + "_" + blockLoc.getBlockY() + "_" + blockLoc.getBlockZ()+".access").getKeys(false);        			  
            plugin.displayMessage(player, "Access List: ");
            Iterator<String> AccList = AccessList.iterator();
            while(AccList.hasNext())  {
              player.sendMessage("- "+AccList.next());
            }
            event.setCancelled(true);
            return;        			
          } else { // no commands to run just open the chest
            plugin.displayMessage(player, "You own this " + blockName + ".");
            return;
          }
        } else if (access == 2) { //return positive you're on one of the access lists
          if (cmdStatus != 0) { //Trying to run a command on someone else's chest! NO NO!
            plugin.displayMessage(player, "Unable to run command on "+blockName+" owned by: "+owner);
            event.setCancelled(true);
          } else { //No command to run allow them access
            plugin.displayMessage(player, "You have access to " + owner + "'s "+ blockName +".");
          }
          return;
        } else if (access == 3) { //return positive. you have bypass ability.
          if (cmdStatus == 2 && player.hasPermission("securechests.bypass.unlock")) {
            plugin.displayMessage(player, "Bypassing and unlocking "+blockName+" owned by "+owner+".");
            lock.unlock();
            event.setCancelled(true);

          } else {
            plugin.displayMessage(player, "Bypassing lock owned by " + owner + ".");
          }	
          return;

          // CURRENTLY NO ACCESS 4
        } else if (access == 4) { // Access 4 doesn't exist yet

          if(cmdStatus == 3 && player.hasPermission("securechests.bypass.access")) {
            if(lock.addToAccessList(otherPlayer))
              plugin.displayMessage(player, otherPlayer + " added to " + blockName + "'s access list.");
            else
              plugin.displayMessage(player, "Player " + otherPlayer + " already on " + blockName + "'s access list.");
            event.setCancelled(true);
            return;
          } else if(cmdStatus == 7 && player.hasPermission("securechests.bypass.access")) {
            Set<String> AccessList = plugin.getStorageConfig().getConfigurationSection(blockLoc.getWorld().getName() + "." + blockLoc.getBlockX() + "_" + blockLoc.getBlockY() + "_" + blockLoc.getBlockZ()+".access").getKeys(false);               
            plugin.displayMessage(player, "Access List: ");
            Iterator<String> AccList = AccessList.iterator();
            while(AccList.hasNext())  {
              player.sendMessage("- "+AccList.next());
            }
            event.setCancelled(true);
            return;
          } else if (cmdStatus == 4 && player.hasPermission("securechests.bypass.access")) {
            if(lock.removeFromAccessList(otherPlayer))
              plugin.displayMessage(player, otherPlayer + " removed from " + blockName + "'s access list.");
            else
              plugin.displayMessage(player, "Unable to find " + otherPlayer + " on " + blockName + "'s access list.");
            event.setCancelled(true);
            return;
          }       		  
          // END ACCESS 4

        } else if (access == 0) { // return negative, this is someone else's locked chest
          plugin.displayMessage(player, "Can not open " + blockName + " owned by " + owner + ".");
          event.setCancelled(true);
          return;
        }
      } else if (cmdStatus == 1) {
        lock.lock(player.getName());
        plugin.displayMessage(player, "Locking " + blockName + ".");
        event.setCancelled(true);
      } else if (cmdStatus == 6) {
        lock.lock(otherPlayer);
        plugin.displayMessage(player, "Locking " + blockName + " for " + otherPlayer + ".");
        event.setCancelled(true);
      }
    }
  }//end onPlayerInteract();

  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerQuit(final PlayerQuitEvent event) {
    if (plugin.scAList.get(event.getPlayer()) != null) {
      plugin.scAList.remove(event.getPlayer());
    }
    if (plugin.scCmd.get(event.getPlayer()) != null) {
      plugin.scCmd.remove(event.getPlayer());
    }
  }

  // when player left click an item frame
  @EventHandler(priority = EventPriority.LOW)
  public void onEntityDamage(final EntityDamageByEntityEvent event) {
    if (event.getEntity().getType().equals(EntityType.ITEM_FRAME)) {      

      String blockName = "Item Frame";
      Player player = (Player) event.getDamager();
      String otherPlayer = plugin.scAList.remove(player);


      Lock lock = new Lock(plugin);
      Location blockLoc = event.getEntity().getLocation();
      lock.setLocation(blockLoc);
      String owner = lock.getOwner();

      Integer cmdStatus = plugin.scCmd.remove(player);      
      if (cmdStatus == null) {
        cmdStatus = 0;
      }

      if(lock.isLocked()) {  // IF ITEMFRAME IS LOCKED
        if (event.getCause() == DamageCause.ENTITY_ATTACK) {
          Integer access = lock.getAccess(player);
          if(access == 1) { // RETURN POSITIVE, YOU OWN THIS ITEMFRAME
            if (cmdStatus == 2) {  // IF UNLOCKING CHEST
              lock.unlock(); // unlock itemframe           
              event.setCancelled(true);  // unlocking itemframe, so do not damage
              plugin.displayMessage(player, blockName + " unlocked.");
              return;
            } else { // IF YOU OWN LOCKED ITEM FRAME BUT ARE NOT UNLOCKING              
              event.setCancelled(true);  // Not unlocking itemframe, so do not damage.
              plugin.displayMessage(player, "You must unlock "+blockName+" before interacting with it.");
              return;
            }

          } else if (access == 3) { // RETURN POSITIVE, YOU ARE NOT OWNER BUT HAVE BYPASS PERMS
            if (cmdStatus == 2 && player.hasPermission("securechests.bypass.unlock")) {              
              lock.unlock();
              event.setCancelled(true); // Bypassing and unlocking, so do no damage
              plugin.displayMessage(player, "Bypassing and unlocking "+blockName+" owned by "+owner+".");
              return;
            } else if(cmdStatus == 2 && !player.hasPermission("securechests.bypass.unlock")) {
              event.setCancelled(true); // Does not have unlock permission
              plugin.displayMessage(player, "No Permission");
              return;
            } else {
              event.setCancelled(true);  // Not Bypass Unlocking itemframe, so do not damage.
              plugin.displayMessage(player, "You must unlock "+blockName+" owned by "+owner+" before interacting with it.");              
              return;
            }             

          } else { // it is not yours and no bypass
            event.setCancelled(true);  // do not own item frame, so do not damage.
            plugin.displayMessage(player, "Can not break " + blockName + " owned by " + owner + ".");
            return;
          }
        }
        
      } else if (cmdStatus == 1) { // IF ITEM IS NOT LOCKED
        lock.lock(player.getName());
        plugin.displayMessage(player, "Locking " + blockName + ".");
        event.setCancelled(true); // locking item frame, so do no damage
        return;
      } else if (cmdStatus == 6) {
        lock.lock(otherPlayer);
        plugin.displayMessage(player, "Locking " + blockName + " for " + otherPlayer + ".");
        event.setCancelled(true); // locking item frame for other, so do no damage
        return;
      }

    }
  }
}

