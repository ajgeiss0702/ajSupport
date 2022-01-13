package us.ajg0702.bots.ajsupport;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MessageListener extends ListenerAdapter {
    private final SupportBot bot;

    public MessageListener(SupportBot bot) {
        this.bot = bot;
    }

    private static final List<String> TEXT_EXTENSIONS = Arrays.asList("txt", "yml", "log", "yaml", "json", "js");

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        List<Message.Attachment> attachments = e.getMessage().getAttachments();
        if(attachments.size() == 0) return;
        bot.getLogger().debug("Message has attachments");

        for(Message.Attachment attachment : attachments) {
            String ext = attachment.getFileExtension();
            if(ext == null) continue;
            if(TEXT_EXTENSIONS.contains(ext.toLowerCase(Locale.ROOT))) {
                e.getMessage().reply("Please use https://paste.ajg0702.us/ to send text files!")
                        .setActionRow(Button.secondary("why_pastesite", "Why?"))
                        .queue();
                return;
            }
        }
    }

    @Override
    public void onButtonClick(ButtonClickEvent e) {
        if (e.getComponentId().equals("why_pastesite")) {
            e.reply("You should use a paste site when sending files for one simple reason:\n It's easier to help you.\n\naj replies to many messages when all he has is his phone. Discord doesnt preview text files on phones (at least not yet), so he would have to download the file to be able to view it (which is a hassle, and a waste of space).\nIf you want aj to be able to help you, please use a paste site (e.g. https://paste.ajg0702.us/ ).")
                    .setEphemeral(true).queue();
        }
    }
}
