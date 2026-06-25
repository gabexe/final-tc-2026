package com.compilador;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class WebUI {
    private static final int PORT = 8080;

    public static void start() {
        int currentPort = PORT;
        HttpServer server = null;
        
        while (currentPort < PORT + 10) {
            try {
                server = HttpServer.create(new InetSocketAddress(currentPort), 0);
                break;
            } catch (IOException e) {
                currentPort++;
            }
        }
        
        if (server == null) {
            System.err.println("No se pudo iniciar el servidor web en ningún puerto disponible.");
            return;
        }

        server.createContext("/", new StaticFileHandler());
        server.createContext("/api/compile", new CompileHandler());
        
        server.setExecutor(null);
        server.start();
        System.out.println("\n\u001B[32m[Web UI]\u001B[0m Servidor iniciado exitosamente.");
        System.out.println("Abre tu navegador y ve a: \u001B[34mhttp://localhost:" + currentPort + "\u001B[0m\n");
        
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("linux")) {
                Runtime.getRuntime().exec("xdg-open http://localhost:" + currentPort);
            } else if (os.contains("win")) {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler http://localhost:" + currentPort);
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec("open http://localhost:" + currentPort);
            }
        } catch (Exception e) {
            // Ignorar
        }
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }
            
            // Evitar directory traversal
            if (path.contains("..")) {
                exchange.sendResponseHeaders(403, -1);
                return;
            }

            try (InputStream is = getClass().getResourceAsStream("/web" + path)) {
                if (is == null) {
                    String notFound = "404 Not Found";
                    exchange.sendResponseHeaders(404, notFound.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(notFound.getBytes());
                    os.close();
                    return;
                }
                
                byte[] bytes = is.readAllBytes();
                String contentType = "text/plain";
                if (path.endsWith(".html")) contentType = "text/html; charset=UTF-8";
                else if (path.endsWith(".css")) contentType = "text/css; charset=UTF-8";
                else if (path.endsWith(".js")) contentType = "application/javascript; charset=UTF-8";
                
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();
            } catch (Exception e) {
                String response = "500 Internal Server Error";
                exchange.sendResponseHeaders(500, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    static class CompileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            // Leer el body como texto plano
            InputStream is = exchange.getRequestBody();
            String code = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            if (code.trim().isEmpty()) {
                sendJsonResponse(exchange, 400, "{\"error\": \"Empty code\"}");
                return;
            }

            // Guardar en archivo temporal
            Path tempFile = Files.createTempFile("web_code_", ".cpp");
            Files.writeString(tempFile, code);

            // Capturar la salida estándar y de error
            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream capturer = new PrintStream(baos);
            System.setOut(capturer);
            System.setErr(capturer);

            try {
                // Ejecutar el compilador
                Compilador compilador = new Compilador(new String[]{tempFile.toAbsolutePath().toString()});
                compilador.compilar();
            } catch (Exception e) {
                e.printStackTrace(System.err);
            } finally {
                // Restaurar salidas
                System.setOut(originalOut);
                System.setErr(originalErr);
                
                // Limpiar archivos temporales
                try {
                    Files.deleteIfExists(tempFile);
                    String nombreBase = tempFile.toFile().getName().replaceAll("\\.[^.]*$", "");
                    Files.deleteIfExists(Path.of("output", nombreBase + ".tac"));
                    Files.deleteIfExists(Path.of("output", nombreBase + ".opt"));
                } catch (Exception e) {}
            }

            String output = baos.toString(StandardCharsets.UTF_8);
            
            // Escapar la salida para empaquetarla en JSON (incluyendo caracteres de control como ESC)
            output = output.replace("\\", "\\\\")
                           .replace("\"", "\\\"")
                           .replace("\n", "\\n")
                           .replace("\r", "")
                           .replace("\t", "\\t")
                           .replace("\u001B", "\\u001b");

            String jsonResponse = "{\"output\": \"" + output + "\"}";
            sendJsonResponse(exchange, 200, jsonResponse);
        }

        private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }
}
