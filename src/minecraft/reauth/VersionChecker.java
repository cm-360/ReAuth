package reauth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cpw.mods.fml.common.versioning.ComparableVersion;
import cpw.mods.fml.common.versioning.DefaultArtifactVersion;
import cpw.mods.fml.common.versioning.VersionParser;
import cpw.mods.fml.common.versioning.VersionRange;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Level;

class VersionChecker implements Runnable {

    private static final String url = "https://raw.githubusercontent.com/TechnicianLP/ReAuth/master/version.json";
    private static final String group = "1.5.2";

    private static boolean isLatestVersion = true;
    private static String latestMessage = (char) 167 + "aUpdate Available!";
    private static boolean isVersionAllowed = true;
    private static String allowedMessage = (char) 167 + "4Critical Update Available!";
    private static long run = 0;

    @Override
    public void run() {
        Main.log.info("Looking for Updates");
        InputStream in = null;
        try {
            in = new URL(url).openStream();
            JsonObject json = new JsonParser().parse(new InputStreamReader(in, "UTF-8")).getAsJsonObject();
            if (json.has(group)) {
                JsonObject data = json.getAsJsonObject(group);
                String latest = data.getAsJsonPrimitive("latest").getAsString();
                latestMessage = (char) 167 + "aUpdate Available!";
                if (data.has("latest-message"))
                    latestMessage += " - " + data.getAsJsonPrimitive("latest-message").getAsString();
                String allowed = null;
                if (data.has("allowed"))
                    allowed = data.getAsJsonPrimitive("allowed").getAsString();
                allowedMessage = (char) 167 + "4Critical Update Available!";
                if (data.has("allowed-message"))
                    allowedMessage += " - " + data.getAsJsonPrimitive("allowed-message").getAsString();

                ComparableVersion rLocal = new ComparableVersion(Main.meta.version);
                ComparableVersion rRecommended = new ComparableVersion(latest);
                isLatestVersion = rLocal.compareTo(rRecommended) >= 0;
                if (allowed == null) {
                    isVersionAllowed = true;
                } else {
                    VersionRange rAllowed = VersionParser.parseRange(allowed);
                    isVersionAllowed = rAllowed.containsVersion(new DefaultArtifactVersion(Main.meta.version));
                }
            } else {
                Main.log.log(Level.SEVERE, "Looking for Updates - Failed: Unknown Version: \"" + group + "\"");
            }
            run = System.currentTimeMillis();
            Main.log.info("Looking for Updates - Finished");
            return;
        } catch (Exception e) {
            Main.log.log(Level.SEVERE, "Looking for Updates - Failed", e);
        } finally {
            IOUtils.closeQuietly(in);
        }
        run = System.currentTimeMillis() - (long) ((1000 * 60 * 60) * 0.75);
    }

    static boolean isLatestVersion() {
        return isLatestVersion;
    }

    static boolean isVersionAllowed() {
        return isVersionAllowed;
    }

    /**
     * returns update-message (hidden if the forge-version-checker is disabled)(allowed-checker unaffected)
     */
    static String getUpdateMessage() {
        if (!isVersionAllowed)
            return allowedMessage;
        if (!isLatestVersion)
            return latestMessage;
        return null;
    }

    /**
     * should the check run? true if last check is older than 60 minutes (or 15 minutes if the last check failed)
     */
    static boolean shouldRun() {
        return System.currentTimeMillis() - run > (1000 * 60 * 60);
    }

    static void update() {
        Thread t = new Thread(new VersionChecker(), "ReAuth-VersionChecker");
        t.setDaemon(true);
        t.start();
    }

}