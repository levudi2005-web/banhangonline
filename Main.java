import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws IOException {

        int port = Integer.parseInt(
                System.getenv().getOrDefault("PORT", "10000")
        );

        HttpServer server = HttpServer.create(
                new InetSocketAddress("0.0.0.0", port),
                0
        );

        // Trang chủ
        server.createContext("/", Main::handleHome);

        // CSS
        server.createContext("/style.css", Main::handleCss);

        // API người dùng
        server.createContext("/api/user", Main::handleUser);

        server.start();

        System.out.println("Server started on port " + port);
    }


    // =========================
    // TRANG CHỦ
    // =========================

    private static void handleHome(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendResponse(
                    exchange,
                    405,
                    "text/plain; charset=UTF-8",
                    "Method Not Allowed"
            );
            return;
        }

        Path file = Path.of("index.html");

        if (!Files.exists(file)) {
            sendResponse(
                    exchange,
                    404,
                    "text/plain; charset=UTF-8",
                    "Không tìm thấy index.html"
            );
            return;
        }

        String html = Files.readString(file);

        sendResponse(
                exchange,
                200,
                "text/html; charset=UTF-8",
                html
        );
    }


    // =========================
    // CSS
    // =========================

    private static void handleCss(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendResponse(
                    exchange,
                    405,
                    "text/plain; charset=UTF-8",
                    "Method Not Allowed"
            );
            return;
        }

        Path file = Path.of("style.css");

        if (!Files.exists(file)) {
            sendResponse(
                    exchange,
                    404,
                    "text/plain; charset=UTF-8",
                    "Không tìm thấy style.css"
            );
            return;
        }

        String css = Files.readString(file);

        sendResponse(
                exchange,
                200,
                "text/css; charset=UTF-8",
                css
        );
    }


    // =========================
    // API USER
    // =========================

    private static void handleUser(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendResponse(
                    exchange,
                    405,
                    "application/json; charset=UTF-8",
                    "{\"error\":\"Method Not Allowed\"}"
            );
            return;
        }

        String query = exchange.getRequestURI().getRawQuery();

        Map<String, String> params = parseQuery(query);

        String name = params.getOrDefault("name", "").trim();

        if (name.isEmpty()) {

            sendResponse(
                    exchange,
                    400,
                    "application/json; charset=UTF-8",
                    """
                    {
                        "error": "Vui lòng nhập tên"
                    }
                    """
            );

            return;
        }


        // Dữ liệu mẫu
        if (name.equalsIgnoreCase("USaD")) {

            String response = """
                    {
                        "name": "USaD",
                        "email": "johndoe@example.com",
                        "job": "Software Engineer"
                    }
                    """;

            sendResponse(
                    exchange,
                    200,
                    "application/json; charset=UTF-8",
                    response
            );

        } else {

            String response = """
                    {
                        "error": "Không tìm thấy người dùng"
                    }
                    """;

            sendResponse(
                    exchange,
                    404,
                    "application/json; charset=UTF-8",
                    response
            );
        }
    }


    // =========================
    // ĐỌC QUERY PARAMETER
    // =========================

    private static Map<String, String> parseQuery(String query) {

        Map<String, String> params = new HashMap<>();

        if (query == null || query.isEmpty()) {
            return params;
        }

        String[] pairs = query.split("&");

        for (String pair : pairs) {

            String[] parts = pair.split("=", 2);

            if (parts.length == 2) {

                String key = URLDecoder.decode(
                        parts[0],
                        StandardCharsets.UTF_8
                );

                String value = URLDecoder.decode(
                        parts[1],
                        StandardCharsets.UTF_8
                );

                params.put(key, value);
            }
        }

        return params;
    }


    // =========================
    // GỬI RESPONSE
    // =========================

    private static void sendResponse(
            HttpExchange exchange,
            int statusCode,
            String contentType,
            String response
    ) throws IOException {

        byte[] data = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                contentType
        );

        exchange.sendResponseHeaders(
                statusCode,
                data.length
        );

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(data);
        }
    }
}