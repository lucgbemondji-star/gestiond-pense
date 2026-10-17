import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyStore;
import java.time.Instant;
import java.util.*;

public class ExpenseServer {
    private static final int PORT = 9090;
    private static final Path DATA_FILE = Path.of(
            System.getProperty("user.home"), ".gestionnaire-depenses", "expenses.db");
    private static final List<Expense> EXPENSES = loadExpenses();

    private static final String KEYSTORE_FILE = "keystore-local.p12";
    private static final char[] KEYSTORE_PASSWORD = "password123".toCharArray();

    public static void main(String[] args) throws Exception {
        SSLContext sslContext = createSSLContext();

        HttpsServer server = HttpsServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            @Override
            public void configure(HttpsParameters params) {
                params.setSSLParameters(getSSLContext().getDefaultSSLParameters());
            }
        });

        server.createContext("/api/expenses", ExpenseServer::handleExpenses);
        server.createContext("/", ExpenseServer::handleIndex);

        server.start();
        System.out.println("Interface HTTPS prête : https://127.0.0.1:" + PORT + "/");
        System.out.println("API HTTPS prête : https://127.0.0.1:" + PORT + "/api/expenses");
    }

    private static void handleIndex(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Méthode non autorisée.");
            return;
        }
        if (!"/".equals(path) && !"/index.html".equals(path)) {
            sendText(exchange, 404, "Ressource introuvable.");
            return;
        }

        String html = Files.readString(Path.of("index.html"), StandardCharsets.UTF_8);
        send(exchange, 200, "text/html; charset=utf-8", html);
    }

    private static SSLContext createSSLContext() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(KEYSTORE_FILE)) {
            keyStore.load(fis, KEYSTORE_PASSWORD);
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, KEYSTORE_PASSWORD);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return sslContext;
    }

    private static void handleExpenses(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            boolean collectionPath = "/api/expenses".equals(path) || "/api/expenses/".equals(path);

            if ("GET".equals(method) && collectionPath) {
                synchronized (EXPENSES) {
                    sendJson(exchange, 200, expensesAsJson());
                }
                return;
            }

            String[] parts = path.substring("/api/expenses".length()).split("/");

            if ("POST".equals(method) && collectionPath) {
                Map<String, String> fields = readForm(exchange);
                String title = requiredField(fields, "title");
                double amount = parseAmount(fields.get("amount"));
                String currency = fields.getOrDefault("currency", "XOF").toUpperCase(Locale.ROOT);
                String date = fields.getOrDefault("date", Instant.now().toString().substring(0, 10));

                if (!Expense.isValidCurrency(currency)) {
                    sendText(exchange, 400, "Devise non supportée (Utilisez EUR, USD, XOF, GBP).");
                    return;
                }

                // Appel de l'API externe pour conversion vers la monnaie locale (XOF)
                double convertedAmount = CurrencyConverter.convertToLocalCurrency(amount, currency);

                Expense expense = new Expense(
                        UUID.randomUUID().toString(),
                        title,
                        amount,
                        currency,
                        convertedAmount,
                        date,
                        null
                );

                synchronized (EXPENSES) {
                    EXPENSES.add(0, expense);
                    saveExpenses();
                    sendJson(exchange, 201, expense.toJson());
                }
                return;
            }

            if (collectionPath || parts.length < 2 || parts[1].isEmpty()) {
                sendText(exchange, 404, "Route introuvable.");
                return;
            }

            String id = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            synchronized (EXPENSES) {
                Expense expense = EXPENSES.stream().filter(e -> e.getId().equals(id)).findFirst().orElse(null);
                if (expense == null) {
                    sendText(exchange, 404, "Dépense introuvable.");
                    return;
                }

                if ("PUT".equals(method)) {
                    Map<String, String> fields = readForm(exchange);
                    String title = requiredField(fields, "title");
                    double amount = parseAmount(fields.get("amount"));
                    String currency = fields.getOrDefault("currency", expense.getCurrency()).toUpperCase(Locale.ROOT);
                    String date = fields.getOrDefault("date", expense.getDate());
                    if (!Expense.isValidCurrency(currency)) {
                        sendText(exchange, 400, "Devise non supportée (Utilisez EUR, USD, XOF, GBP).");
                        return;
                    }
                    double convertedAmount = CurrencyConverter.convertToLocalCurrency(amount, currency);

                    expense.setTitle(title);
                    expense.setAmount(amount);
                    expense.setCurrency(currency);
                    expense.setDate(date);
                    expense.setConvertedAmount(convertedAmount);

                    saveExpenses();
                    sendJson(exchange, 200, expense.toJson());
                } else if ("DELETE".equals(method)) {
                    EXPENSES.remove(expense);
                    saveExpenses();
                    sendText(exchange, 204, "");
                } else {
                    sendText(exchange, 405, "Méthode non autorisée.");
                }
            }

        } catch (IllegalArgumentException e) {
            sendText(exchange, 400, e.getMessage());
        } catch (CurrencyConverter.ConversionException e) {
            System.err.println("Erreur de conversion de devise : " + e.getMessage());
            sendText(exchange, 502, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            sendText(exchange, 500, "Erreur interne du serveur.");
        } finally {
            exchange.close();
        }
    }

    private static String requiredField(Map<String, String> fields, String key) {
        String val = fields.getOrDefault(key, "").trim();
        if (val.isEmpty()) throw new IllegalArgumentException("Le champ '" + key + "' est obligatoire.");
        return val;
    }

    private static double parseAmount(String val) {
        try {
            double amount = Double.parseDouble(val);
            if (!Double.isFinite(amount)) throw new NumberFormatException();
            return amount;
        } catch (Exception e) {
            throw new IllegalArgumentException("Montant invalide.");
        }
    }

    private static Map<String, String> readForm(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> fields = new LinkedHashMap<>();
        for (String pair : body.split("&")) {
            if (pair.isEmpty()) continue;
            String[] entry = pair.split("=", 2);
            fields.put(URLDecoder.decode(entry[0], StandardCharsets.UTF_8),
                    entry.length == 2 ? URLDecoder.decode(entry[1], StandardCharsets.UTF_8) : "");
        }
        return fields;
    }

    private static String expensesAsJson() {
        return "[" + String.join(",", EXPENSES.stream().map(Expense::toJson).toList()) + "]";
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        send(exchange, status, "application/json; charset=utf-8", json);
    }

    private static void sendText(HttpExchange exchange, int status, String text) throws IOException {
        send(exchange, status, "text/plain; charset=utf-8", text);
    }

    private static void send(HttpExchange exchange, int status, String type, String text) throws IOException {
        byte[] content = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.sendResponseHeaders(status, status == 204 ? -1 : content.length);
        if (status != 204) {
            try (var output = exchange.getResponseBody()) { output.write(content); }
        }
    }

    private static List<Expense> loadExpenses() {
        List<Expense> result = new ArrayList<>();
        if (!Files.isRegularFile(DATA_FILE)) return result;
        try {
            for (String line : Files.readAllLines(DATA_FILE, StandardCharsets.UTF_8)) {
                String[] f = line.split("\\t", -1);
                if (f.length < 7) continue;
                Expense e = new Expense(decode(f[0]), decode(f[1]), Double.parseDouble(decode(f[2])),
                        decode(f[3]), Double.parseDouble(decode(f[4])), decode(f[5]), decode(f[6]));
                result.add(e);
            }
        } catch (Exception e) {
            System.err.println("Erreur de lecture des dépenses : " + e.getMessage());
        }
        return result;
    }

    private static void saveExpenses() throws IOException {
        Files.createDirectories(DATA_FILE.getParent());
        Path temp = DATA_FILE.resolveSibling("expenses.db.tmp");
        Files.write(temp, EXPENSES.stream().map(Expense::toStorageLine).toList(), StandardCharsets.UTF_8);
        try {
            Files.move(temp, DATA_FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            Files.move(temp, DATA_FILE, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String decode(String val) {
        return new String(Base64.getUrlDecoder().decode(val), StandardCharsets.UTF_8);
    }
}