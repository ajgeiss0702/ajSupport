package us.ajg0702.bots.ajsupport;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static us.ajg0702.bots.ajsupport.CommandListener.hasRole;

public class MessageListener extends ListenerAdapter {
    private final SupportBot bot;

    public MessageListener(SupportBot bot) {
        this.bot = bot;
    }

    private static final List<String> TEXT_EXTENSIONS = Arrays.asList("txt", "yml", "log", "yaml", "json", "js");

    private final Map<Long, Long> lastHelperMentionWarns = new HashMap<>();
    private final Map<Long, Long> lastAjMentionWarns = new HashMap<>();


    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        List<Message.Attachment> attachments = e.getMessage().getAttachments();
        if(attachments.size() > 0) {
            bot.getLogger().debug("Message has attachments");
            for(Message.Attachment attachment : attachments) {
                String ext = attachment.getFileExtension();
                if(ext == null) continue;
                if(TEXT_EXTENSIONS.contains(ext.toLowerCase(Locale.ROOT))) {
                    e.getMessage().reply("Please use https://paste.ajg0702.us/ to send text files!")
                            .setActionRow(Button.secondary("why_pastesite", "Why?"))
                            .queue();
                    break;
                }
            }
        }

        if(e.getMember() == null) return;

        if(
                !e.getAuthor().isBot() &&
                e.getTextChannel().getParentCategoryIdLong() == 804502763547000893L &&
                !hasRole(e.getMember(), 615729338020528128L) &&
                !hasRole(e.getMember(), 615721804585107477L)
        ) {
            for (Member mentionedMember : e.getMessage().getMentions().getMembers()) {
                if(hasRole(mentionedMember, 615729338020528128L) || hasRole(mentionedMember, 859784384739278928L)  && e.getMessage().getMessageReference() == null) {
                    Long last = lastHelperMentionWarns.getOrDefault(e.getChannel().getIdLong(), 0L);
                    if(System.currentTimeMillis() - last > 15000) {
                        lastHelperMentionWarns.put(e.getChannel().getIdLong(), System.currentTimeMillis());
                        e.getMessage().reply(
                                "Please don't ping our helpers!\n" +
                                        "\nThey help out when they're able to, and all pinging does is annoy them.\n" +
                                        "In the future, please be patient and wait for a response.")
                                .queue();
                    }
                }

                if(hasRole(mentionedMember, 615721804585107477L) && e.getMessage().getMessageReference() == null) {
                    Long last = lastAjMentionWarns.getOrDefault(e.getChannel().getIdLong(), 0L);
                    lastAjMentionWarns.put(e.getChannel().getIdLong(), System.currentTimeMillis());
                    if(System.currentTimeMillis() - last > 15000) {
                        e.getMessage().reply(
                                        "Please don't ping aj!\n" +
                                                "\naj has all notifications on, so he will see your message without you needing to ping him.\n" +
                                                "Because of that, the only thing that pinging does is annoy him.\n" +
                                                "If he doesn't respond, that means he's probably busy. He will respond when he can.\n" +
                                                "In the future, please be patient and wait for a response. (without pinging)")
                                .queue();
                    }
                }
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent e) {
        if (e.getComponentId().equals("why_pastesite")) {
            e.reply("You should use a paste site when sending files for one simple reason:\n It's easier to help you.\n\naj replies to many messages when all he has is his phone. Discord doesnt preview text files on phones (at least not yet), so he would have to download the file to be able to view it (which is a hassle, and a waste of space).\nIf you want aj to be able to help you, please use a paste site (e.g. https://paste.ajg0702.us/ ).")
                    .setEphemeral(true).queue();
        }
    }
}
