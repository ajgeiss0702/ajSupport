package us.ajg0702.bots.ajsupport;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutionException;

public class ReactionRoleListener extends ListenerAdapter {
    private final SupportBot bot;

    public ReactionRoleListener(SupportBot bot) {
        this.bot = bot;
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent e) {
        if(!e.getMessageId().equals("712797750915235870")) return;
        try {
            User user = e.retrieveUser().submit().get();
            Member member = e.retrieveMember().submit().get();
            Role role = e.getGuild().getRoleById(712797305723682867L);
            assert role != null;
            e.getGuild().addRoleToMember(member, role).queue();
            bot.getLogger().debug(user.getAsTag()+" added reaction");
        } catch (InterruptedException | ExecutionException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent e) {
        if(!e.getMessageId().equals("712797750915235870")) return;
        try {
            User user = e.retrieveUser().submit().get();
            Member member = e.retrieveMember().submit().get();
            Role role = e.getGuild().getRoleById(712797305723682867L);
            assert role != null;
            e.getGuild().removeRoleFromMember(member, role).queue();
            bot.getLogger().debug(user.getAsTag()+" removed reaction");
        } catch (InterruptedException | ExecutionException ex) {
            ex.printStackTrace();
        }
    }
}
