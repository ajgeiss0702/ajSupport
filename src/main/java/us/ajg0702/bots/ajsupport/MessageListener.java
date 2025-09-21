package us.ajg0702.bots.ajsupport;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.unions.IThreadContainerUnion;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.forum.ForumTagAddEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static us.ajg0702.bots.ajsupport.CommandListener.hasRole;

public class MessageListener extends ListenerAdapter {
    private final SupportBot bot;

    public MessageListener(SupportBot bot) {
        this.bot = bot;
    }

    private static final List<String> TEXT_EXTENSIONS = Arrays.asList("txt", "yml", "log", "yaml", "json", "js", "log.gz");

    private static final int WEEK_SECONDS = 604800;

    private final Map<Long, Long> lastHelperMentionWarns = new HashMap<>();
    private final Map<Long, Long> lastAjMentionWarns = new HashMap<>();

    private final Map<String, Long> lastPingWarn = new HashMap<>();
    private final Map<String, String> warnedAboutLongMute = new HashMap<>();

    private final Pattern pastebinPattern = Pattern.compile("https://pastebin\\.com/(raw/)?(.*)");



    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        List<Message.Attachment> attachments = e.getMessage().getAttachments();
        if(attachments.size() > 0) {
            bot.getLogger().debug("Message has attachments");
            for(Message.Attachment attachment : attachments) {
                String fileName = attachment.getFileName();
                String ext = attachment.getFileExtension();
                String allExt = fileName.substring(fileName.indexOf(".") + 1);
                if(ext == null) continue;
                ext = ext.toLowerCase(Locale.ROOT);
                allExt = allExt.toLowerCase(Locale.ROOT);
                if(TEXT_EXTENSIONS.contains(ext) || TEXT_EXTENSIONS.contains(allExt)) {
                    e.getChannel().sendTyping().queue();

                    try {
                        String url = Utils.uploadAttachment(bot, attachment);
                        e.getMessage()
                                .reply(
                                        "I've uploaded that for you: " + url
                                )
                                .setActionRow(Button.secondary("why_pastesite", "Why?"))
                                .queue();
                        break;
                    } catch (IOException | ExecutionException | InterruptedException | TimeoutException err) {
                        bot.getLogger().error("An error occurred while uploading text file:", err);
                        e.getMessage().reply("Please use https://paste.ajg0702.us/ to send text files!\n(Normally I would upload it automatically, but something went wrong)")
                                .setActionRow(Button.secondary("why_pastesite", "Why?"))
                                .queue();
                        return;
                    }
                }
            }
        }


        List<String> pastebinLinks = new ArrayList<>();
        Matcher pastebinMatcher = pastebinPattern.matcher(e.getMessage().getContentStripped());
        while(pastebinMatcher.find()) {
            String code = pastebinMatcher.group(2);
            pastebinLinks.add("https://paste.ajg0702.us/pastebin/" + code);
        }
        if(pastebinLinks.size() > 0) {
            e.getMessage()
                    .reply(
                            "I've uploaded " + (pastebinLinks.size() == 1 ? "that" : "those") + " for you: "
                            + String.join("\n", pastebinLinks)
                    )
                    .setActionRow(Button.secondary("why_pastesite", "Why?"))
                    .queue();
        }

        if(e.getMember() == null) {
            bot.getLogger().warn("{} is not a member!", e.getAuthor().getName());
            return;
        }

        MessageChannelUnion channel = e.getChannel();
        long parent = channel instanceof ICategorizableChannel ? ((ICategorizableChannel) channel).getParentCategoryIdLong() : -1;

        if(channel.getType().isThread()) {
            IThreadContainerUnion threadParent = channel.asThreadChannel().getParentChannel();
            parent = threadParent instanceof ICategorizableChannel ?
                    ((ICategorizableChannel) threadParent).getParentCategoryIdLong() : -2;
        }

        bot.getLogger().info("parent is " + parent);

