package eu.andret.telesign;

import eu.andret.telesign.helper.PluginTest;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.assertj.core.api.Assertions.assertThat;

class TeleSignListenerTest extends PluginTest {
	private PlayerMock player;
	// In front of the signs at 0, 5, 0, which face south
	private Location start;

	@BeforeEach
	void setUpPlayer() {
		player = server.addPlayer();
		player.setOp(true);
		start = new Location(world, 0.5, 5, 3);
		player.setLocation(start);
	}

	// ── createSign ────────────────────────────────────────────────────────────

	@Test
	void createSign() {
		// given
		final Block block = placeSign(0, 5, 0);

		// when
		final SignChangeEvent event = writeSign(player, block, "[TELESIGN]", "[world]", "[1.5, 64, -2.5]", "[90, 45]");

		// then
		assertThat(event.lines()).map(PluginTest::plain).containsExactly("[TELESIGN]", "", "world", "1.5, 64.0, -2.5");
		final PersistentDataContainer pdc = pdc(block);
		assertThat(pdc.get(key(Side.FRONT, "world"), PersistentDataType.STRING)).isEqualTo("world");
		assertThat(pdc.get(key(Side.FRONT, "x"), PersistentDataType.DOUBLE)).isEqualTo(1.5);
		assertThat(pdc.get(key(Side.FRONT, "y"), PersistentDataType.DOUBLE)).isEqualTo(64.0);
		assertThat(pdc.get(key(Side.FRONT, "z"), PersistentDataType.DOUBLE)).isEqualTo(-2.5);
		assertThat(pdc.get(key(Side.FRONT, "yaw"), PersistentDataType.FLOAT)).isEqualTo(90.0f);
		assertThat(pdc.get(key(Side.FRONT, "pitch"), PersistentDataType.FLOAT)).isEqualTo(45.0f);
	}

	@Test
	void createSignIgnoresCase() {
		// given
		final Block block = placeSign(0, 5, 0);

		// when
		writeSign(player, block, "[telesign]", "[world]", "[1, 2, 3]", "");

		// then
		assertThat(pdc(block).get(key(Side.FRONT, "world"), PersistentDataType.STRING)).isEqualTo("world");
	}

	@Test
	void createSignFromPlayer() {
		// given
		final Block block = placeSign(0, 5, 0);
		final PlayerMock target = server.addPlayer("Target");
		target.setLocation(new Location(world, 100.3, 64.7, 200.1, 91.3f, 45.7f));

		// when
		final SignChangeEvent event = writeSign(player, block, "[TELESIGN]", "@Target", "", "");

		// then
		assertThat(event.lines()).map(PluginTest::plain).containsExactly("[TELESIGN]", "", "world", "100.5, 64.5, 200.0");
		final PersistentDataContainer pdc = pdc(block);
		assertThat(pdc.get(key(Side.FRONT, "yaw"), PersistentDataType.FLOAT)).isEqualTo(91.3f);
		assertThat(pdc.get(key(Side.FRONT, "pitch"), PersistentDataType.FLOAT)).isEqualTo(45.7f);
	}

	@Test
	void createSignOnBack() {
		// given
		final Block block = placeSign(0, 5, 0);

		// when
		final SignChangeEvent event = writeSign(player, block, Side.BACK, "[TELESIGN]", "[world]", "[1, 2, 3]", "");

		// then
		assertThat(event.lines()).map(PluginTest::plain).containsExactly("[TELESIGN]", "", "world", "1.0, 2.0, 3.0");
		final PersistentDataContainer pdc = pdc(block);
		assertThat(pdc.get(key(Side.BACK, "world"), PersistentDataType.STRING)).isEqualTo("world");
		assertThat(pdc.get(key(Side.BACK, "x"), PersistentDataType.DOUBLE)).isEqualTo(1.0);
		assertThat(pdc.has(key(Side.FRONT, "world"))).isFalse();
	}

	@Test
	void createRegularSign() {
		// given
		final Block block = placeSign(0, 5, 0);

		// when
		final SignChangeEvent event = writeSign(player, block, "Hello", "[world]", "[1, 2, 3]", "");

		// then
		assertThat(event.lines()).map(PluginTest::plain).containsExactly("Hello", "[world]", "[1, 2, 3]", "");
		assertThat(pdc(block).isEmpty()).isTrue();
	}

	@Test
	void createSignWithoutPermission() {
		// given
		final Block block = placeSign(0, 5, 0);
		player.setOp(false);

		// when
		final SignChangeEvent event = writeSign(player, block, "[TELESIGN]", "[world]", "[1, 2, 3]", "");

		// then
		assertThat(event.lines()).map(PluginTest::plain).containsExactly("[TELESIGN]", "[world]", "[1, 2, 3]", "");
		assertThat(pdc(block).isEmpty()).isTrue();
	}

