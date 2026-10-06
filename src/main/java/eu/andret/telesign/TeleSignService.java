package eu.andret.telesign;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TeleSignService {
	private static final Pattern LOCATION_PATTERN = Pattern.compile("\\[(-?\\d+(\\.\\d+)?),\\s*(-?\\d+(\\.\\d+)?),\\s*(-?\\d+(\\.\\d+)?)(,\\s*(-?\\d+(\\.\\d+)?),\\s*(-?\\d+(\\.\\d+)?))?]");
	private static final Pattern WORLD_PATTERN = Pattern.compile("\\[(\\S+)]");
	private static final Pattern YAW_PITCH_PATTERN = Pattern.compile("\\[(-?\\d+(\\.\\d+)?),\\s*(-?\\d+(\\.\\d+)?)]");
	private static final String HEADER = "[TELESIGN]";
	private static final String LINES = "lines";
	private static final int LINE_COUNT = 4;

	@NotNull
	private final TeleSignPlugin plugin;
	@NotNull
	private final Map<Side, Keys> keys = new EnumMap<>(Side.class);
	@NotNull
	private List<String> lines = List.of();

	public TeleSignService(@NotNull final TeleSignPlugin plugin) {
		this.plugin = plugin;
		for (final Side side : Side.values()) {
			keys.put(side, Keys.of(plugin, side));
		}
	}

	/**
	 * Reads the sign lines from {@code config.yml}. An invalid file throws {@link IllegalArgumentException} with a
	 * readable message and keeps the lines read before.
	 */
	public void reload() {
		final YamlConfiguration config = new YamlConfiguration();
		try {
			config.load(new File(plugin.getDataFolder(), "config.yml"));
		} catch (final IOException | InvalidConfigurationException ex) {
			throw new IllegalArgumentException("Could not read config.yml: " + ex.getMessage(), ex);
		}
		final List<String> configLines = config.getStringList(LINES);
		if (configLines.size() != LINE_COUNT) {
			throw new IllegalArgumentException("'" + LINES + "' in config.yml has to have exactly " + LINE_COUNT
					+ " lines, it has " + configLines.size() + ".");
		}
		lines = List.copyOf(configLines);
	}

	/**
	 * @return the world of the destination of the given side, or {@code null} when that side does not teleport
	 */
	@Nullable
	public String getWorldName(@NotNull final PersistentDataContainer pdc, @NotNull final Side side) {
		return pdc.get(keys.get(side).world(), PersistentDataType.STRING);
	}

	/**
	 * @return whether any side of the sign teleports
	 */
	public boolean isTeleportSign(@NotNull final PersistentDataContainer pdc) {
		return keys.values()
				.stream()
				.anyMatch(sideKeys -> pdc.has(sideKeys.world(), PersistentDataType.STRING));
	}

	/**
	 * @return the destination of the given side, or {@code null} when its world does not exist (anymore)
	 */
	@Nullable
	public Location buildLocation(@NotNull final String worldName, @NotNull final PersistentDataContainer pdc, @NotNull final Side side) {
		final World world = plugin.getServer().getWorld(worldName);
		if (world == null) {
			return null;
		}
		final Keys sideKeys = keys.get(side);
		return new Location(
				world,
				pdc.getOrDefault(sideKeys.x(), PersistentDataType.DOUBLE, 0.0),
				pdc.getOrDefault(sideKeys.y(), PersistentDataType.DOUBLE, 0.0),
				pdc.getOrDefault(sideKeys.z(), PersistentDataType.DOUBLE, 0.0),
				pdc.getOrDefault(sideKeys.yaw(), PersistentDataType.FLOAT, 0.0f),
				pdc.getOrDefault(sideKeys.pitch(), PersistentDataType.FLOAT, 0.0f));
	}

	@Nullable
	public String[] parseSignData(@NotNull final String[] lines) {
		if (!lines[0].equalsIgnoreCase(HEADER)) {
			return null;
		}
		if (lines[1].startsWith("@")) {
			return parsePlayerSignData(lines[1].substring(1));
		}
		final Matcher worldMatcher = WORLD_PATTERN.matcher(lines[1]);
		if (!worldMatcher.find()) {
			return null;
		}
		if (plugin.getServer().getWorld(worldMatcher.group(1)) == null) {
			return null;
		}
		final Matcher locationMatcher = LOCATION_PATTERN.matcher(lines[2]);
		if (!locationMatcher.find()) {
			return null;
		}
		final boolean hasYawPitch = locationMatcher.group(7) != null;
		final String yaw;
		final String pitch;
		if (hasYawPitch) {
			yaw = locationMatcher.group(8);
			pitch = locationMatcher.group(10);
		} else {
			final Matcher yawPitchMatcher = YAW_PITCH_PATTERN.matcher(lines[3]);
			if (yawPitchMatcher.find()) {
				yaw = yawPitchMatcher.group(1);
				pitch = yawPitchMatcher.group(3);
			} else {
				yaw = "0.0";
				pitch = "0.0";
			}
		}
		return new String[]{
				worldMatcher.group(1),
				locationMatcher.group(1),
				locationMatcher.group(3),
				locationMatcher.group(5),
				yaw,
				pitch
		};
	}

	public void applySignData(@NotNull final SignChangeEvent event, @NotNull final PersistentDataContainer pdc, @NotNull final String[] data) {
		final double x = Double.parseDouble(data[1]);
		final double y = Double.parseDouble(data[2]);
		final double z = Double.parseDouble(data[3]);
		final float yaw = Float.parseFloat(data[4]);
		final float pitch = Float.parseFloat(data[5]);
		for (int i = 0; i < lines.size(); i++) {
			event.line(i, createLine(lines.get(i), data[0], x, y, z, yaw, pitch));
		}
		final Keys sideKeys = keys.get(event.getSide());
		pdc.set(sideKeys.world(), PersistentDataType.STRING, data[0]);
		pdc.set(sideKeys.x(), PersistentDataType.DOUBLE, x);
		pdc.set(sideKeys.y(), PersistentDataType.DOUBLE, y);
		pdc.set(sideKeys.z(), PersistentDataType.DOUBLE, z);
		pdc.set(sideKeys.yaw(), PersistentDataType.FLOAT, yaw);
		pdc.set(sideKeys.pitch(), PersistentDataType.FLOAT, pitch);
	}

	public void updateChunk(@NotNull final Chunk chunk) {
		for (final BlockState tileEntity : chunk.getTileEntities()) {
			if (!(tileEntity instanceof final Sign sign)) {
				continue;
			}
			if (!isTeleportSign(sign.getPersistentDataContainer())) {
				continue;
			}
			for (final Side side : Side.values()) {
				updateSide(sign, side);
			}
			sign.update();
		}
	}

	public void updateSigns() {
		for (final World world : plugin.getServer().getWorlds()) {
			for (final Chunk chunk : world.getLoadedChunks()) {
				updateChunk(chunk);
			}
		}
	}

	/**
	 * Renders the lines on the given side of the sign when that side teleports.
	 */
	private void updateSide(@NotNull final Sign sign, @NotNull final Side side) {
		final PersistentDataContainer pdc = sign.getPersistentDataContainer();
		final Keys sideKeys = keys.get(side);
		final String worldName = pdc.get(sideKeys.world(), PersistentDataType.STRING);
		if (worldName == null) {
			return;
		}
		final double x = pdc.getOrDefault(sideKeys.x(), PersistentDataType.DOUBLE, 0.0);
		final double y = pdc.getOrDefault(sideKeys.y(), PersistentDataType.DOUBLE, 0.0);
		final double z = pdc.getOrDefault(sideKeys.z(), PersistentDataType.DOUBLE, 0.0);
		final float yaw = pdc.getOrDefault(sideKeys.yaw(), PersistentDataType.FLOAT, 0.0f);
		final float pitch = pdc.getOrDefault(sideKeys.pitch(), PersistentDataType.FLOAT, 0.0f);
		final SignSide signSide = sign.getSide(side);
		for (int i = 0; i < lines.size(); i++) {
			signSide.line(i, createLine(lines.get(i), worldName, x, y, z, yaw, pitch));
		}
	}

	@Nullable
	private String[] parsePlayerSignData(@NotNull final String playerName) {
		final Player target = plugin.getServer().getPlayerExact(playerName);
		if (target == null) {
			return null;
		}
		final Location loc = target.getLocation();
		return new String[]{
				loc.getWorld().getName(),
				String.valueOf(roundToHalf(loc.getX())),
				String.valueOf(roundToHalf(loc.getY())),
				String.valueOf(roundToHalf(loc.getZ())),
				String.valueOf(loc.getYaw()),
				String.valueOf(loc.getPitch())
		};
	}

	@NotNull
	private static Component createLine(@NotNull final String pattern, @NotNull final String world, final double x, final double y, final double z, final float yaw, final float pitch) {
		return MiniMessage.miniMessage().deserialize(pattern,
				Placeholder.unparsed("world", world),
				Placeholder.unparsed("x", String.valueOf(x)),
				Placeholder.unparsed("y", String.valueOf(y)),
				Placeholder.unparsed("z", String.valueOf(z)),
				Placeholder.unparsed("yaw", String.valueOf(yaw)),
				Placeholder.unparsed("pitch", String.valueOf(pitch)));
	}

	private static double roundToHalf(final double value) {
		return Math.round(value * 2) / 2.0;
	}

	/**
	 * The keys of the destination of one side of a sign, e.g. {@code telesign:front/world}.
	 */
	private record Keys(@NotNull NamespacedKey world, @NotNull NamespacedKey x, @NotNull NamespacedKey y,
			@NotNull NamespacedKey z, @NotNull NamespacedKey yaw, @NotNull NamespacedKey pitch) {
		@NotNull
		static Keys of(@NotNull final TeleSignPlugin plugin, @NotNull final Side side) {
			final String prefix = side.name().toLowerCase(Locale.ROOT) + "/";
			return new Keys(
					new NamespacedKey(plugin, prefix + "world"),
					new NamespacedKey(plugin, prefix + "x"),
					new NamespacedKey(plugin, prefix + "y"),
					new NamespacedKey(plugin, prefix + "z"),
					new NamespacedKey(plugin, prefix + "yaw"),
					new NamespacedKey(plugin, prefix + "pitch"));
		}
	}
}
