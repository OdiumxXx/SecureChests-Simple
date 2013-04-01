package me.odium.securechestssimple;

import java.util.Iterator;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
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
        if (!(event.getAction().equals(Action.LEFT_CLICK_BLOCK) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK)))
            return;


        Block b = event.getClickedBlock();
        Player player = event.getPlayer();

        //START NEW CODE
        
        if(SecureChests.BLOCK_LIST.containsKey(b.getTypeId()) && plugin.blockStatus.get(b.getTypeId())) {//check to see if block clicked is on the watch list and is enabled.
        	Location blockLoc = b.getLocation();
        	String blockName = SecureChests.BLOCK_LIST.get(b.getTypeId());
        	
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
        	
       // Start Door Corrections
          else if(b.getTypeId() == 64) { //make sure block click is a DOOR
              if (b.getRelative(BlockFace.DOWN).getTypeId() == 64) {
                  blockLoc = blockLoc.subtract(0,1,0);
              }
          }
          // End Door Corrections
        	
        	Lock lock = new Lock(plugin);
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
        		//The block has a locked status. lets now get the owner
        		String owner = lock.getOwner();
        		Integer access = lock.getAccess(player);
        		if(access == 1) { //it's yours yay!
        			if (cmdStatus == 2) {//unlock and stop from further interacton.
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
        		} else if (access == 2) { //you're on the allow list
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

        		} else if (access == 4) {

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

        		} else {
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
}
