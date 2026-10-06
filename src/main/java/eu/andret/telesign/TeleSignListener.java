package eu.andret.telesign;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.jetbrains.annotations.NotNull;

public class TeleSignListener implements Listener {
	@NotNull
	private final TeleSignService service;

	public TeleSignListener(@NotNull final TeleSignService service) {
		this.service = service;
	}

	// HIGH and ignoreCancelled: region protections cancelling at NORMAL or lower stop the teleport
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void clickSign(@NotNull final PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}
		final Block block = event.getClickedBlock();
		if (block == null) {
			return;
		}
		if (!(block.getState() instanceof final Sign sign)) {
			return;
		}
		final Player player = event.getPlayer();
		// Each side has its own destination, the player uses the side they face
		final Side side = sign.getInteractableSideFor(player);
		final PersistentDataContainer pdc = sign.getPersistentDataContainer();
		final String worldName = service.getWorldName(pdc, side);
		if (worldName == null) {
			return;
		}
		if (!player.hasPermission("telesign.use")) {
			return;
		}
		event.setCancelled(true);
		final Location location = service.buildLocation(worldName, pdc, side);
		if (location == null) {
			player.sendMessage(Component.text("The world \"" + worldName + "\" of this sign does not exist.", NamedTextColor.RED));
			return;
		}
		player.teleport(location);
	}

	@EventHandler(ignoreCancelled = true)
	public void breakBlock(@NotNull final BlockBreakEvent event) {
		if (!(event.getBlock().getState() instanceof final PersistentDataHolder holder)) {
			return;
		}
		if (!service.isTeleportSign(holder.getPersistentDataContainer())) {
			return;
		}
		if (!event.getPlayer().hasPermission("telesign.break")) {
			event.setCancelled(true);
		}
	}

	// HIGH and ignoreCancelled: a sign is not turned into a teleport sign where a protection forbids the change
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void createSign(@NotNull final SignChangeEvent event) {
		final String[] lines = event.lines()
				.stream()
				.map(PlainTextComponentSerializer.plainText()::serialize)
				.toArray(String[]::new);
		final String[] data = service.parseSignData(lines);
		if (data == null) {
			return;
		}
		if (!event.getPlayer().hasPermission("telesign.create")) {
			return;
		}
		final BlockState state = event.getBlock().getState();
		if (!(state instanceof final PersistentDataHolder holder)) {
			return;
		}
		service.applySignData(event, holder.getPersistentDataContainer(), data);
		state.update();
	}

	@EventHandler
	public void chunkLoad(@NotNull final ChunkLoadEvent event) {
		service.updateChunk(event.getChunk());
	}
}
