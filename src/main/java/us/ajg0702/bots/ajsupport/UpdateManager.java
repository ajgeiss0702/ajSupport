package us.ajg0702.bots.ajsupport;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.InteractionHook;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class UpdateManager {
    private String state = "No current state";
    private final List<InteractionHook> updateMessages = new ArrayList<>();

    private boolean running = false;

    public void update(InteractionHook message) {
        updateMessages.add(message);
        if(running) {
            message.editOriginal(state).queue();
            return;
        }
        running = true;
        new Thread(() -> {
            setState("<a:loading:1350993899241472082> Pulling changes..");
            try {
                new ProcessBuilder("/bin/bash", "-c", "git pull")
                        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                        .redirectError(ProcessBuilder.Redirect.INHERIT)
                        .start().waitFor(20, TimeUnit.SECONDS);
                setState("<a:loading:1350993899241472082> Compiling...");
                new ProcessBuilder("/bin/bash", "-c", "./gradlew clean shadowJar")
                        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                        .redirectError(ProcessBuilder.Redirect.INHERIT)
                        .start().waitFor(5, TimeUnit.MINUTES);
                setState(":white_check_mark: Done. Restarting now!");
                new ProcessBuilder("/bin/bash", "-c", "sudo systemctl restart ajsupport &")
                        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                        .redirectError(ProcessBuilder.Redirect.INHERIT)
                        .start().waitFor(5, TimeUnit.SECONDS);
                updateMessages.clear();
                running = false;
            } catch (IOException | InterruptedException e) {
                setState(":warning: Errored: " + e.getMessage());
                updateMessages.clear();
                running = false;
                throw new RuntimeException(e);
            }
        }).start();
    }

    public void setState(String state) {
        this.state = state;
        updateMessages.forEach(e -> e.editOriginal(state).queue());
    }
}
