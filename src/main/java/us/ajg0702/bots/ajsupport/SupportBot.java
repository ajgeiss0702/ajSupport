package us.ajg0702.bots.ajsupport;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.internal.utils.JDALogger;
import org.slf4j.Logger;
import us.ajg0702.bots.ajsupport.autorespond.AutoRespondManager;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ExecutionException;

public class SupportBot {

    private final JDA jda;
    private final Logger logger;
    private JsonObject json;
    private final UpdateManager updateManager = new UpdateManager();

    public static void main(String[] args) throws InterruptedException {
        JDABuilder builder = JDABuilder.createDefault(args[0]);
        SupportBot bot;

        try {
            bot = new SupportBot(builder
                    .setAutoReconnect(true)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_INVITES,
                            GatewayIntent.GUILD_MESSAGE_REACTIONS,
                            GatewayIntent.MESSAGE_CONTENT,
                            GatewayIntent.GUILD_MESSAGES)
                    .build());
        } catch (InterruptedException | InvalidTokenException | IllegalArgumentException e) {
            e.printStackTrace();
            System.exit(1);
            return;
        }

        JDA jda = bot.getJDA();
        jda.addEventListener(new CommandListener(bot), new InviteListener(bot), new ReactionRoleListener(bot),
                new MessageListener(bot), new ContextListener(bot));

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
        jda.addEventListener(new AutoRespondManager(json, this));


        new Thread(() -> {
            Guild guild = jda.getGuildById(615715762912362565L);
            if(guild == null) {
                getLogger().warn("aj's plugins guild doesnt exist. not creating commands.");
                return;
            }
            CommandListUpdateAction commands = guild.updateCommands();
            CommandListUpdateAction globalCommands = jda.updateCommands();
            json.keySet().forEach(name -> {
                String value = json.get(name).getAsString();
                getLogger().debug("Adding command "+name);

//                commands.addCommands(Commands.slash(name, cutString(value, 100)));
                globalCommands.addCommands(
                        Commands.slash(name, cutString(value, 100))
                                .setContexts(
                                        InteractionContextType.BOT_DM,
                                        InteractionContextType.PRIVATE_CHANNEL,
                                        InteractionContextType.GUILD
                                )
                                .setIntegrationTypes(
                                        IntegrationType.GUILD_INSTALL,
                                        IntegrationType.USER_INSTALL
                                )
                );
                //guild.upsertCommand(name, value.substring(0, Math.min(99, value.length()))).queue();
            });
            globalCommands.addCommands(
                    Commands.message("Upload Text File")
                            .setContexts(
                                    InteractionContextType.BOT_DM,
                                    InteractionContextType.PRIVATE_CHANNEL,
                                    InteractionContextType.GUILD
                            )
                            .setIntegrationTypes(
                                    IntegrationType.GUILD_INSTALL,
                                    IntegrationType.USER_INSTALL
                            )
            );
            commands.addCommands(
                    Commands.slash("remove", "Unregister commands (aj only)"),
                    Commands.slash("update", "Update the bot (aj only)"),
                    Commands.slash("reply", "reply to a certain message (aj only)")
                            .addOption(OptionType.STRING, "message_id", "The message to reply to", true)
                            .addOption(OptionType.STRING, "response_name", "The response to send (e.g. onlineonly)", true),
                    Commands.slash("upload", "Upload the first attachment from a message to the paste site (aj only)")
                            .addOption(OptionType.STRING, "message_id", "The message to get attachments from", true),
                    Commands.message("Reply"),
                    Commands.message("Add to Vectorize")
                            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
                    Commands.message("Remove from Vectorize")
                            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
            );
            commands.addCommands(
                    Commands.slash("ticketban", "Ban someone from creating tickets (aj only)")
                            .addOption(OptionType.USER, "user", "The user to ban", true)
                            .addOption(OptionType.STRING, "reason", "The ban reason", false)
            );
            commands.queue();
            globalCommands.queue();

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

    public UpdateManager getUpdateManager() {
        return updateManager;
    }

    public void reply(Message message, User user, String key) throws EchoException {
        TextChannel channel = getJDA().getTextChannelById(698756204801032202L);
        if(channel == null) {
            getLogger().error("Cannot find logger-log channel for aj's plugins!");
            throw new EchoException("Cannot find log channel!");
        }
        channel.sendMessageEmbeds(
                new EmbedBuilder()
                        .setDescription("<@"+user.getId()+"> is replying\n" +
                                "Reply to "+message.getAuthor().getName()+": " +
                                SupportBot.cutString(
                                        message.getContentStripped().replaceAll("\n", " "),
                                        100
                                )
                        )
                        .build()
        ).queue();
        logger.debug("Replied with "+key);

        message.reply(getJson().get(key).getAsString()).queue();
    }
}
