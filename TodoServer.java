import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;

public class TodoServer {
    static class Todo {
        int id;
        String title;
        boolean done;

        Todo(int id, String title) {
            this.id = id;
            this.title = title;
            this.done = false;
        }

        public String toJson() {
            return String.format("{\"id\": %d, \"title\": \"%s\", \"done\": %s}", id, title, done);
        }
    }

    private static List<Todo> todos = new ArrayList<>();
    private static int nextId = 1;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(5000), 0);
        server.createContext("/todos", new TodosHandler());
        server.createContext("/todos/", new TodoIdHandler()); // For /todos/<id>
        System.out.println("Server started at http://localhost:5000/");
        server.start();
    }

    // Handle /todos and POST /todos
    static class TodosHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            if (method.equalsIgnoreCase("GET")) {
                String response = todos.stream()
                        .map(Todo::toJson)
                        .collect(Collectors.joining(",", "[", "]"));
                writeResponse(exchange, response, 200);
            } else if (method.equalsIgnoreCase("POST")) {
                String body = readRequestBody(exchange);
                String title = parseJsonField(body, "title");

                if (title == null) {
                    writeResponse(exchange, "{\"error\": \"Missing title\"}", 400);
                    return;
                }

                Todo todo = new Todo(nextId++, title);
                todos.add(todo);
                writeResponse(exchange, todo.toJson(), 201);
            } else {
                writeResponse(exchange, "{\"error\": \"Method Not Allowed\"}", 405);
            }
        }
    }

    // Handle /todos/<id> GET, PUT, DELETE
    static class TodoIdHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            if (parts.length != 3) {
                writeResponse(exchange, "{\"error\": \"Bad Request\"}", 400);
                return;
            }

            int id;
            try {
                id = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                writeResponse(exchange, "{\"error\": \"Invalid ID\"}", 400);
                return;
            }

            Optional<Todo> todoOpt = todos.stream().filter(t -> t.id == id).findFirst();

            if (method.equalsIgnoreCase("GET")) {
                if (todoOpt.isPresent()) {
                    writeResponse(exchange, todoOpt.get().toJson(), 200);
                } else {
                    writeResponse(exchange, "{\"error\": \"Not found\"}", 404);
                }

            } else if (method.equalsIgnoreCase("PUT")) {
                if (!todoOpt.isPresent()) {
                    writeResponse(exchange, "{\"error\": \"Not found\"}", 404);
                    return;
                }

                String body = readRequestBody(exchange);
                String title = parseJsonField(body, "title");
                String doneStr = parseJsonField(body, "done");

                Todo todo = todoOpt.get();
                if (title != null) todo.title = title;
                if (doneStr != null) todo.done = Boolean.parseBoolean(doneStr);

                writeResponse(exchange, todo.toJson(), 200);

            } else if (method.equalsIgnoreCase("DELETE")) {
                if (todoOpt.isPresent()) {
                    todos.remove(todoOpt.get());
                    writeResponse(exchange, "{\"message\": \"Deleted\"}", 200);
                } else {
                    writeResponse(exchange, "{\"error\": \"Not found\"}", 404);
                }
            } else {
                writeResponse(exchange, "{\"error\": \"Method Not Allowed\"}", 405);
            }
        }
    }

    // Helpers

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream in = exchange.getRequestBody();
        return new BufferedReader(new InputStreamReader(in))
                .lines()
                .collect(Collectors.joining("\n"));
    }

    private static void writeResponse(HttpExchange exchange, String response, int statusCode) throws IOException {
        byte[] bytes = response.getBytes("UTF-8");
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    // Very basic JSON parser (for simple { "title": "something", "done": true })
    private static String parseJsonField(String json, String key) {
        String regex = "\"" + key + "\"\\s*:\\s*(\"[^\"]*\"|true|false)";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(regex).matcher(json);
        if (matcher.find()) {
            String value = matcher.group(1);
            return value.replace("\"", "");
        }
        return null;
    }
}
