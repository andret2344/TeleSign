package eu.andret.telesign.helper;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.jetbrains.annotations.NotNull;
import org.mockbukkit.mockbukkit.world.ChunkCoordinate;
import org.mockbukkit.mockbukkit.world.ChunkMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.Arrays;

/**
 * A MockBukkit world whose chunks implement {@link Chunk#getTileEntities()}, which MockBukkit leaves unimplemented.
 */
class TileEntityWorld extends WorldMock {
	@NotNull
	@Override
	public ChunkMock getChunkAt(@NotNull final ChunkCoordinate coordinate) {
		final ChunkMock chunk = super.getChunkAt(coordinate);
		return new TileEntityChunk(this, chunk.getX(), chunk.getZ());
	}

	@Override
	public Chunk @NotNull [] getLoadedChunks() {
		return Arrays.stream(super.getLoadedChunks())
				.map(chunk -> new TileEntityChunk(this, chunk.getX(), chunk.getZ()))
				.toArray(Chunk[]::new);
	}

	private static class TileEntityChunk extends ChunkMock {
		TileEntityChunk(@NotNull final World world, final int x, final int z) {
			super(world, x, z);
		}

		@Override
		public BlockState @NotNull [] getTileEntities() {
			return getBlocks()
					.stream()
					.map(Block::getState)
					.filter(TileState.class::isInstance)
					.toArray(BlockState[]::new);
		}
	}
}
