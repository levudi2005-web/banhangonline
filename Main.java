import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) throws IOException {
        // Your code here
        // HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "10000"));
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/user", exchange -> {
            String response = """
                    String response = "Hello, World!";
                    <h2> Thong tin nguoi dung: </h2>
                    <p> Ten: USaD </p>
                    <p> Email: johndoe@example.com </p>
                    <p> Nghe nghiep: Software Engineer </p>
                    """;
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
        });
        server.start();
        System.out.println("Server started on port" + port);
    }
}
