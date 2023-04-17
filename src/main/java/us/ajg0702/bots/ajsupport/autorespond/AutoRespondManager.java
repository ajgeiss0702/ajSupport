package us.ajg0702.bots.ajsupport.autorespond;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import us.ajg0702.bots.ajsupport.autorespond.responders.BDNEResponder;
import us.ajg0702.bots.ajsupport.autorespond.responders.EmptyLBResponder;
import us.ajg0702.bots.ajsupport.autorespond.responders.OnlineOnlyResponse;
import us.ajg0702.bots.ajsupport.autorespond.responders.TestResponder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class AutoRespondManager extends ListenerAdapter {

    List<Responder> responders = Arrays.asList(
            new BDNEResponder(),
            new EmptyLBResponder(),
            new OnlineOnlyResponse(),
            new TestResponder()
    );

    public AutoRespondManager(JsonObject responses) {
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

        if(responses.isEmpty()) return;

        responses.sort(Comparator.comparingInt(Response::getConfidence).reversed());

        Response bestResponse = responses.get(0);
        if(bestResponse == null) return; // I don't think this could happen, but just to be safe

        event.getMessage()
                .reply(bestResponse.getMessage())
                .addEmbeds(
                        new EmbedBuilder()
                                .setDescription("The message above is an automated response. If it is not helpful, please state that it was not helpful so that a human can help you when they are available. Otherwise they may assume this message solved your issue")
                                .setFooter("ajSupport • Response confidence: " + bestResponse.getConfidence() + "%")
                                .build()
                )
                .queue();
    }
}
