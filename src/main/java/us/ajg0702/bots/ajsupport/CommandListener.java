package us.ajg0702.bots.ajsupport;

import com.google.gson.JsonElement;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.internal.utils.JDALogger;
import org.slf4j.Logger;
import us.ajg0702.bots.ajsupport.autorespond.EmbeddingUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class CommandListener  extends ListenerAdapter {
    private final SupportBot bot;

    private final Logger logger;
    public CommandListener(SupportBot bot) {
        this.bot = bot;
        logger = JDALogger.getLog("Commands");
    }

    @Override
    public void onMessageContextInteraction(MessageContextInteractionEvent e) {
        final String name = e.getName();

        if(name.equals("Add to Vectorize")) {
            if(e.getMember().getId().equals("171160105155297282")) {
                StringSelectMenu.Builder selectMenuBuilder = StringSelectMenu.create("add-vectorize-message");

                int i = 0;

                for (Map.Entry<String, JsonElement> entry : bot.getJson().entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue().getAsString();

                    selectMenuBuilder.addOption(key, key, SupportBot.cutString(value, 100));
                    i++;
                    if(i >= 25) break;
                }

                e.reply("Pick a message to attach to this vector | "+e.getTarget().getId()).addActionRow(selectMenuBuilder.build()).setEphemeral(true).queue();
            } else {
                e.reply("You can't do this!").setEphemeral(true).queue();
                return;
            }
        }
        if(name.equals("Remove from Vectorize")) {
            if(e.getMember().getId().equals("171160105155297282")) {
                e.deferReply(true).queue();
                InteractionHook hook = e.getHook().setEphemeral(true);

                try {
                    EmbeddingUtils.deleteFromVectorize(e.getTarget().getId());
                    hook.sendMessage("Removed from vectorize!").queue();
                } catch (IOException ex) {
                    hook.sendMessage("Failed to remove from vectorize: " + ex.getMessage()).queue();
                    bot.getLogger().warn("Failed to remove from vectorize:", ex);
                }
            }
        }

        if(name.equals("Upload Text File")) {
            e.deferReply(true).queue();
            InteractionHook hook = e.getHook().setEphemeral(true);

            List<String> urls = new ArrayList<>();
            for (Message.Attachment attachment : e.getTarget().getAttachments()) {
                try {
                    String url = Utils.uploadAttachment(bot, attachment);
                    urls.add(url);
                } catch (IOException | ExecutionException | InterruptedException | TimeoutException ex) {
                    bot.getLogger().warn("Failed to upload file:", ex);
                    urls.add("[" + attachment.getFileName() + " + fail]");
                }
            }
            hook.sendMessage("Uploaded!" + (urls.size() > 1 ? "\n" : " ") + String.join("\n", urls)).setEphemeral(true).queue();

        }
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
                hook.sendMessage("Need user to ban!").setEphemeral(true).queue();
                return;
            }

            userOption.getAsUser().openPrivateChannel().queue(privateChannel -> {
                try {
                    String reason = reasonOption == null ? "" : "Reason: " + reasonOption.getAsString();
                    privateChannel.sendMessage(
                            "You have been banned from creating new tickets in `aj's Plugins`.\n" + reason + "\nIf you are confused why, make sure to read the above reason, and the text in the <#804502842165297253> channel."
                    ).queue();
                    hook.sendMessage("Sent DM.").queue();
                } catch(Exception error) {
                    hook.sendMessage("An error occurred while DMing user: " + error.getMessage()).queue();
                }
            });

        }
        if(name.equals("upload")) {
            e.deferReply(true).queue();
            InteractionHook hook = e.getHook().setEphemeral(true);

            if(!e.getUser().getId().equals("171160105155297282")) {
                hook.sendMessage("You can't do this!").setEphemeral(true).queue();
                return;
            }

            OptionMapping messageOption = e.getOption("message_id");
            if(messageOption == null) {
                hook.sendMessage("Need message to get attachments from!").setEphemeral(true).queue();
                return;
            }

            long messageId = messageOption.getAsLong();

            e.getMessageChannel().retrieveMessageById(messageId).queue(message -> {
                if(message.getAttachments().size() == 0) {
                    hook.sendMessage("That message has no attachments!").queue();
                    return;
                }
                for (Message.Attachment attachment : message.getAttachments()) {
                    try {
                        String url = Utils.uploadAttachment(bot, attachment);
                        hook.sendMessage("Uploaded! " + url).setEphemeral(true).queue();
                        message.reply("I've uploaded that for you: " + url).queue();
                        break;
                    } catch (IOException | ExecutionException | InterruptedException | TimeoutException ex) {
                        bot.getLogger().warn("Failed to upload file:", ex);
                    }
                }
            });
            return;
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
                    .addCommands(Commands.slash("update", "Update the bot (aj only)"))
                    .and(bot.getJDA().updateCommands()) // removes global commands
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
        if(name.equals("update")) {
            if(!e.getUser().getId().equals("171160105155297282")) {
                e.reply("You cant do this!").setEphemeral(true).queue();
                return;
            }
            e.reply("Starting update...").setEphemeral(true)
                    .queue(m -> bot.getUpdateManager().update(m));
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

        e.reply(bot.getJson().get(name).getAsString())
                .setEphemeral(Objects.equals(e.getChannelId(), "1371201897528164382"))
                .queue();
    }


    public static boolean hasRole(Member member, long roleId) {
        for (Role role : member.getRoles()) {
            if(role.getIdLong() == roleId) return true;
        }
        return false;
    }
}
