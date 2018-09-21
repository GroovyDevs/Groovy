package io.groovybot.bot.commands.settings;

import io.groovybot.bot.GroovyBot;
import io.groovybot.bot.core.command.Command;
import io.groovybot.bot.core.command.CommandCategory;
import io.groovybot.bot.core.command.CommandEvent;
import io.groovybot.bot.core.command.Result;
import io.groovybot.bot.core.command.permission.Permissions;
import io.groovybot.bot.core.entity.User;

import java.util.Locale;

public class LanguageCommand extends Command {

    public LanguageCommand() {
        super(new String[] {"languages", "lang"}, CommandCategory.SETTINGS, Permissions.everyone(), "Sets your own language", "[language-tag]");
    }

    @Override
    public Result run(String[] args, CommandEvent event) {
        User user = GroovyBot.getInstance().getUserCache().get(event.getAuthor().getIdLong());
        if (args.length == 0)
            return send(info(event.translate("command.language.info.title"), String.format(event.translate("command.language.info.description"), user.getLocale().getLanguage())));
        Locale locale;
        try {
            locale = Locale.forLanguageTag(args[0].replace("_", "-"));
        } catch (Exception e) {
            return send(error(event.translate("command.language.invalid.title"), event.translate("command.language.invalid.description")));
        }
        if (event.getGroovyBot().getTranslationManager().getLocaleByLocale(locale) == null)
            return send(error(event.translate("command.language.nottranslated.title"), event.translate("command.language.nottranslated.description")));
        user.setLocale(locale);
        return send(success(event.translate("command.language.set.title"), String.format(event.translate("command.language.set.description"), locale.getLanguage())));
    }
}
