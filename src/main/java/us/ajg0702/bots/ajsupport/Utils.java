package us.ajg0702.bots.ajsupport;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Utils {

    /**
     * Uploads the specified attachment
     * @param bot the bot
     * @param attachment the attachment to upload
     * @return the url (e.g. https://paste.ajg0702.us/7B8Rmr8 )
     */
    public static String uploadAttachment(SupportBot bot, Message.Attachment attachment) throws IOException, ExecutionException, InterruptedException, TimeoutException {
        URL url = new URL("https://paste.ajg0702.us/post");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", "ajSupport/1.0.0");
        if(attachment.getContentType() != null) {
            con.setRequestProperty("Content-Type", attachment.getContentType());
        } else {
            con.setRequestProperty("Content-Type", "text/plain");
        }
        if("gz".equalsIgnoreCase(attachment.getFileExtension())) {
            con.setRequestProperty("Content-Encoding", "gzip");
        }
        bot.getLogger().info("Sending with " + con.getRequestProperty("Content-Type"));
        con.setDoOutput(true);

        try (OutputStream os = con.getOutputStream()) {
            InputStream inputStream = attachment.getProxy().download().get(15, TimeUnit.SECONDS);
            byte[] input = inputStream.readAllBytes();
            os.write(input, 0, input.length);
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }

            JsonObject responseJson = new Gson().fromJson(response.toString(), JsonObject.class);

            return "https://paste.ajg0702.us/" + responseJson.get("key").getAsString();
        }
    }
}
