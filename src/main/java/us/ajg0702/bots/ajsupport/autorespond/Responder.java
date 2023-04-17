package us.ajg0702.bots.ajsupport.autorespond;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

public abstract class Responder {

    public static JsonObject RESPONSES;
    public abstract Response checkForResponse(Member author, Message message);
}
