package asuender.insy.a02;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * INSY Webshop Server
 */
public class Server {

    /**
     * Port to bind to for HTTP service
     */
    private final int port = 8000;

    public static void main(String[] args) throws Throwable {
        Server webshop = new Server();
        webshop.start();
        System.out.println("Webshop running at http://127.0.0.1:" + webshop.port);
    }

    /**
     * Helper method to parse query paramaters
     *
     * @param query
     * @return
     */
    public static Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<String, String>();
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1) {
                result.put(pair[0], pair[1]);
            } else {
                result.put(pair[0], "");
            }
        }
        return result;
    }

    /**
     * Connect to the database
     *
     * @throws IOException
     */
    Connection setupDB() {
        String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        Properties dbProps = new Properties();
        try {
            dbProps.load(new FileInputStream("./db.properties"));

            return DriverManager.getConnection(dbProps.getProperty("url"), dbProps.getProperty("user"), dbProps.getProperty("password"));
        } catch (Exception throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

    /**
     * Startup the Webserver
     *
     * @throws IOException
     */
    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/articles", new ArticlesHandler());
        server.createContext("/clients", new ClientsHandler());
        server.createContext("/placeOrder", new PlaceOrderHandler());
        server.createContext("/orders", new OrdersHandler());
        server.createContext("/", new IndexHandler());

        server.start();
    }

    /**
     * Helper function to send an answer given as a String back to the browser
     *
     * @param t        HttpExchange of the request
     * @param response Answer to send
     * @throws IOException
     */
    private void answerRequest(HttpExchange t, String response) throws IOException {
        byte[] payload = response.getBytes();
        t.sendResponseHeaders(200, payload.length);
        OutputStream os = t.getResponseBody();
        os.write(payload);
        os.close();
    }

    /**
     * Handler for listing all articles
     */
    class ArticlesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            Connection conn = setupDB();

            JSONArray res = new JSONArray();

            try {
                Statement statement = conn.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT * FROM articles");

                while (resultSet.next()) {
                    JSONObject art = new JSONObject();
                    art.put("id", resultSet.getInt(1));
                    art.put("description", resultSet.getString(2));
                    art.put("price", resultSet.getLong(3));
                    art.put("amount", resultSet.getInt(4));
                    res.put(art);
                }
            } catch (SQLException sqle) {
                sqle.printStackTrace();
            }

            /*JSONObject art1 = new JSONObject();
            art1.put("id", 1);
            art1.put("description", "Bleistift");
            art1.put("price", 0.70);
            art1.put("amount", 2);
            res.put(art1);
            JSONObject art2 = new JSONObject();
            art2.put("id", 2);
            art2.put("description", "Papier");
            art2.put("price", 2);
            art2.put("amount", 100);
            res.put(art2);*/

            t.getResponseHeaders().set("Content-Type", "application/json");
            answerRequest(t, res.toString());
        }

    }

    /**
     * Handler for listing all clients
     */
    class ClientsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            Connection conn = setupDB();

            JSONArray res = new JSONArray();

            try {
                Statement statement = conn.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT * FROM clients");

                while (resultSet.next()) {
                    JSONObject cli = new JSONObject();
                    cli.put("id", resultSet.getInt(1));
                    cli.put("name", resultSet.getString(2));
                    cli.put("address", resultSet.getString(3));
                    res.put(cli);
                }
            } catch (SQLException sqle) {
                sqle.printStackTrace();
            }

            /*JSONObject cli = new JSONObject();
            cli.put("id", 1);
            cli.put("name", "Brein");
            cli.put("address", "TGM, 1220 Wien");
            res.put(cli);*/

            t.getResponseHeaders().set("Content-Type", "application/json");
            answerRequest(t, res.toString());
        }
    }

    /**
     * Handler for listing all orders
     */
    class OrdersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            Connection conn = setupDB();

            JSONArray res = new JSONArray();

            try {
                Statement statement = conn.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT * FROM orders");

                JSONObject ord;

                while (resultSet.next()) {
                    ord = new JSONObject();
                    ord.put("id", resultSet.getInt(1));
                    ord.put("created_at", resultSet.getTimestamp(2));
                    ord.put("client_id", resultSet.getInt(3));
                    res.put(ord);
                }

                resultSet = statement.executeQuery("select * from orders\n" +
                        "inner join clients c on c.id = orders.client_id\n" +
                        "inner join order_lines ol on orders.id = ol.order_id\n" +
                        "inner join articles a on a.id = ol.article_id;");

                while (resultSet.next()) {
                    ord = new JSONObject();
                    ord.put("id", resultSet.getInt("orders.id"));
                    ord.put("client", resultSet.getString("name"));
                    ord.put("lines", resultSet.getInt("ol.amount"));
                    ord.put("price", resultSet.getInt("price"));
                    res.put(ord);
                }
            } catch (SQLException sqle) {
                sqle.printStackTrace();
            }

            /*JSONObject ord = new JSONObject();
            ord.put("id", 1);
            ord.put("client", "Brein");
            ord.put("lines", 2);
            ord.put("price", 3.5);
            res.put(ord);*/

            t.getResponseHeaders().set("Content-Type", "application/json");
            answerRequest(t, res.toString());
        }
    }

    /**
     * Handler class to place an order
     */
    class PlaceOrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            Connection conn = setupDB();
            Map<String, String> params = queryToMap(t.getRequestURI().getQuery());

            int client_id = Integer.parseInt(params.get("client_id"));

            ResultSet resultSet;
            String response = "";
            int order_id = 1;
            try {
                Statement statement = conn.createStatement();

                resultSet = statement.executeQuery("select max(id) as id from orders");
                if (resultSet.next()) {
                    order_id = resultSet.getInt("id") + 1;
                } else {
                    throw new SQLException("error fetching next free order id");
                }

                statement.execute(String.format("insert into orders (id, client_id) values (%d, %d)", order_id, client_id));

                for (int i = 1; i <= (params.size() - 1) / 2; ++i) {
                    int article_id = Integer.parseInt(params.get("article_id_" + i));
                    int amount = Integer.parseInt(params.get("amount_" + i));

                    resultSet = statement.executeQuery(String.format("select amount from articles where id = %d", article_id));

                    int available = -1;
                    if (resultSet.next()) {
                        available = resultSet.getInt("amount");
                    }

                    if (available < amount)
                        throw new IllegalArgumentException(String.format("Not enough items of article #%d available", article_id));

                    statement.execute(String.format("update articles set amount = amount - %d where id = %d", amount, article_id));

                    statement.execute(String.format(
                            "insert into order_lines(article_id, order_id, amount) values (%d, %d, 1)", article_id, order_id, i));
                }

                response = String.format("{\"order_id\": %d}", order_id);
            } catch (IllegalArgumentException iae) {
                response = String.format("{\"error\":\"%s\"}", iae.getMessage());
            } catch (SQLException sqle) {
                sqle.printStackTrace();
            }

            t.getResponseHeaders().set("Content-Type", "application/json");
            answerRequest(t, response);
        }
    }

    /**
     * Handler for listing static index page
     */
    class IndexHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "<!doctype html>\n" +
                    "<html><head><title>INSY Webshop</title><link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/water.css@2/out/water.css\"></head>" +
                    "<body><h1>INSY Pseudo-Webshop</h1>" +
                    "<h2>Verf&uuml;gbare Endpoints:</h2><dl>" +
                    "<dt>Alle Artikel anzeigen:</dt><dd><a href=\"http://127.0.0.1:" + port + "/articles\">http://127.0.0.1:" + port + "/articles</a></dd>" +
                    "<dt>Alle Bestellungen anzeigen:</dt><dd><a href=\"http://127.0.0.1:" + port + "/orders\">http://127.0.0.1:" + port + "/orders</a></dd>" +
                    "<dt>Alle Kunden anzeigen:</dt><dd><a href=\"http://127.0.0.1:" + port + "/clients\">http://127.0.0.1:" + port + "/clients</a></dd>" +
                    "<dt>Bestellung abschicken:</dt><dd><a href=\"http://127.0.0.1:" + port + "/placeOrder?client_id=1&article_id_1=2&amount_1=1&article_id_2=3&amount_2=1\">http://127.0.0.1:" + port + "/placeOrder?client_id=1&article_id_1=1&amount_1=1&article_id_2=3&amount_2=1</a></dd>" +
                    "</dl></body></html>";

            answerRequest(t, response);
        }

    }


}
