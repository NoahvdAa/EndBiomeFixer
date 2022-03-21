package me.noahvdaa.endbiomefixer.endbiomefixer;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EndBiomeFixer extends JavaPlugin implements Listener {

    private static final List<Biome> END_BIOMES = List.of(Biome.END_BARRENS, Biome.END_HIGHLANDS, Biome.END_MIDLANDS, Biome.THE_END, Biome.SMALL_END_ISLANDS);

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        Chunk chunk = e.getChunk();
        World world = e.getWorld();
        if (world.getEnvironment() != World.Environment.THE_END) return;
        Biome currentBiome = chunk.getBlock(7, (world.getMinHeight() + world.getMaxHeight()) / 2, 7).getBiome(); // Grab a nice block in the middle.
        if (END_BIOMES.contains(currentBiome)) return; // This chunk is fine.

        // Calculate biome async to prevent tps lag.
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            // Use a map so the scheduler doesn't crap itself scheduling 65K tasks per chunk.
            Map<Block, Biome> setQueue = new HashMap<>();
            for (int x = 0; x < 15; x++) {
                for (int z = 0; z < 15; z++) {
                    for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                        Biome biome = world.vanillaBiomeProvider().getBiome(world, x, y, z);

                        setQueue.put(chunk.getBlock(x, y, z), biome);
                    }
                }
            }

            // Set the blocks synchronously.
            getServer().getScheduler().runTask(this, () -> {
                for (Map.Entry<Block, Biome> entry : setQueue.entrySet()) {
                    entry.getKey().setBiome(entry.getValue());
                }
            });

            if (getConfig().getBoolean("logChunkFixes", false)) {
                getLogger().info("Fixing biomes for end chunk: " + chunk.getX() + ", " + chunk.getZ() + "...");
            }
        });
    }

}
