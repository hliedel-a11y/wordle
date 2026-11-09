import java.util.Random;

public class WordList {
    private static final String[] WORDS = {
        "APPLE", "HOUSE", "WORLD", "HAPPY", "MUSIC",
        "PLANE", "STONE", "CRANE", "BREAD", "CLOUD",
        "GRAPE", "LIGHT", "STORM", "PIANO", "BRAVE"
    };
    private static final Random RNG = new Random();

    public static String random() {
        return WORDS[RNG.nextInt(WORDS.length)];
    }

    public static int size() {
        return WORDS.length;
    }
}
