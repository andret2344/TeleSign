package eu.andret.telesign;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class TeleSignCommand implements CommandExecutor {
	@NotNull
	private final TeleSignService service;

	public TeleSignCommand(@NotNull final TeleSignService service) {
		this.service = service;
	}

	@Override
	public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
		if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
			sender.sendMessage(Component.text("Error: ", NamedTextColor.DARK_RED)
					.append(Component.text("Usage: /telesign reload", NamedTextColor.RED)));
			return true;
		}
		try {
			service.reload();
		} catch (final IllegalArgumentException ex) {
			sender.sendMessage(Component.text(ex.getMessage(), NamedTextColor.RED));
			return true;
		}
		service.updateSigns();
		sender.sendMessage(Component.text("TeleSign configuration reloaded.", NamedTextColor.GREEN));
		return true;
	}
}