        if(

                !e.getAuthor().isBot() &&
                parent == 804502763547000893L && // only in support channels
                !hasRole(e.getMember(), 615729338020528128L) && // exclude helpers
                !hasRole(e.getMember(), 615721804585107477L) // exclude @aj
        ) {
            for (Member mentionedMember : e.getMessage().getMentions().getMembers()) {
                if(hasRole(mentionedMember, 615729338020528128L) || hasRole(mentionedMember, 859784384739278928L)  && e.getMessage().getMessageReference() == null) {
                    Long last = lastHelperMentionWarns.getOrDefault(e.getChannel().getIdLong(), 0L);
                    long distanceSinceLast = System.currentTimeMillis() - last;
                    if(distanceSinceLast > 15000) {
                        lastHelperMentionWarns.put(e.getChannel().getIdLong(), System.currentTimeMillis());
                        e.getMessage().reply(
                                "Please don't ping our helpers!\n" +
                                        "\nThey help out when they're able to, and all pinging does is annoy them.\n" +
                                        "In the future, please be patient and wait for a response.\n" +
                                        "If you are not willing to wait, <#1371201897528164382> will give fast responses 24/7, and is able to answer 90% of questions and issues.\n"
                                        )
                                .queue();
                    }
                    e.getMember().timeoutFor(Duration.ofSeconds(distanceSinceLast < WEEK_SECONDS ? 60 : 30)).queue();
                }

                if(hasRole(mentionedMember, 615721804585107477L) && e.getMessage().getMessageReference() == null) {
                    Long last = lastAjMentionWarns.getOrDefault(e.getChannel().getIdLong(), 0L);
                    Long lastUser = lastPingWarn.getOrDefault(e.getAuthor().getId(), 0L);
                    lastAjMentionWarns.put(e.getChannel().getIdLong(), System.currentTimeMillis());
                    lastPingWarn.put(e.getAuthor().getId(), System.currentTimeMillis());

                    long distanceSinceLast = System.currentTimeMillis() - last;
                    long distanceSinceLastUser = System.currentTimeMillis() - lastUser;

                    boolean hasBeenWarnedAboutLongMute = warnedAboutLongMute.get(e.getAuthor().getId()) != null;

                    // forgive if the user hasnt pinged aj in 60 days (they probably just forgot)
                    boolean muteUser = hasBeenWarnedAboutLongMute && distanceSinceLastUser < 60 * 24 * 60 * 60e3;

                    if(distanceSinceLast > 15000) {
                        boolean showNotice = !muteUser;
                        Message warningMessage = e.getMessage().reply(
                                        "Please don't ping aj!\n" +
                                                "\naj has all notifications on, so he will see your message without you needing to ping him.\n" +
                                                "Because of that, the only thing that pinging does is annoy him.\n" +
                                                "If he doesn't respond, that means he's probably busy. He will respond when he can.\n" +
                                                "In the future, please be patient and wait for a response. (without pinging)\n" +
                                                "If you are not willing to wait, <#1371201897528164382> will give fast responses 24/7, and is able to answer 90% of questions and issues.\n" +
                                                "\n" +
                                                (showNotice ?
                                                "**NOTICE:** If you ignore this warning and ping aj again, you will be muted for a day." :
                                                "**You have been muted** because you ignored this warning [before](https://discord.com/channels/615715762912362565/" + warnedAboutLongMute.get(e.getAuthor().getId()) + "). In the future, do not ping aj.") + "\n" +
                                                "-# This message is a warning for <@" + e.getAuthor().getId() + ">")
                                .complete();
                        if(showNotice) {
                            warnedAboutLongMute.put(e.getAuthor().getId(), warningMessage.getChannelId() + "/" + warningMessage.getId());
                        }
                    } else {
                        bot.getLogger().info("Not warning due to distanceSinceLast of " + distanceSinceLast);
                    }
                    int timeoutSeconds = distanceSinceLast < WEEK_SECONDS ? 60 : 30;

                    if(muteUser) timeoutSeconds = 24 * 60 * 60;

                    e.getMember().timeoutFor(Duration.ofSeconds(timeoutSeconds)).queue();
                }
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent e) {
        if (e.getComponentId().equals("why_pastesite")) {
            e.reply("You should use a paste site when sending files for one simple reason:\n" +
                            " It's easier to help you.\n" +
                            "\n" +
                            "aj replies to many messages when all he has is his phone. " +
                            "Discord doesnt preview text files on phones (at least not yet), " +
                            "so he would have to download the file to be able to view it " +
                            "(which is a hassle, and a waste of space).\n" +
                            "If you want aj to be able to help you, please use a paste site " +
                            "(e.g. https://paste.ajg0702.us/ ).\n" +
                            "\n" +
                            "I recommend against using pastebin, as it tends to be annoying to view, and it's ads are annoying.")
                    .setEphemeral(true).queue();
        }
    }
}
