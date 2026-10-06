package eu.andret.telesign;

import eu.andret.telesign.helper.PluginTest;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class TeleSignServiceTest extends PluginTest {
	private TeleSignService service;

	@BeforeEach
	void setUpService() {
		service = new TeleSignService(plugin);
		service.reload();
	}

	// ── reload ────────────────────────────────────────────────────────────────

	@Test
	void reloadReadsNewLines() throws IOException {
		// given
		final Block block = placeTeleportSign(0, 5, 0, "world", 10.5, 70, -3.5, 90, 45);
		writeConfig("""
				lines:
				  - '<gold>Go to'
				  - '<world>'
				  - '<x> <y> <z>'
				  - '<yaw> <pitch>'
				""");

		// when
		service.reload();
		service.updateChunk(block.getChunk());

		// then
		assertThat(frontLines(block)).containsExactly("Go to", "world", "10.5 70.0 -3.5", "90.0 45.0");
	}

	@Test
	void reloadInvalidYaml() throws IOException {
		// given
		final Block block = placeTeleportSign(0, 5, 0, "world", 10.5, 70, -3.5, 90, 45);
		writeConfig("lines: [");

		// when
		assertThatIllegalArgumentException()
				.isThrownBy(service::reload)
				.withMessageStartingWith("Could not read config.yml: ");
		service.updateChunk(block.getChunk());

		// then
		assertThat(frontLines(block)).containsExactly("[TELESIGN]", "", "world", "10.5, 70.0, -3.5");
	}

	@Test
	void reloadWrongNumberOfLines() throws IOException {
		// given
		writeConfig("""
				lines:
				  - 'one'
				  - 'two'
				  - 'three'
				""");

		// when / then
		assertThatIllegalArgumentException()
				.isThrownBy(service::reload)
				.withMessage("'lines' in config.yml has to have exactly 4 lines, it has 3.");
	}

	@Test
	void reloadWithoutLines() throws IOException {
		// given
		writeConfig("something: else");

		// when / then
		assertThatIllegalArgumentException()
				.isThrownBy(service::reload)
				.withMessage("'lines' in config.yml has to have exactly 4 lines, it has 0.");
	}

	@Test
	void lineWithWorldNameIsNotParsed() {
		// given
		final Block block = placeTeleportSign(0, 5, 0, "<red>world", 10.5, 70, -3.5, 90, 45);

		// when
		service.updateChunk(block.getChunk());

		// then
		assertThat(frontLines(block)).containsExactly("[TELESIGN]", "", "<red>world", "10.5, 70.0, -3.5");
	}

	// ── buildLocation ─────────────────────────────────────────────────────────

	@Test
	void buildLocation() {
		// given
		final PersistentDataContainer pdc = pdc(placeTeleportSign(0, 5, 0, "world", 1.4, 1.5, 1.6, 45, 30));

		// when
		final Location location = service.buildLocation("world", pdc, Side.FRONT);

		// then
		assertThat(location).isEqualTo(new Location(world, 1.4, 1.5, 1.6, 45, 30));
	}

	@Test
	void buildLocationOfBack() {
		// given
		final Block block = placeTeleportSign(0, 5, 0, "world", 1.4, 1.5, 1.6, 45, 30);
		addDestination(block, Side.BACK, "world", -7.5, 90, 2.5, 180, -10);

		// when
		final Location location = service.buildLocation("world", pdc(block), Side.BACK);

		// then
		assertThat(location).isEqualTo(new Location(world, -7.5, 90, 2.5, 180, -10));
	}

	@Test
	void buildLocationInMissingWorld() {
		// given
		final PersistentDataContainer pdc = pdc(placeTeleportSign(0, 5, 0, "gone", 1.4, 1.5, 1.6, 45, 30));

		// when
		final Location location = service.buildLocation("gone", pdc, Side.FRONT);

		// then
		assertThat(location).isNull();
	}

	// ── parseSignData ─────────────────────────────────────────────────────────

	@Test
	void parseSignDataEmpty() {
		// when / then
		assertThat(service.parseSignData(new String[]{"", "", "", ""})).isNull();
	}

	@Test
	void parseSignDataWithOldHeader() {
		// when / then
		assertThat(service.parseSignData(new String[]{"[TELEPORT]", "[world]", "[1, 1, 1]", ""})).isNull();
	}

	@Test
	void parseSignDataWithNoWorld() {
		// when / then
		assertThat(service.parseSignData(new String[]{"[TELESIGN]", "world", "[1, 1, 1]", ""})).isNull();
	}

	@Test
	void parseSignDataWithUnknownWorld() {
		// when / then
		assertThat(service.parseSignData(new String[]{"[TELESIGN]", "[gone]", "[1, 1, 1]", ""})).isNull();
	}

	@Test
	void parseSignDataWithWrongCoords() {
		// when / then
		assertThat(service.parseSignData(new String[]{"[TELESIGN]", "[world]", "[1]", ""})).isNull();
	}

	@Test
	void parseSignDataWithCoords() {
		// when
		final String[] result = service.parseSignData(new String[]{"[TELESIGN]", "[world]", "[1.4, -1.5, 1.6]", ""});

		// then
		assertThat(result).containsExactly("world", "1.4", "-1.5", "1.6", "0.0", "0.0");
	}

	@Test
	void parseSignDataWithYawAndPitchInCoords() {
		// when
		final String[] result = service.parseSignData(new String[]{"[TELESIGN]", "[world]", "[1.4, 1.5, 1.6, 90.0, -45.0]", "[1, 2]"});

		// then
		assertThat(result).containsExactly("world", "1.4", "1.5", "1.6", "90.0", "-45.0");
	}

	@Test
	void parseSignDataWithYawAndPitchInLastLine() {
		// when
		final String[] result = service.parseSignData(new String[]{"[TELESIGN]", "[world]", "[1.4, 1.5, 1.6]", "[90.0, 45.0]"});

		// then
		assertThat(result).containsExactly("world", "1.4", "1.5", "1.6", "90.0", "45.0");
	}

	@Test
	void parseSignDataFromOfflinePlayer() {
		// when / then
		assertThat(service.parseSignData(new String[]{"[TELESIGN]", "@somePlayer", "", ""})).isNull();
	}

	// ── updateSigns / updateChunk ─────────────────────────────────────────────

	@Test
	void updateSigns() {
		// given
		final Block block = placeTeleportSign(0, 5, 0, "world", 10.5, 70, -3.5, 90, 45);

		// when
		service.updateSigns();

		// then
		assertThat(frontLines(block)).containsExactly("[TELESIGN]", "", "world", "10.5, 70.0, -3.5");
	}

	@Test
	void updateChunkKeepsSideWithoutDestination() {
		// given
		final Block block = placeSign(0, 5, 0);
		final Sign sign = (Sign) block.getState();
		sign.getSide(Side.FRONT).line(0, Component.text("Hello"));
		sign.update();
		addDestination(block, Side.BACK, "world", 10.5, 70, -3.5, 90, 45);

		// when
		service.updateChunk(block.getChunk());

		// then
		assertThat(frontLines(block)).containsExactly("Hello", "", "", "");
		assertThat(lines(block, Side.BACK)).containsExactly("[TELESIGN]", "", "world", "10.5, 70.0, -3.5");
	}

	@Test
	void updateChunkUpdatesBothSides() {
		// given
		final Block block = placeTeleportSign(0, 5, 0, "world", 10.5, 70, -3.5, 90, 45);
		addDestination(block, Side.BACK, "world", -7.5, 90, 2.5, 180, -10);

		// when
		service.updateChunk(block.getChunk());

		// then
		assertThat(frontLines(block)).containsExactly("[TELESIGN]", "", "world", "10.5, 70.0, -3.5");
		assertThat(lines(block, Side.BACK)).containsExactly("[TELESIGN]", "", "world", "-7.5, 90.0, 2.5");
	}

	@Test
	void updateChunkSkipsOtherBlocks() {
		// given
		final Block chest = world.getBlockAt(0, 5, 0);
		chest.setType(Material.CHEST);
		final Block block = world.getBlockAt(1, 5, 0);
		block.setType(Material.OAK_SIGN);
		final Sign sign = (Sign) block.getState();
		sign.getSide(Side.FRONT).line(0, Component.text("Hello"));
		sign.update();

		// when
		service.updateChunk(block.getChunk());

		// then
		assertThat(frontLines(block)).containsExactly("Hello", "", "", "");
	}

	@NotNull
	private static PersistentDataContainer pdc(@NotNull final Block block) {
		return ((Sign) block.getState()).getPersistentDataContainer();
	}
}
