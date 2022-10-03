package us.ajg0702.bots.ajsupport;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.internal.utils.JDALogger;
import org.slf4j.Logger;

import java.util.concurrent.ExecutionException;

public class CommandListener  extends ListenerAdapter {
    private final SupportBot bot;

    private final Logger logger;
    public CommandListener(SupportBot bot) {
        this.bot = bot;
        logger = JDALogger.getLog("Commands");
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {
        String name = e.getName();

        logger.info("Running command login for "+name);


        if(name.equals("ticketban")) {
            e.deferReply(true).queue();
            InteractionHook hook = e.getHook().setEphemeral(true);

            if(!e.getUser().getId().equals("171160105155297282")) {
                hook.sendMessage("You can't do this!").setEphemeral(true).queue();
                return;
            }

            OptionMapping userOption = e.getOption("user");
            OptionMapping reasonOption = e.getOption("reason");
            if(userOption == null) {
                e.reply("Need user to ban!").setEphemeral(true).queue();
                return;
            }

            userOption.getAsUser().openPrivateChannel().queue(privateChannel -> {
                try {
                    String reason = reasonOption == null ? "" : "Reason: " + reasonOption.getAsString();
                    privateChannel.sendMessage(
                            "You have been banned from creating new tickets in `aj's Plugins`.\nReason: `" + reason + "`\nIf you are confused why, make sure to read the reas, and the text in the create-ticket channel."
                    ).queue();
                    hook.sendMessage("Sent DM.").queue();
                } catch(Exception error) {
                    hook.sendMessage("An error occurred while DMing user: " + error.getMessage()).queue();
                }
            });

        }
        if(name.equals("remove")) {
            e.deferReply(true).queue();
            InteractionHook hook = e.getHook();

            if(!e.getUser().getId().equals("171160105155297282")) {
                hook.sendMessage("You can't do this!").setEphemeral(true).queue();
                return;
            }

            Guild guild = e.getGuild();
            if(guild == null) {
                hook.sendMessage("No guild!").setEphemeral(true).queue();
                return;
            }

            guild.updateCommands()
                    .addCommands(Commands.slash("remove", "Unregister commands (aj only)"))
                    .addCommands(Commands.slash("stop", "Stop the bot (aj only)"))
                    .submit().thenRun(() -> hook.sendMessage("Removed the commands!").setEphemeral(true).queue());


            /*try {
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
            }*/
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

        if(name.equals("reply")) {
            if(e.getMember() == null) {
                e.reply("No member!").setEphemeral(true).queue();
                return;
            }
            if(!e.getUser().getId().equals("171160105155297282") && !hasRole(e.getMember(), 615729338020528128L)) {
                e.reply("You can't do this!").setEphemeral(true).queue();
                return;
            }

            OptionMapping messageIdOption = e.getOption("message_id");
            OptionMapping responseNameOption = e.getOption("response_name");
            if(messageIdOption == null) {
                e.reply("Need message ID!").setEphemeral(true).queue();
                return;
            }
            if(responseNameOption == null) {
                e.reply("Need response name!").setEphemeral(true).queue();
                return;
            }
            long messageId = messageIdOption.getAsLong();
            String responseName = responseNameOption.getAsString();
            if(!bot.getJson().keySet().contains(responseName)) {
                e.reply("Invalid response name!")
                        .setEphemeral(true).queue();
                return;
            }

            e.getMessageChannel().retrieveMessageById(messageId).queue(message -> {
                try {
                    bot.reply(message, e.getUser(), responseName);
                } catch (EchoException ex) {
                    e.reply(ex.getMessage()).setEphemeral(true).queue();
                    return;
                }
                e.reply("Replied with "+responseName+"! :)").setEphemeral(true).queue();
            });

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


    public static boolean hasRole(Member member, long roleId) {
        for (Role role : member.getRoles()) {
            if(role.getIdLong() == roleId) return true;
        }
        return false;
    }
}
