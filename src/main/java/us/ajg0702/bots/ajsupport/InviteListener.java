package us.ajg0702.bots.ajsupport;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

public class InviteListener extends ListenerAdapter {

    private final SupportBot bot;

    Map<String, Integer> invites = new HashMap<>();

    public InviteListener(SupportBot bot) {
        this.bot = bot;

        Guild guild = bot.getJDA().getGuildById(615715762912362565L);
        if(guild == null) {
            bot.getLogger().warn("aj's plugins guild doesnt exist. not creating invite list.");
            return;
        }
        try {
            invites = getInviteCounts(guild);
        } catch (ExecutionException | InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent e) {
        String mention = "<@"+e.getUser().getId()+">";
        bot.getLogger().debug(e.getUser().getAsTag()+" left!");
        TextChannel welcomeChannel = bot.getJDA().getTextChannelById(615723022917304321L);
        if(welcomeChannel == null) {
            bot.getLogger().error("Cannot find welcome channel for aj's plugins! Cannot send leave message");
        } else {
            welcomeChannel.sendMessage( mention + " left.").queue();
        }
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent e) {
        String mention = "<@"+e.getUser().getId()+">";
        bot.getLogger().debug(e.getUser().getAsTag()+" joined!");

        TextChannel welcomeChannel = bot.getJDA().getTextChannelById(615723022917304321L);
        if(welcomeChannel == null) {
            bot.getLogger().error("Cannot find welcome channel for aj's plugins! Cannot send join message");
        } else {
            welcomeChannel.sendMessage("Hey " + mention + ", welcome to the discord server for aj's Plugins! If you are here for support, make sure to read <#1064604482752762076>!").queue();
        }

        TextChannel channel = bot.getJDA().getTextChannelById(698756204801032202L);
        if(channel == null) {
            bot.getLogger().error("Cannot find logger-log channel for aj's plugins!");
            return;
        }
        try {
            Thread.sleep(100);
            Map<String, Integer> newUses = getInviteCounts(e.getGuild());

            AtomicReference<String> usedCode = new AtomicReference<>("");
            newUses.forEach((code, uses) -> {
                if(!invites.containsKey(code)) return;
                //bot.getLogger().debug(code+" "+invites.get(code)+" "+uses);
                if(invites.get(code) < uses) {
                    usedCode.set(code);
                }
            });

            invites = newUses;

            if(usedCode.get().isEmpty()) {
                channel.sendMessage(mention+" joined with an unknown invite! ").queue();
                return;
            }

            Invite invite = getInvite(e.getGuild(), usedCode.get());

            if(invite == null) {
                channel.sendMessage("Failed to get invite "+usedCode.get()+" for "+mention).queue();
                return;
            }

            Invite.Channel inviteChannel = invite.getChannel();
            if(inviteChannel == null) {
                channel.sendMessage("Failed to get channel id for invite "+ usedCode.get()+" for "+mention).queue();
                return;
            }
            channel.sendMessage(mention+" for <#"+inviteChannel.getId()+">").queue();

        } catch (InterruptedException | ExecutionException ex) {
            ex.printStackTrace();
            channel.sendMessage("Exception for "+mention).queue();
        }
    }


    @Override
    public void onGuildInviteCreate(@NotNull GuildInviteCreateEvent e) {
        bot.getLogger().debug("Adding newly-created invite "+e.getCode());
        invites.put(e.getCode(), e.getInvite().getUses());
    }

    private Map<String, Integer> getInviteCounts(Guild guild) throws ExecutionException, InterruptedException {
        Map<String, Integer> newInvites = new HashMap<>();
        for (Invite invite : guild.retrieveInvites().submit().get()) {
            newInvites.put(invite.getCode(), invite.getUses());
        }
        return newInvites;
    }

    private Invite getInvite(Guild guild, String code) throws ExecutionException, InterruptedException {
        for (Invite invite : guild.retrieveInvites().submit().get()) {
            if(invite.getCode().equalsIgnoreCase(code)) {
                return invite;
            }
        }
        return null;
    }
}
