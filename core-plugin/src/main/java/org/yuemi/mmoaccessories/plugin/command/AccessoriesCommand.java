package org.yuemi.mmoaccessories.plugin.command;

import org.jetbrains.annotations.NotNull;
import org.yuemi.mmoaccessories.api.AccessoriesApi;
import org.yuemi.mmoaccessories.plugin.AccessoriesPlugin;
import org.yuemi.mmoaccessories.plugin.command.subcommands.EquipCommand;
import org.yuemi.mmoaccessories.plugin.command.subcommands.GiveCommand;
import org.yuemi.mmoaccessories.plugin.command.subcommands.ListCommand;
import org.yuemi.mmoaccessories.plugin.command.subcommands.OpenCommand;
import org.yuemi.mmoaccessories.plugin.command.subcommands.ReloadCommand;
import org.yuemi.mmoaccessories.plugin.command.subcommands.SetCommand;
import org.yuemi.mmoaccessories.plugin.command.subcommands.ViewCommand;
import org.yuemi.mmoaccessories.plugin.command.subcommands.WipeCommand;

public final class AccessoriesCommand extends BaseCommand {

    public AccessoriesCommand(@NotNull AccessoriesApi api, @NotNull AccessoriesPlugin plugin) {
        register(new OpenCommand(api));
        register(new EquipCommand(api));
        register(new ReloadCommand(plugin));
        register(new ListCommand(api));
        register(new ViewCommand(api));
        register(new WipeCommand(api));
        register(new SetCommand(api));
        register(new GiveCommand());
    }

    @Override
    protected String getCommandLabel() {
        return "Accessories";
    }
}
