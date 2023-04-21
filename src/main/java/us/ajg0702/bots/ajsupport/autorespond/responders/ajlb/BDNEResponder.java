package us.ajg0702.bots.ajsupport.autorespond.responders.ajlb;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import us.ajg0702.bots.ajsupport.autorespond.Responder;
import us.ajg0702.bots.ajsupport.autorespond.Response;

import java.util.Locale;

public class BDNEResponder extends Responder {
    private Response r;
    @Override
    public Response checkForResponse(Member author, Message message) {
        String c = message.getContentStripped().toLowerCase(Locale.ROOT);
        if(!c.contains("board does not exist") && !c.contains("bdne")) return null;

        if(r == null && RESPONSES != null) {
            r = new Response(90, RESPONSES.get("board").getAsString());
        }
        return r;
    }
}
