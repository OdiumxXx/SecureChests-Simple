package me.odium.securechestssimple;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.material.Door;

public class SecureChestsExplosionListener implements Listener {

  public SecureChests plugin;

  public SecureChestsExplosionListener(SecureChests instance) {
    plugin = instance;
  }
  // ON AN EXPLOSION EVENT
  @EventHandler(priority = EventPriority.LOW) 
  public void onExplosionEvent(final EntityExplodeEvent event) {
    // GET BLOCKS EFFECT BY AN EXPLOSION
    List<Block> blockList = event.blockList();
    int len = blockList.size();
    // FOR EVERY BLOCK EFFECTED BY EXPLOSION
    for(int i = 0; i < len; i++) {
      Block b = blockList.get(i);
      int bId = b.getTypeId();
      Location blockLoc = b.getLocation();

      if (SecureChests.BLOCK_LIST.containsKey(bId)) {

        // IF BLOCK IS DOOR SELECT BOTTOM HALF
        if(b.getTypeId() == 64) {
          Door d = (Door)b.getState().getData();          
          if (d.isTopHalf()) { 
            blockLoc = b.getLocation().subtract(0,1,0);
          }
        }        

        Lock lock = new Lock(plugin);
        lock.setLocation(blockLoc);

        // IF THING IS LOCKED REMOVE FROM EXPLOSION DAMAGE LIST
        if(lock.isLocked()) {
          blockList.remove(i);
          i--;
          len--;
          // IF BLOCK IS CHEST
          if (bId == 54) {
            Location ccN = b.getLocation();
            Location ccE = b.getLocation();
            Location ccS = b.getLocation();
            Location ccW = b.getLocation();
            ccN = ccN.subtract(0,0,1);
            ccE = ccE.subtract(1,0,0);
            ccS = ccS.add(0,0,1);
            ccW = ccW.add(1,0,0);
            // IF DOUBLE CHEST, REMOVE BOTH HALVES FROM EXPLOSIN DAMAGE LIST          
            if (ccN.getBlock().getTypeId() == 54) {
              blockList.remove(i);
              i--;
              len--;            
            } else if (ccE.getBlock().getTypeId() == 54) {
              blockList.remove(i);
              i--;
              len--;            
            } else if (ccS.getBlock().getTypeId() == 54) {
              blockList.remove(i);
              i--;
              len--;            
            } else if (ccW.getBlock().getTypeId() == 54) {
              blockList.remove(i);
              i--;
              len--;   
            } 
          }
        } 
      }
      // IF BLOCK ABOVE IS A DOOR, REMOVE FROM EXPLOSION DAMAGE LIST
      if (b.getLocation().add(0,1,0).getBlock().getTypeId() == 64) {
        blockList.remove(i);
        i--;
        len--;
      }
    }
  }
}

