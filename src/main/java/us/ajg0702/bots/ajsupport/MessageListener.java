package us.ajg0702.bots.ajsupport;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.IThreadContainerUnion;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
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

    private final Map<String, Long> lastSpamTimeout = new HashMap<>();

    private final Map<String, List<Long>> previousMessageSends = new HashMap<>();
    private final Map<String, Long> lastChannelSendTime = new HashMap<>();
    private final Map<String, String> lastChannelSend = new HashMap<>();

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        String sourceUserId = e.getAuthor().getId();
        if(e.getMessage().getInteractionMetadata() != null) {
            // get interaction because sometimes people use interactions (bot commands) to spam
            sourceUserId = e.getMessage().getInteractionMetadata().getUser().getId();
        }

        String userLastChannelSend = lastChannelSend.get(sourceUserId);
        Long userLastChannelSendTime = lastChannelSendTime.get(sourceUserId);
        if(userLastChannelSendTime != null && !e.getChannel().getId().equals(userLastChannelSend)) {
            long distance = System.currentTimeMillis() - userLastChannelSendTime;
            if(distance < 750) {
                spamTimeout(e.getGuild(), sourceUserId);
                return;
            }
        }
        lastChannelSend.put(sourceUserId, e.getChannel().getId());
        lastChannelSendTime.put(sourceUserId, System.currentTimeMillis());

        previousMessageSends.compute(sourceUserId, (k, v) -> {
            if(v == null) {
                return new ArrayList<>(List.of(System.currentTimeMillis()));
            } else {
                v.add(System.currentTimeMillis());
                while(v.size() > 25) {
                    v.remove(0);
                }
                return v;
            }
        });

        List<Long> userPreviousMessageTimes = previousMessageSends.get(sourceUserId)
                .stream()
                .filter(t -> System.currentTimeMillis() - t < 60e3)
                .toList();

        if(userPreviousMessageTimes.size() > 3) {
            List<Long> previousMessageGaps = new ArrayList<>();
            for (int i = 1; i < userPreviousMessageTimes.size(); i++) {
                previousMessageGaps.add(userPreviousMessageTimes.get(i) - userPreviousMessageTimes.get(i - 1));
            }

            long total = previousMessageGaps.stream().mapToLong(Long::longValue).sum();
            double avgGapMs = (double) total / previousMessageGaps.size();
            if(avgGapMs < 500) {
                bot.getLogger().info("Timing out {} due to average message gap of {}ms", sourceUserId, Math.floor(avgGapMs * 100d) / 100d);
                spamTimeout(e.getGuild(), sourceUserId);
                return;
            }
        }



        List<Message.Attachment> attachments = e.getMessage().getAttachments();
        if(attachments.size() > 0) {
            bot.getLogger().debug("Message has attachments");
            List<String> urls = new ArrayList<>();
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
                        urls.add(Utils.uploadAttachment(bot, attachment));
                    } catch (IOException | ExecutionException | InterruptedException | TimeoutException err) {
                        bot.getLogger().error("An error occurred while uploading text file:", err);
                        e.getMessage().reply("Please use https://paste.ajg0702.us/ to send text files!\n(Normally I would upload it automatically, but something went wrong)")
                                .setActionRow(Button.secondary("why_pastesite", "Why?"))
                                .queue();
                        return;
                    }
                }
            }
            if(!urls.isEmpty()) {
                e.getMessage()
                        .reply(
                                "I've uploaded " + (urls.size() == 1 ? "that" : "those") + " for you:\n" + String.join("\n", urls)
                        )
                        .setActionRow(Button.secondary("why_pastesite", "Why?"))
                        .queue();
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

    public void spamTimeout(Guild guild, String userId) {
        Member member = guild.getMemberById(userId);
        if(member == null) {
            bot.getLogger().warn("Unable to get member from id {}, so unable to time them out for spam", userId);
            return;
        }

        Long userLastSpamTimeout = lastSpamTimeout.get(userId);
        boolean shortTimeout = userLastSpamTimeout == null || System.currentTimeMillis() - userLastSpamTimeout > 30 * 24 * 60 * 60e3;
        if(shortTimeout) {
            member.timeoutFor(Duration.ofMinutes(30)).queue();
        } else {
            member.timeoutFor(Duration.ofHours(20)).queue();
        }
        lastSpamTimeout.put(userId, System.currentTimeMillis());

        member.getUser().openPrivateChannel()
                .queue(channel ->
                        channel.sendMessage(
                                "You have been automatically timed out in aj's Plugins due to spam. " +
                                        (shortTimeout ?
                                                "If you continue to spam after this timeout ends, you will be timed out for much longer. " :
                                                "Since this is the second time you have triggered this recently, you have gotten an even longer timeout. "
                                        ) +
                                        "You cannot appeal this timeout (unless you genuinely weren't spamming, but if this check triggered you almost certainly are), just don't spam."
                                        )
                                .queue()
                );

        int messageCount = 0;
        for (TextChannel textChannel : guild.getTextChannels()) {
            List<Message> messages = new ArrayList<>();
            for (Message message : textChannel.getIterableHistory()) {
                // only purge messages from the past hour
                if(System.currentTimeMillis() - (message.getTimeCreated().toEpochSecond() * 1e3) > 60 * 60e3) break;
                if(
                        !message.getAuthor().getId().equals(userId) &&
                        (message.getInteractionMetadata() == null || !message.getInteractionMetadata().getUser().getId().equals(userId))
                ) continue; // skip if not from our target
                messages.add(message);
            }
            if(messages.isEmpty()) continue;
            if(messages.size() == 1) {
                messages.get(0).delete().queue();
            } else {
                // JDA's purgeMessages requires 2-100 messages per call (Discord bulk-delete limit).
                // Chunk to avoid IllegalArgumentException on >100 entries.
                for (int i = 0; i < messages.size(); i += 100) {
                    List<Message> chunk = messages.subList(i, Math.min(i + 100, messages.size()));
                    if (chunk.size() == 1) {
                        chunk.get(0).delete().queue();
                    } else {
                        textChannel.purgeMessages(chunk);
                    }
                }
            }
            messageCount += messages.size();
        }

        TextChannel logChannel = bot.getJDA().getTextChannelById(698756204801032202L);
        if(logChannel == null) {
            bot.getLogger().error("Cannot find logger-log channel for aj's plugins!");
        } else {
            logChannel.sendMessage("Timed out " + member.getAsMention() + " with shortTimeout:" + shortTimeout + " and purged " + messageCount + " messages.")
                    .queue();
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
