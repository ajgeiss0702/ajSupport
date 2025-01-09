package us.ajg0702.bots.ajsupport;

import com.google.gson.JsonElement;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;
import us.ajg0702.bots.ajsupport.autorespond.EmbeddingUtils;

import java.io.IOException;
import java.util.Map;

public class ContextListener extends ListenerAdapter {
    private final SupportBot bot;

    public ContextListener(SupportBot bot) {
        this.bot = bot;
    }

    @Override
    public void onMessageContextInteraction(@NotNull MessageContextInteractionEvent e) {
        if(!e.getName().equals("Reply")) return;
        if(e.getMember() == null) {
            e.reply("No member!").setEphemeral(true).queue();
            return;
        }
        if(!e.getUser().getId().equals("171160105155297282") && !CommandListener.hasRole(e.getMember(), 615729338020528128L)) {
            e.reply("You can't do this!").setEphemeral(true).queue();
            return;
        }
        StringSelectMenu.Builder selectMenuBuilder = StringSelectMenu.create("reply-message");

        for (Map.Entry<String, JsonElement> entry : bot.getJson().entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue().getAsString();

            selectMenuBuilder.addOption(name, name, SupportBot.cutString(value, 100));
        }

        e.reply("Pick a message to reply with | "+e.getTarget().getId()).addActionRow(selectMenuBuilder.build()).setEphemeral(true).queue();
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent e) {
        if(e.getComponentId().equals("reply-message")) {
            String key = e.getValues().get(0);
            String id = e.getMessage().getContentStripped().split(" \\| ")[1];

            Message message = e.getMessageChannel().retrieveMessageById(id).complete();

            try {
                bot.reply(message, e.getUser(), key);
            } catch (EchoException ex) {
                e.reply(ex.getMessage()).queue();
                return;
            }
            e.reply("Replied with "+key+"! :)").setEphemeral(true).queue();
        }

        if(e.getComponentId().equals("add-vectorize-message")) {

            e.deferReply(true).queue();
            InteractionHook hook = e.getHook().setEphemeral(true);

            String key = e.getValues().get(0);
            String id = e.getMessage().getContentStripped().split(" \\| ")[1];

            Message message = e.getMessageChannel().retrieveMessageById(id).complete();

            try {
                EmbeddingUtils.insertIntoVectorize(
                        message.getId(),
                        EmbeddingUtils.embed(message.getContentStripped().replaceAll("\\n", " ")),
                        e.getChannelId(),
                        key
                );

                hook.sendMessage("Added to vectorize!").queue();

            } catch (IOException ex) {
                hook.sendMessage("Failed to add to vectorize: " + ex.getMessage()).queue();
            }
        }
    }
}