	@Test
	void createSignOnBlockWithoutData() {
		// given
		final Block block = world.getBlockAt(0, 5, 0);
		block.setType(Material.STONE);

		// when
		final SignChangeEvent event = writeSign(player, block, "[TELESIGN]", "[world]", "[1, 2, 3]", "");

		// then
		assertThat(event.lines()).map(PluginTest::plain).containsExactly("[TELESIGN]", "[world]", "[1, 2, 3]", "");
	}

	@Test
	void createSignCancelledByOtherPlugin() {
		// given
		final Block block = placeSign(0, 5, 0);
		server.getPluginManager().registerEvents(new Listener() {
			@EventHandler
			public void protect(@NotNull final SignChangeEvent event) {
				event.setCancelled(true);
			}
		}, plugin);

		// when
		final SignChangeEvent event = writeSign(player, block, "[TELESIGN]", "[world]", "[1, 2, 3]", "");

		// then
		assertThat(event.lines()).map(PluginTest::plain).containsExactly("[TELESIGN]", "[world]", "[1, 2, 3]", "");
		assertThat(pdc(block).isEmpty()).isTrue();
	}

	// ── clickSign ─────────────────────────────────────────────────────────────

	@Test
	void clickSign() {
		// given
		final Block block = placeTeleportSign(0, 5, 0, "world", 10.5, 70, -3.5, 90, 45);

		// when
		final PlayerInteractEvent event = click(Action.RIGHT_CLICK_BLOCK, block);

		// then
		assertThat(event.useInteractedBlock()).isEqualTo(Event.Result.DENY);
		assertThat(player.getLocation()).isEqualTo(new Location(world, 10.5, 70, -3.5, 90, 45));
	}

	@Test
	void clickSignFromBehind() {
		// given
		final Block block = placeTeleportSign(0, 5, 0, "world", 10.5, 70, -3.5, 90, 45);
		final Location behind = new Location(world, 0.5, 5, -2);
		player.setLocation(behind);

		// when
		final PlayerInteractEvent event = click(Action.RIGHT_CLICK_BLOCK, block);

		// then
		assertThat(event.useInteractedBlock()).isNotEqualTo(Event.Result.DENY);
		assertThat(player.getLocation()).isEqualTo(behind);
	}

	@Test
	void clickSignWithTwoDestinations() {
		// given
		final Block block = placeTeleportSign(0, 5, 0, "world", 10.5, 70, -3.5, 90, 45);
		addDestination(block, Side.BACK, "world", -20.5, 80, 7.5, 180, 0);
		player.setLocation(new Location(world, 0.5, 5, -2));

		// when
		click(Action.RIGHT_CLICK_BLOCK, block);

		// then
		assertThat(player.getLocation()).isEqualTo(new Location(world, -20.5, 80, 7.5, 180, 0));
	}

	@Test
	void clickSignToMissingWorld() {
		// given
		final Block block = placeTeleportSign(0, 5, 0, "gone", 10.5, 70, -3.5, 90, 45);

		// when
		final PlayerInteractEvent event = click(Action.RIGHT_CLICK_BLOCK, block);

		// then
		assertThat(event.useInteractedBlock()).isEqualTo(Event.Result.DENY);
		assertThat(player.getLocation()).isEqualTo(start);
		assertThat(plain(player.nextComponentMessage())).isEqualTo("The world \"gone\" of this sign does not exist.");
	}

	@Test
	void clickSignWithoutPermission() {
		// given
		final Block block = placeTeleportSign(0, 5, 0, "world", 10.5, 70, -3.5, 90, 45);
		player.addAttachment(plugin, "telesign.use", false);

		// when
		final PlayerInteractEvent event = click(Action.RIGHT_CLICK_BLOCK, block);

		// then
		assertThat(event.useInteractedBlock()).isNotEqualTo(Event.Result.DENY);
		assertThat(player.getLocation()).isEqualTo(start);
	}

	@Test
	void clickSignWithLeftClick() {
		// given
		final Block block = placeTeleportSign(0, 5, 0, "world", 10.5, 70, -3.5, 90, 45);

		// when
		final PlayerInteractEvent event = click(Action.LEFT_CLICK_BLOCK, block);

		// then
		assertThat(event.useInteractedBlock()).isNotEqualTo(Event.Result.DENY);
		assertThat(player.getLocation()).isEqualTo(start);
	}

