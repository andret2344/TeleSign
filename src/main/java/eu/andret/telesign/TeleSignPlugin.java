package eu.andret.telesign;

import org.bstats.bukkit.Metrics;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class TeleSignPlugin extends JavaPlugin {
	@Override
	public void onEnable() {
		saveDefaultConfig();
		final TeleSignService service = new TeleSignService(this);
		service.reload();
		getServer().getPluginManager().registerEvents(new TeleSignListener(service), this);
		service.updateSigns();
		final PluginCommand command = Objects.requireNonNull(getCommand("telesign"));
		command.setExecutor(new TeleSignCommand(service));
		new Metrics(this, 16239);
	}
}
