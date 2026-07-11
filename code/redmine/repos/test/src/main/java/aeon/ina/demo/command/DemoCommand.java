package aeon.ina.demo.command;

import aeon.ina.demo.service.MessagingService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class DemoCommand implements CommandExecutor {

    private final MessagingService messagingService;

    public DemoCommand(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(messagingService.buildPingMessage(sender.getName()));
        return true;
    }
}
