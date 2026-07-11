package aeon.ina.demo;

import aeon.ina.demo.command.DemoCommand;
import aeon.ina.demo.service.MessagingService;
import org.bukkit.plugin.java.JavaPlugin;

public final class DemoPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        MessagingService messagingService = new MessagingService();
        getCommand("ping").setExecutor(new DemoCommand(messagingService));
        getLogger().info("Plugin enabled");
    }

    @Override
    public void onDisable() {
	getLogger().info("Plugin Disabled");
    }
}
