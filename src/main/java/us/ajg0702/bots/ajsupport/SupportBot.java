package us.ajg0702.bots.ajsupport;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
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
        jda.addEventListener(new CommandListener(bot), new InviteListener(bot), new ReactionRoleListener(bot),
                new MessageListener(bot));

        //jda.upsertCommand("aping", "ping pong").queue();
        //Objects.requireNonNull()).upsertCommand("agping", "ping pong (guild").queue();

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
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
            Guild guild = jda.getGuildById(615715762912362565L);
            if(guild == null) {
                getLogger().warn("aj's plugins guild doesnt exist. not creating commands.");
                return;
            }
            CommandListUpdateAction commands = guild.updateCommands();
            json.keySet().forEach(name -> {
                String value = json.get(name).getAsString();
                getLogger().debug("Adding command "+name);
                commands.addCommands(new CommandData(name, cutString(value, 100)));
                //guild.upsertCommand(name, value.substring(0, Math.min(99, value.length()))).queue();
            });
            commands.addCommands(new CommandData("remove", "Unregister commands (aj only)"));
            commands.addCommands(new CommandData("stop", "Stop the bot (aj only)"));
            commands.addCommands(
                    new CommandData("reply", "reply to a certain message (aj only)")
                            .addOption(OptionType.STRING, "message_id", "The message to reply to", true)
                            .addOption(OptionType.STRING, "response_name", "The response to send (e.g. onlineonly)", true)
            );
            commands.addCommands(
                    new CommandData("ticketban", "Ban someone from creating tickets (aj only)")
                            .addOption(OptionType.USER, "user", "The user to ban", true)
                            .addOption(OptionType.STRING, "reason", "The ban reason", false)
            );
            commands.queue();
        }).start();
    }

    public static String cutString(String string, int length) {
        if(string.length() < length) return string;
        return string.substring(0, length-1)+"…";
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
