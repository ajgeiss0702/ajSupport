package us.ajg0702.bots.ajsupport.autorespond.responders.ajlb;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import us.ajg0702.bots.ajsupport.autorespond.Responder;
import us.ajg0702.bots.ajsupport.autorespond.Response;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class OnlineOnlyResponse extends Responder {
    private Response r;

    private final List<String> activators = Arrays.asList(
            "only show online",
            "only show people who are online",
            "only has online players",
            "only has online people",
            "only have online",
            "only shows on person",
            "only shows one person"
    );
    @Override
    public Response checkForResponse(Member author, Message message) {
        String c = message.getContentStripped().toLowerCase(Locale.ROOT);
        if(!has(c)) return null;
        boolean mentions_ajlb = c.contains("ajlb") || c.contains("ajleaderboards");

        if(r == null && RESPONSES != null) {
            r = new Response(mentions_ajlb ? 90 : 70, RESPONSES.get("onlineonly").getAsString());
        }
        return r;
    }

    private boolean has(String c) {
        for (String activator : activators) {
            if(c.contains(activator)) return true;
        }
        return false;
    }
}