	@Test
	void clickNoBlock() {
		// given
		final PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, null, BlockFace.NORTH, EquipmentSlot.HAND);
		// Without a block the event starts cancelled
		event.setUseInteractedBlock(Event.Result.ALLOW);

		// when
		server.getPluginManager().callEvent(event);

		// then
		assertThat(event.useInteractedBlock()).isEqualTo(Event.Result.ALLOW);
		assertThat(player.getLocation()).isEqualTo(start);
	}

	@Test
	void clickBlockWithoutData() {
		// given
		final Block block = world.getBlockAt(0, 5, 0);
		block.setType(Material.STONE);

		// when
		final PlayerInteractEvent event = click(Action.RIGHT_CLICK_BLOCK, block);

		// then
		assertThat(event.useInteractedBlock()).isNotEqualTo(Event.Result.DENY);
	}

	@Test
	void clickRegularSign() {
		// given
		final Block block = placeSign(0, 5, 0);

		// when
		final PlayerInteractEvent event = click(Action.RIGHT_CLICK_BLOCK, block);

		// then
		assertThat(event.useInteractedBlock()).isNotEqualTo(Event.Result.DENY);
		assertThat(player.getLocation()).isEqualTo(start);
	}

	@Test
	void clickSignCancelledByOtherPlugin() {
		// given
		final Block block = placeTeleportSign(0, 5, 0, "world", 10.5, 70, -3.5, 90, 45);
		server.getPluginManager().registerEvents(new Listener() {
			@EventHandler
			public void protect(@NotNull final PlayerInteractEvent event) {
				event.setCancelled(true);
			}
		}, plugin);

		// when
		click(Action.RIGHT_CLICK_BLOCK, block);

		// then
		assertThat(player.getLocation()).isEqualTo(start);
	}

	// ── breakBlock ────────────────────────────────────────────────────────────

	@Test
	void breakSign() {
		// given
		final Block block = placeTeleportSign(0, 5, 0, "world", 10.5, 70, -3.5, 90, 45);

		// when
		final BlockBreakEvent event = breakBlock(block);

		// then
		assertThat(event.isCancelled()).isFalse();
	}

	@Test
	void breakSignWithoutPermission() {
		// given
		final Block block = placeTeleportSign(0, 5, 0, "world", 10.5, 70, -3.5, 90, 45);
		player.setOp(false);

		// when
		final BlockBreakEvent event = breakBlock(block);

		// then
		assertThat(event.isCancelled()).isTrue();
	}

	@Test
	void breakSignWithBackDestinationWithoutPermission() {
		// given
		final Block block = placeSign(0, 5, 0);
		addDestination(block, Side.BACK, "world", 10.5, 70, -3.5, 90, 45);
		player.setOp(false);

		// when
		final BlockBreakEvent event = breakBlock(block);

		// then
		assertThat(event.isCancelled()).isTrue();
	}

	@Test
	void breakRegularSignWithoutPermission() {
		// given
		final Block block = placeSign(0, 5, 0);
		player.setOp(false);

		// when
		final BlockBreakEvent event = breakBlock(block);

		// then
		assertThat(event.isCancelled()).isFalse();
	}

	@Test
	void breakBlockWithoutData() {
		// given
		final Block block = world.getBlockAt(0, 5, 0);
		block.setType(Material.STONE);
		player.setOp(false);

		// when
		final BlockBreakEvent event = breakBlock(block);

		// then
		assertThat(event.isCancelled()).isFalse();
	}

	// ── chunkLoad ─────────────────────────────────────────────────────────────

	@Test
	void chunkLoadUpdatesSigns() {
		// given
		final Block block = placeTeleportSign(0, 5, 0, "world", 10.5, 70, -3.5, 90, 45);

		// when
		server.getPluginManager().callEvent(new ChunkLoadEvent(block.getChunk(), false));

		// then
		assertThat(frontLines(block)).containsExactly("[TELESIGN]", "", "world", "10.5, 70.0, -3.5");
	}

	@NotNull
	private PersistentDataContainer pdc(@NotNull final Block block) {
		return ((Sign) block.getState()).getPersistentDataContainer();
	}

	@NotNull
	private PlayerInteractEvent click(@NotNull final Action action, @NotNull final Block block) {
		final PlayerInteractEvent event = new PlayerInteractEvent(player, action, null, block, BlockFace.NORTH, EquipmentSlot.HAND);
		server.getPluginManager().callEvent(event);
		return event;
	}

	@NotNull
	private BlockBreakEvent breakBlock(@NotNull final Block block) {
		final BlockBreakEvent event = new BlockBreakEvent(block, player);
		server.getPluginManager().callEvent(event);
		return event;
	}
}
