package us.ajg0702.bots.ajsupport.autorespond;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import us.ajg0702.bots.ajsupport.EchoException;
import us.ajg0702.bots.ajsupport.SupportBot;
import us.ajg0702.bots.ajsupport.autorespond.responders.*;
import us.ajg0702.bots.ajsupport.autorespond.responders.ajlb.BDNEResponder;
import us.ajg0702.bots.ajsupport.autorespond.responders.ajlb.DontUpdatePermResponse;
import us.ajg0702.bots.ajsupport.autorespond.responders.ajlb.EmptyLBResponder;
import us.ajg0702.bots.ajsupport.autorespond.responders.ajlb.OnlineOnlyResponse;
import us.ajg0702.bots.ajsupport.autorespond.responders.ajq.SpigotForwardingResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class AutoRespondManager extends ListenerAdapter {

    private final SupportBot bot;

    List<Responder> responders = Arrays.asList(
            // ajlb
            new BDNEResponder(),
            new EmptyLBResponder(),
            new OnlineOnlyResponse(),
            new TestResponder(),
            new DontUpdatePermResponse(),

            // ajq
            new SpigotForwardingResponse()
    );

    public AutoRespondManager(JsonObject responses, SupportBot bot) {
        this.bot = bot;
        Responder.RESPONSES = responses;
    }
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Member member = event.getMember();
        if(member == null) return;
        if(member.getUser().isBot()) return;

        List<Response> responses = new ArrayList<>();
        for (Responder responder : responders) {
            Response r = responder.checkForResponse(member, event.getMessage());
            if(r != null) responses.add(r);
        }

        if(responses.isEmpty()) {
            autoRespondEmbeddings(event.getMessage());
            return;
        }

        responses.sort(Comparator.comparingInt(Response::getConfidence).reversed());

        Response bestResponse = responses.get(0);
        if(bestResponse == null) { // I don't think this could happen, but just to be safe
            autoRespondEmbeddings(event.getMessage());
            return;
        }

        event.getMessage()
                .reply(bestResponse.getMessage())
                .addEmbeds(
                        new EmbedBuilder()
                                .setDescription("The message above is an automated response. If it is not helpful, please state that it was not helpful so that a human can help you when they are available. Otherwise they may assume this message solved your issue")
                                .setFooter("ajSupport • Selection: static • Response confidence: " + bestResponse.getConfidence() + "%")
                                .build()
                )
                .queue();
    }

    public void autoRespondEmbeddings(Message message) {
        String categoryID = message.getChannel().asTextChannel().getParentCategoryId();
        if(categoryID == null || (!categoryID.equals("804502763547000893") && !message.getChannelId().equals("700885801352822825"))) return;

        try {
            BigDecimal[] vec = EmbeddingUtils.embed(message.getContentStripped().replaceAll("\\n", " "));
            EmbeddingUtils.VectorizeResponse topResult = EmbeddingUtils.queryVectorize(vec);
            if(topResult == null || topResult.getScore() < 0.85) return;

            String responseKey = topResult.getMetadata().get("response").getAsString();
            String originalChannelId = topResult.getMetadata().get("channelId").getAsString();
            String originalMessageLink = "https://discord.com/channels/615715762912362565/" + originalChannelId + "/" + topResult.getId();

            TextChannel channel = bot.getJDA().getTextChannelById(698756204801032202L);
            if(channel == null) {
                bot.getLogger().error("Cannot find logger-log channel for aj's plugins!");
                throw new IOException("Cannot find log channel!");
            }
            channel.sendMessageEmbeds(
                    new EmbedBuilder()
                            .setDescription("vectorize is replying with " + responseKey + " due to result from " + originalMessageLink + "\n" +
                                    "Reply to "+message.getAuthor().getName()+": " +
                                    SupportBot.cutString(
                                            message.getContentStripped().replaceAll("\n", " "),
                                            100
                                    )
                            )
                            .build()
            ).queue();
            message.reply(bot.getJson().get(responseKey).getAsString())
                    .addEmbeds(
                            new EmbedBuilder()
                                    .setDescription("The message above is an automated response. If it is not helpful, please state that it was not helpful so that a human can help you when they are available. Otherwise they may assume this message solved your issue")
                                    .setFooter("ajSupport • Selection: vectorize • Response confidence: " + Math.round(topResult.getScore()*10000d)/100d + "%")
                                    .build()
                    )
                    .queue();

        } catch (IOException e) {
            bot.getLogger().warn("Error while embedding for auto-response:", e);
        }

    }
}
