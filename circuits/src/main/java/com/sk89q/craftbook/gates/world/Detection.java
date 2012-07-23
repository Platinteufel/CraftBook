package com.sk89q.craftbook.gates.world;

import java.util.LinkedHashSet;
import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.PoweredMinecart;
import org.bukkit.entity.StorageMinecart;

import com.sk89q.craftbook.ic.AbstractIC;
import com.sk89q.craftbook.ic.AbstractICFactory;
import com.sk89q.craftbook.ic.ChipState;
import com.sk89q.craftbook.ic.IC;
import com.sk89q.craftbook.ic.RestrictedIC;
import com.sk89q.craftbook.util.EnumUtil;
import com.sk89q.craftbook.util.SignUtil;

/**
 * @author Silthus
 */
public class Detection extends AbstractIC {

    private enum Type {
        PLAYER,
        MOBHOSTILE,
        MOBPEACEFUL,
        ANYMOB,
        ANY,
        CART,
        STORAGECART,
        POWEREDCART;

        public boolean is(Entity entity) {

            switch (this) {
            case PLAYER:
                return entity instanceof Player;
            case MOBHOSTILE:
                return entity instanceof Monster;
            case MOBPEACEFUL:
                return entity instanceof Animals;
            case ANYMOB:
                return entity instanceof Creature;
            case CART:
                return entity instanceof Minecart;
            case STORAGECART:
                return entity instanceof StorageMinecart;
            case POWEREDCART:
                return entity instanceof PoweredMinecart;
            case ANY:
                return true;
            }
            return false;
        }

        public static Type fromString(String name) {
            return EnumUtil.getEnumFromString(Detection.Type.class, name);
        }
    }

    private Type type;

	private Location center;
	private Set<Chunk> chunks;
    private int radius;

    public Detection(Server server, Sign block) {
        super(server, block);
        // lets set some defaults
        radius = 0;
        load();
    }

    private void load() {
        Sign sign = getSign();
	    Block block = SignUtil.getBackBlock(sign.getBlock());
	    // lets get the type to detect first
	    this.type = Type.fromString(sign.getLine(3).trim());
	    // set the type to any if wrong format
	    if (type == null) this.type = Type.ANY;
	    // update the sign with correct upper case name
	    sign.setLine(3, type.name());
	    sign.update();
	    // now check the third line for the radius and offset
	    String line = sign.getLine(2).trim();
	    // if the line contains a = the offset is given
	    // the given string should look something like that:
	    // radius=x:y:z or radius, e.g. 1=-2:5:11
	    if (line.contains("=")) {
	        try {
	            String[] split = line.split("=");
	            this.radius = Integer.parseInt(split[0]);
	            // parse the offset
	            String[] offsetSplit = split[1].split(":");
                int offsetX = Integer.parseInt(offsetSplit[0]);
                int offsetY = Integer.parseInt(offsetSplit[1]);
                int offsetZ = Integer.parseInt(offsetSplit[2]);
		        block = block.getWorld().getBlockAt(block.getX() + offsetX, block.getY() + offsetY, block.getZ() + offsetZ);
            } catch (NumberFormatException e) {
                // do nothing and use the defaults
            } catch (IndexOutOfBoundsException e) {
                // do nothing and use the defaults
            }
        } else {
            this.radius = Integer.parseInt(line);
        }
	    this.center = block.getLocation();
	    this.chunks = getSurroundingChunks(block.getChunk(), ((radius / 16) + 1));
    }

    @Override
    public String getTitle() {
        return "Detection";
    }

    @Override
    public String getSignTitle() {
        return "DETECTION";
    }

    @Override
    public void trigger(ChipState chip) {
        if (chip.getInput(0)) {
            chip.setOutput(0, isDetected());
        }
    }

    protected boolean isDetected() {
        for (Chunk chunk : this.chunks) {
            if (chunk.isLoaded()) {
                // get all entites from the chunks in the defined radius
                for (Entity entity : chunk.getEntities()) {
                    if (!entity.isDead()) {
                        if (type.is(entity)) {
                            // at last check if the entity is within the radius
                            if (getGreatestDistance(entity.getLocation(), center) <= radius) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

	private Set<Chunk> getSurroundingChunks(Chunk chunk, int radius) {
		Set<Chunk> chunks = new LinkedHashSet<Chunk>();
		World world = chunk.getWorld();
		int cX = chunk.getX();
		int cZ = chunk.getZ();
		for (int x = radius; x >= 0; x--) {
			for (int z = radius; z >= 0; z--) {
				chunks.add(world.getChunkAt(cX + x, cZ + z));
				chunks.add(world.getChunkAt(cX - x, cZ - z));
			}
		}
		return chunks;
	}

    private Set<Chunk> getSurroundingChunks(Location loc, int radius) {
        Set<Chunk> chunks = new LinkedHashSet<Chunk>();
        Chunk chunk = loc.getChunk();
        chunks.add(chunk);
        // get the block the furthest away
        loc.add(radius, 0, radius);
        // add the chunk
        chunks.add(loc.getChunk());
	    chunks.addAll(getChunksBetween(chunk, loc.getChunk()));
	    loc.subtract(radius * 2, 0, radius * 2);
	    chunks.addAll(getChunksBetween(chunk, loc.getChunk()));
        return chunks;
    }

	private Set<Chunk> getChunksBetween(Chunk chunk1, Chunk chunk2) {
		Set<Chunk> chunks = new LinkedHashSet<Chunk>();
		// get the x, z difference between the two chunks then...
		int z = 0;
		// ...iterate over all chunks in between
		for (int x = chunk1.getX() - chunk2.getX(); x > 0; x--) {
			// add all surrounding chunks one by one
			chunks.add(chunk1.getWorld().getChunkAt(chunk1.getX() + x, chunk1.getZ() + z));
			for (z = chunk1.getZ() - chunk2.getZ(); z > 0; z--) {
				chunks.add(chunk1.getWorld().getChunkAt(chunk1.getX() + x, chunk1.getZ() + z));
			}
		}
		return chunks;
	}

    public static int getGreatestDistance(Location l1, Location l2) {
        int x = Math.abs(l1.getBlockX() - l2.getBlockX());
        int y = Math.abs(l1.getBlockY() - l2.getBlockY());
        int z = Math.abs(l1.getBlockZ() - l2.getBlockZ());
        if (x >= y && x >= z) {
            return x;
        } else if (y >= x && y >= z) {
            return y;
        } else if (z >= x && z >= y) {
            return z;
        } else {
            return x;
        }
    }

    public static class Factory extends AbstractICFactory implements RestrictedIC {

        public Factory(Server server) {
            super(server);
        }

        @Override
        public IC create(Sign sign) {
            return new Detection(getServer(), sign);
        }
    }
}
