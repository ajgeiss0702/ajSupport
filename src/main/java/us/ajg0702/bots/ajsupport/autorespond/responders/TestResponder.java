package us.ajg0702.bots.ajsupport.autorespond.responders;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import us.ajg0702.bots.ajsupport.autorespond.Responder;
import us.ajg0702.bots.ajsupport.autorespond.Response;

public class TestResponder extends Responder {
    @Override
    public Response checkForResponse(Member author, Message message) {
        if(author.getIdLong() != 171160105155297282L) return null;
        if(!message.getContentRaw().equals("test auto response")) return null;

        return new Response(100, "This is a test response!");
    }
}
