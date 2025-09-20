package us.ajg0702.bots.ajsupport.autorespond.responders.ajlb;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import us.ajg0702.bots.ajsupport.autorespond.Responder;
import us.ajg0702.bots.ajsupport.autorespond.Response;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class TrojanResponder extends Responder {
    private Response r;
    @Override
    public Response checkForResponse(Member author, Message message) {
        List<String> textAttachments = message.getAttachments()
                .stream()
                .filter(a -> a.getContentType() == null || a.getContentType().contains("text"))
                .map(a -> {
                    try {
                        return new BufferedReader(new InputStreamReader(a.getProxy().download().get()))
                                .lines().collect(Collectors.joining("\n"));
                    } catch (InterruptedException | ExecutionException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
        boolean found = false;
        for (String textAttachment : textAttachments) {
            if(textAttachment.contains("Illegal local variable table start_pc 72 in method 'void us.ajg0702.leaderboards.LeaderboardPlugin.onEnable()'")) {
                found = true;
                break;
            }
        }
        if(!found) return null;
//        if(!c.contains("board does not exist") && !c.contains("bdne")) return null;

        if(r == null && RESPONSES != null) {
            r = new Response(90, RESPONSES.get("trojan").getAsString());
        }
        return r;
    }
}
