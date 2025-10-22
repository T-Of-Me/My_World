import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Instant;

public class App {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", exchange -> {
            String body = "{" +
                    "\"message\":\"Hello from Java in Docker!\"," +
                    "\"time\":\"" + Instant.now() + "\"," +
                    "\"hostname\":\"" + InetAddress.getLocalHost().getHostName() + "\"," +
                    "\"java\":\"" + System.getProperty("java.version") + "\"" +
                    "}";
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            byte[] data = body.getBytes();
            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }
        });
        server.setExecutor(null);
        System.out.println("Server listening on port " + port);
        server.start();
    }
}