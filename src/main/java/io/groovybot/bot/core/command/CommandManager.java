package io.groovybot.bot.core.command;

import io.groovybot.bot.GroovyBot;
import io.groovybot.bot.core.events.command.CommandExecutedEvent;
import io.groovybot.bot.core.events.command.CommandFailEvent;
import io.groovybot.bot.core.events.command.NoPermissionEvent;
import lombok.extern.log4j.Log4j;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.SubscribeEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Log4j
public class CommandManager {

    private final Map<String, Command> commandAssociations;
    private final String defaultPrefix;

    public CommandManager(String defaultPrefix) {
        commandAssociations = new HashMap<>();
        this.defaultPrefix = defaultPrefix;
    }

    public void registerCommands(Command... commands) {
        for (Command command : commands) {
            registerCommand(command);
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public void onMessageRecived(GuildMessageReceivedEvent event) {
        if (event.getAuthor().isBot() || event.getAuthor().isFake() || event.isWebhookMessage())
            return;

        CommandEvent commandEvent = parseEvent(event);
        if (commandEvent == null)
            return;
        Command command = commandAssociations.get(commandEvent.getInvocation());
        if (command == null)
            return;
        if (commandEvent.getArgs().length > 0)
            if (command.getSubCommandAssociations().containsKey(commandEvent.getInvocation()))
                command = command.getSubCommandAssociations().get(commandEvent.getInvocation());
            call(command, commandEvent);
    }

    private void call(Command command, CommandEvent commandEvent) {
        if (!command.getPermissions().isCovered(GroovyBot.getInstance().getUserCache().get(commandEvent.getAuthor().getIdLong()).getPermissions(), commandEvent)) {
            GroovyBot.getInstance().getEventManager().handle(new NoPermissionEvent(commandEvent, command));
            return;
        }
        try {
            TextChannel channel = commandEvent.getChannel();
            channel.sendTyping().queue();
            if (commandEvent.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_MANAGE)) {
                commandEvent.getMessage().delete().queue();
            }
            Result result = command.run(commandEvent.getArgs(), commandEvent);
            if (result != null)
                result.sendMessage(channel, 60);
            GroovyBot.getInstance().getEventManager().handle(new CommandExecutedEvent(commandEvent, command));
        } catch (Exception e) {
            GroovyBot.getInstance().getEventManager().handle(new CommandFailEvent(commandEvent, command, e));
        }
    }

    private CommandEvent parseEvent(GuildMessageReceivedEvent event) {
        String prefix = null;
        String customPrefix = GroovyBot.getInstance().getGuildCache().get(event.getGuild().getIdLong()).getPrefix();
        String content = event.getMessage().getContentRaw();
        if (content.startsWith(event.getGuild().getSelfMember().getAsMention()))
            prefix = event.getGuild().getSelfMember().getAsMention();
        else if (content.startsWith(defaultPrefix))
            prefix = defaultPrefix;
        else if (content.startsWith(customPrefix))
            prefix = customPrefix;

        if (prefix != null) {
            String beheaded = content.substring(prefix.length()).trim();
            String[] allArgs = beheaded.split("\\s+");
            String invocation = allArgs[0];
            String[] args = new String[allArgs.length -1];
            System.arraycopy(allArgs, 1, args, 0, args.length);
            return new CommandEvent(event.getJDA(), event.getResponseNumber(), event.getMessage(), GroovyBot.getInstance(), args, invocation);
        }
        return null;
    }

    private void registerCommand(Command command) {
        for (String alias : command.getAliases()) {
            if (commandAssociations.containsKey(alias))
                log.warn(String.format("[CommandManager] Alias %s is already taken by %s", alias, commandAssociations.get(alias).getClass().getCanonicalName()));
            else
                commandAssociations.put(alias, command);
        }
    }
}