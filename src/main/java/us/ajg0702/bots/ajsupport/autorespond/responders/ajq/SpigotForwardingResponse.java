package us.ajg0702.bots.ajsupport.autorespond.responders.ajq;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import us.ajg0702.bots.ajsupport.autorespond.Responder;
import us.ajg0702.bots.ajsupport.autorespond.Response;

import java.util.Locale;

public class SpigotForwardingResponse extends Responder {

    private Response r;
    @Override
    public Response checkForResponse(Member author, Message message) {
        String c = message.getContentStripped().toLowerCase(Locale.ROOT);

        if(!c.contains("queue") && !c.contains("send") && !c.contains("ajq")) return null; // probably not about ajQueue

        boolean mentions_ajq = c.contains("ajq");

        if(
                (c.contains("menu") || c.contains("npc") || c.contains("deluxehub") || c.contains("deluxemenus") || c.contains("citizen"))
                && (c.contains("not work") || c.contains("no work") || c.contains("unknown"))
        ) {
            if(r == null && RESPONSES != null) {
                r = new Response(mentions_ajq ? 80 : 70, RESPONSES.get("spigotforward").getAsString());
            }
            return r;
        }

        return null;
    }
}
