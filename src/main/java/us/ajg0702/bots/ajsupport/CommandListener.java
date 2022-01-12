package us.ajg0702.bots.ajsupport;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.internal.utils.JDALogger;
import org.slf4j.Logger;

import java.util.concurrent.ExecutionException;

public class CommandListener  extends ListenerAdapter {
    private JDA jda;
    private SupportBot bot;

    private final Logger logger;
    public CommandListener() {
        logger = JDALogger.getLog("Commands");
    }

    @Override
    public void onSlashCommand(SlashCommandEvent e) {
        String name = e.getName();


        if(name.equals("remove")) {
            e.deferReply(true).queue();
            InteractionHook hook = e.getHook();
            Guild guild = e.getGuild();
            if(guild == null) {
                hook.sendMessage("No guild!").setEphemeral(true).queue();
                return;
            }
            if(!e.getUser().getId().equals("171160105155297282")) {
                hook.sendMessage("You cant do this!").setEphemeral(true).queue();
                return;
            }
            try {
                guild.retrieveCommands().submit().get().forEach(command -> {
                    if(!(command.getName().equals("remove") || command.getName().equals("stop"))) {
                        bot.getLogger().debug("Removing command "+command.getName()+" ("+command.getId()+")");
                        guild.deleteCommandById(command.getId()).queue();
                    }
                });
                hook.sendMessage("Removed the commands!").setEphemeral(true).queue();
            } catch (InterruptedException | ExecutionException ex) {
                ex.printStackTrace();
                hook.sendMessage("An error occurred").setEphemeral(true).queue();
            }
            return;
        }
        if(name.equals("stop")) {
            if(!e.getUser().getId().equals("171160105155297282")) {
                e.reply("You cant do this!").setEphemeral(true).queue();
                return;
            }
            try {
                e.reply("Stopping :)").setEphemeral(true).submit().get();
            } catch (InterruptedException | ExecutionException ex) {
                ex.printStackTrace();
            }
            System.exit(0);
            return;
        }

        if(!bot.getJson().keySet().contains(name)) {
            e.reply("Invalid command!")
                    .setEphemeral(true).queue();
            return;
        }
        logger.debug("SlashCommand "+name);

        e.reply(bot.getJson().get(name).getAsString()).queue();
    }

    public void setJDA(JDA jda) {
        this.jda = jda;
    }

    public void setBot(SupportBot bot) {
        this.bot = bot;
    }
}
