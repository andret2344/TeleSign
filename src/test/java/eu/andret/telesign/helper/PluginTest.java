package eu.andret.telesign.helper;

import eu.andret.telesign.TeleSignPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.block.BlockMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Starts a mocked server with the plugin loaded from the shipped {@code config.yml} before every test.
 */
public abstract class PluginTest {
	protected ServerMock server;
	protected TeleSignPlugin plugin;
	protected WorldMock world;

	@BeforeEach
	void setUpServer() {
		server = MockBukkit.mock();
		plugin = MockBukkit.load(TeleSignPlugin.class);
		world = new TileEntityWorld();
		world.setName("world");
		server.addWorld(world);
	}

	@AfterEach
	void tearDownServer() {
		MockBukkit.unmock();
	}

	/**
	 * Fires the event of the player writing the lines on the front of the block, as when finishing a sign.
	 */
	@NotNull
	protected SignChangeEvent writeSign(@NotNull final Player player, @NotNull final Block block, @NotNull final String... lines) {
		return writeSign(player, block, Side.FRONT, lines);
	}

	/**
	 * Fires the event of the player writing the lines on the given side of the block, as when finishing a sign.
	 */
	@NotNull
	protected SignChangeEvent writeSign(@NotNull final Player player, @NotNull final Block block, @NotNull final Side side,
			@NotNull final String... lines) {
		// Mutable, as on the server: the listener replaces the lines
		final List<Component> components = Arrays.stream(lines)
				.map(Component::text)
				.collect(Collectors.toCollection(ArrayList::new));
		final SignChangeEvent event = new SignChangeEvent(block, player, components, side);
		server.getPluginManager().callEvent(event);
		return event;
	}

	/**
	 * Puts a standing sign at the position, its front facing south (towards +z).
	 */
	@NotNull
	protected Block placeSign(final int x, final int y, final int z) {
		final BlockMock block = world.getBlockAt(x, y, z);
		// On a server a block is only changed in a loaded chunk, MockBukkit loads it on first access
		block.getChunk().load();
		block.setType(Material.OAK_SIGN);
		final Rotatable data = (Rotatable) block.getBlockData();
		data.setRotation(BlockFace.SOUTH);
		block.setBlockData(data);
		// MockBukkit gives a new state default block data, which update() would write back to the block
		final SidedSignStateMock state = new SidedSignStateMock(block);
		state.setBlockData(data);
		block.setState(state);
		return block;
	}

	/**
	 * Puts a sign at the position whose front leads to the given world and coordinates, without the lines written on it.
	 */
	@NotNull
	protected Block placeTeleportSign(final int x, final int y, final int z, @NotNull final String worldName,
			final double targetX, final double targetY, final double targetZ, final float yaw, final float pitch) {
		final Block block = placeSign(x, y, z);
		addDestination(block, Side.FRONT, worldName, targetX, targetY, targetZ, yaw, pitch);
		return block;
	}

	/**
	 * Makes the given side of the sign lead to the given world and coordinates, without the lines written on it.
	 */
	protected void addDestination(@NotNull final Block block, @NotNull final Side side, @NotNull final String worldName,
			final double targetX, final double targetY, final double targetZ, final float yaw, final float pitch) {
		final Sign sign = (Sign) block.getState();
		final PersistentDataContainer pdc = sign.getPersistentDataContainer();
		pdc.set(key(side, "world"), PersistentDataType.STRING, worldName);
		pdc.set(key(side, "x"), PersistentDataType.DOUBLE, targetX);
		pdc.set(key(side, "y"), PersistentDataType.DOUBLE, targetY);
		pdc.set(key(side, "z"), PersistentDataType.DOUBLE, targetZ);
		pdc.set(key(side, "yaw"), PersistentDataType.FLOAT, yaw);
		pdc.set(key(side, "pitch"), PersistentDataType.FLOAT, pitch);
		sign.update();
	}

	@NotNull
	protected NamespacedKey key(@NotNull final Side side, @NotNull final String name) {
		return new NamespacedKey(plugin, side.name().toLowerCase(Locale.ROOT) + "/" + name);
	}

	@NotNull
	protected static String plain(@NotNull final Component component) {
		return PlainTextComponentSerializer.plainText().serialize(component);
	}

	@NotNull
	protected List<String> frontLines(@NotNull final Block block) {
		return lines(block, Side.FRONT);
	}

	@NotNull
	protected List<String> lines(@NotNull final Block block, @NotNull final Side side) {
		return ((Sign) block.getState()).getSide(side).lines()
				.stream()
				.map(PluginTest::plain)
				.toList();
	}

	protected void writeConfig(@NotNull final String content) throws IOException {
		Files.writeString(plugin.getDataFolder().toPath().resolve("config.yml"), content);
	}
}
