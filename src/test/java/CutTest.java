import org.junit.jupiter.api.Test;
import us.ajg0702.bots.ajsupport.SupportBot;

public class CutTest {
    @Test
    void CutTest() throws Exception {
        String c;

        c = SupportBot.cutString("hello there", 50);
        if(!c.equals("hello there")) {
            throw new Exception("Shorter non-cut is not equal: "+c);
        }

        c = SupportBot.cutString("hello there! How are you today?", 30);
        if(c.length() > 30) {
            throw new Exception("Cut is too long: "+c);
        }
        System.out.println(c.length()+" "+c);

        c = SupportBot.cutString("hello there! How are you today? I'm doing well, thanks", 12);
        if(c.length() > 12) {
            throw new Exception("Cut is too long: "+c);
        }
        System.out.println(c.length()+" "+c);
    }
}
