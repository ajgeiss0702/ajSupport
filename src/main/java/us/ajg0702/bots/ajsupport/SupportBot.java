package us.ajg0702.bots.ajsupport;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.internal.utils.JDALogger;
import org.slf4j.Logger;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ExecutionException;

public class SupportBot {

    private final JDA jda;
    private final Logger logger;
    private JsonObject json;

    public static void main(String[] args) throws InterruptedException {
        JDABuilder builder = JDABuilder.createDefault(args[0]);
        SupportBot bot;

        try {
            bot = new SupportBot(builder
                    .setAutoReconnect(true)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_INVITES,
                            GatewayIntent.GUILD_MESSAGE_REACTIONS)
                    .build());
        } catch (LoginException e) {
            e.printStackTrace();
            System.exit(1);
            return;
        }

        JDA jda = bot.getJDA();
        jda.addEventListener(new CommandListener(bot), new InviteListener(bot), new ReactionRoleListener(bot));

        //jda.upsertCommand("aping", "ping pong").queue();
        //Objects.requireNonNull()).upsertCommand("agping", "ping pong (guild").queue();

    }

    private SupportBot(JDA jda) throws InterruptedException {
        this.jda = jda;
        logger = JDALogger.getLog(jda.getSelfUser().getName());
        getLogger().info("Hello!");

        jda.awaitReady();

        StringBuilder jsonRaw = new StringBuilder();
        try {
            Files.readAllLines(new File("responses.json").toPath()).forEach(line -> jsonRaw.append(line).append("\n"));
        } catch (IOException e) {
            getLogger().error("Unable to read responses.json");
            e.printStackTrace();
            System.exit(2);
            return;
        }
        json = new Gson().fromJson(jsonRaw.toString(), JsonObject.class);


        new Thread(() -> {
            try {
                Guild guild = jda.getGuildById(615715762912362565L);
                if(guild == null) {
                    getLogger().warn("aj's plugins guild doesnt exist. not creating commands.");
                    return;
                }

                guild.retrieveCommands().submit().get().forEach(command -> {
                    if(!(command.getName().equals("remove") || command.getName().equals("stop")) && !json.keySet().contains(command.getName())) {
                        getLogger().debug("Removing non-existant command "+command.getName()+" ("+command.getId()+")");
                        guild.deleteCommandById(command.getId()).queue();
                    }
                });
                json.keySet().forEach(name -> {
                    String value = json.get(name).getAsString();
                    getLogger().debug("Adding command "+name);
                    guild.upsertCommand(name, value.substring(0, Math.min(99, value.length()))).queue();
                });
                guild.upsertCommand("remove", "Unregister commands (aj only)").queue();
                guild.upsertCommand("stop", "Stop the bot (aj only)").queue();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public JDA getJDA() {
        return jda;
    }

    public Logger getLogger() {
        return logger;
    }

    public JsonObject getJson() {
        return json;
    }
}
