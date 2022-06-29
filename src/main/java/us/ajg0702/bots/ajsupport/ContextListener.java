package us.ajg0702.bots.ajsupport;

import com.google.gson.JsonElement;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import org.jetbrains.annotations.NotNull;

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
        SelectMenu.Builder selectMenuBuilder = SelectMenu.create("reply-message");

        for (Map.Entry<String, JsonElement> entry : bot.getJson().entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue().getAsString();

            selectMenuBuilder.addOption(name, name, SupportBot.cutString(value, 100));
        }

        e.reply("Pick a message to reply with | "+e.getTarget().getId()).addActionRow(selectMenuBuilder.build()).setEphemeral(true).queue();
    }

    @Override
    public void onSelectMenuInteraction(@NotNull SelectMenuInteractionEvent e) {
        if(!e.getComponentId().equals("reply-message")) return;

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
}
