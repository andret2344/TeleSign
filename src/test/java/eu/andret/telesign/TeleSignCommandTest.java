package eu.andret.telesign;

import eu.andret.telesign.helper.PluginTest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class TeleSignCommandTest extends PluginTest {
	private PlayerMock player;

	@BeforeEach
	void setUpPlayer() {
		player = server.addPlayer();
		player.setOp(true);
	}

	@Test
	void reload() throws IOException {
		// given
		final Block block = placeTeleportSign(0, 5, 0, "world", 10.5, 70, -3.5, 90, 45);
		writeConfig("""
				lines:
				  - '<gold>Go to'
				  - '<world>'
				  - '<x> <y> <z>'
				  - ''
				""");

		// when
		player.performCommand("telesign reload");

		// then
		assertThat(player.nextComponentMessage())
				.isEqualTo(Component.text("TeleSign configuration reloaded.", NamedTextColor.GREEN));
		assertThat(frontLines(block)).containsExactly("Go to", "world", "10.5 70.0 -3.5", "");
	}

	@Test
	void reloadWithAliasIgnoresCase() {
		// when
		player.performCommand("tsigns RELOAD");

		// then
		assertThat(player.nextComponentMessage())
				.isEqualTo(Component.text("TeleSign configuration reloaded.", NamedTextColor.GREEN));
	}

	@Test
	void reloadInvalidConfig() throws IOException {
		// given
		final Block block = placeTeleportSign(0, 5, 0, "world", 10.5, 70, -3.5, 90, 45);
		writeConfig("lines: []");

		// when
		player.performCommand("telesign reload");

		// then
		assertThat(player.nextComponentMessage()).isEqualTo(Component.text(
				"'lines' in config.yml has to have exactly 4 lines, it has 0.", NamedTextColor.RED));
		assertThat(player.nextComponentMessage()).isNull();
		assertThat(frontLines(block)).containsExactly("", "", "", "");
	}

	@Test
	void noArguments() {
		// when
		player.performCommand("telesign");

		// then
		assertThat(player.nextComponentMessage()).isEqualTo(usage());
	}

	@Test
	void unknownSubcommand() {
		// when
		player.performCommand("telesign foo");

		// then
		assertThat(player.nextComponentMessage()).isEqualTo(usage());
	}

	@Test
	void tooManyArguments() {
		// when
		player.performCommand("telesign reload now");

		// then
		assertThat(player.nextComponentMessage()).isEqualTo(usage());
	}

	@NotNull
	private static Component usage() {
		return Component.text("Error: ", NamedTextColor.DARK_RED)
				.append(Component.text("Usage: /telesign reload", NamedTextColor.RED));
	}
}
