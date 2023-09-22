
import java.io.*;
import java.net.InetSocketAddress;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import model.Article;
import model.Client;
import model.Order;
import model.OrderLine;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.json.*;

import org.hibernate.*;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * INSY Webshop Server
 */
public class Server {
    private SessionFactory sf = null;

    /**
     * Port to bind to for HTTP service
     */
    private int port = 8000;

    private static <E> List<E> retrieveAllData(Class<E> type, Session ses) {
        CriteriaBuilder cb = ses.getCriteriaBuilder();
        CriteriaQuery<E> cq = cb.createQuery(type);
        Root<E> rootEntry = cq.from(type);
        CriteriaQuery<E> all = cq.select(rootEntry);

        TypedQuery<E> allQuery = ses.createQuery(all);
        return allQuery.getResultList();
    }

    /**
     * Connect to the database
     * @throws IOException
     */
    Session setupDB() {
        // Create SessionFactory on the first call of this method ONLY, return Session on EACH call.

        final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .configure("hibernate.cfg.xml")
                .build();

        if (this.sf == null) {
            try {
                this.sf = new MetadataSources(registry)
                        .buildMetadata().buildSessionFactory();
            } catch (Exception e) {
                System.err.println(e.getMessage());
                StandardServiceRegistryBuilder.destroy(registry);
            }
        }

        return this.sf.openSession();
    }

    /**
     * Startup the Webserver
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


    public static void main(String[] args) throws Throwable {
        Server webshop = new Server();
        webshop.start();
        System.out.println("Webshop running at http://127.0.0.1:" + webshop.port);
    }


    /**
     * Handler for listing all articles
     */
    class ArticlesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            Session ses = setupDB();
            JSONArray res = new JSONArray(retrieveAllData(Article.class, ses));

            t.getResponseHeaders().set("Content-Type", "application/json");
            answerRequest(t,res.toString());
        }
    }

    /**
     * Handler for listing all clients
     */
    class ClientsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            Session ses = setupDB();

            String response;
            try {
                List<Client> data = retrieveAllData(Client.class, ses);
                data.forEach((Client c) -> {
                    c.getOrders().forEach((Order o) -> {
                        o.setOrderLines(null);
                        o.setClient(null);
                    });
                });

                response = new JSONArray(data).toString();
            } catch (Exception e) {
               response = String.format("{\"error\":\"%s\"}", e.getMessage());
            }

            t.getResponseHeaders().set("Content-Type", "application/json");
            answerRequest(t, response);
        }
    }


    /**
     * Handler for listing all orders
     */
    class OrdersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            Session ses = setupDB();

            String response;
            try {
                List<Order> data = retrieveAllData(Order.class, ses);
                data.forEach((Order c) -> {
                    c.setClient(null);
                    c.setOrderLines(null);
                });

                response = new JSONArray(data).toString();
            } catch (Exception e) {
                response = String.format("{\"error\":\"%s\"}", e.getMessage());
            }

            // TODO Join orders with clients, order lines, and articles
            // TODO Get the order id, client name, number of lines, and total prize of each order and add them to res

            t.getResponseHeaders().set("Content-Type", "application/json");
            answerRequest(t, response);
        }
    }

   
    /**
     * Handler class to place an order
     */
    class PlaceOrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            Session ses = setupDB();
            Transaction tx = ses.getTransaction();
            tx.begin();

            Map <String,String> params  = queryToMap(t.getRequestURI().getQuery());

            int client_id = Integer.parseInt(params.get("client_id"));

            String response;
            int order_id = 1;
            Client client;

            try {
                client = ses.get(Client.class, client_id);

                CriteriaBuilder cb = ses.getCriteriaBuilder();
                CriteriaQuery<Order> cq = cb.createQuery(Order.class);
                Root<Order> rootEntry = cq.from(Order.class);
                cq.select(rootEntry);
                cq.orderBy(cb.desc(rootEntry.get("id")));
                TypedQuery<Order> allQuery = ses.createQuery(cq);
                allQuery.setMaxResults(1);
                order_id = allQuery.getSingleResult().getId() + 1;

                // Create a new order with this id for client client_id

                Order order = new Order();
                order.setId(order_id);
                order.setClient(client);
                order.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
                order.setOrderLines(new HashSet<>());

                ses.save(order);

                for (int i = 1; i <= (params.size()-1) / 2; ++i ){
                    int article_id = Integer.parseInt(params.get("article_id_"+i));
                    int amount = Integer.parseInt(params.get("amount_"+i));

                    Article article = ses.get(Article.class, article_id);
                    long available = article.getAmount();

                    if (available < amount)
                        throw new IllegalArgumentException(String.format("Not enough items of article #%d available", article_id));

                    article.setAmount(available - amount);
                    ses.saveOrUpdate(article);

                    OrderLine orderLine = new OrderLine();
                    orderLine.setOrder(order);
                    orderLine.setAmount(amount);
                    orderLine.setArticle(article);

                    ses.save(orderLine);
                }

                if (tx.getStatus().equals(TransactionStatus.ACTIVE)) {
                    tx.commit();
                }

                response = String.format("{\"order_id\": %d}", order_id);
            } catch (Exception iae) {
                response = String.format("{\"error\":\"%s\"}", iae.getMessage());
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
                    "<h2>Verf&uuml;gbare Endpoints:</h2><dl>"+
                    "<dt>Alle Artikel anzeigen:</dt><dd><a href=\"http://127.0.0.1:"+port+"/articles\">http://127.0.0.1:"+port+"/articles</a></dd>"+
                    "<dt>Alle Bestellungen anzeigen:</dt><dd><a href=\"http://127.0.0.1:"+port+"/orders\">http://127.0.0.1:"+port+"/orders</a></dd>"+
                    "<dt>Alle Kunden anzeigen:</dt><dd><a href=\"http://127.0.0.1:"+port+"/clients\">http://127.0.0.1:"+port+"/clients</a></dd>"+
                    "<dt>Bestellung abschicken:</dt><dd><a href=\"http://127.0.0.1:" + port + "/placeOrder?client_id=1&article_id_1=2&amount_1=1&article_id_2=3&amount_2=1\">http://127.0.0.1:" + port + "/placeOrder?client_id=1&article_id_1=1&amount_1=1&article_id_2=3&amount_2=1</a></dd>" +
                    "</dl></body></html>";

            answerRequest(t, response);
        }

    }


    /**
     * Helper function to send an answer given as a String back to the browser
     * @param t HttpExchange of the request
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
     * Helper method to parse query paramaters
     * @param query
     * @return
     */
    public static Map<String, String> queryToMap(String query){
        Map<String, String> result = new HashMap<String, String>();
        for (String param : query.split("&")) {
            String pair[] = param.split("=");
            if (pair.length>1) {
                result.put(pair[0], pair[1]);
            }else{
                result.put(pair[0], "");
            }
        }
        return result;
    }
}
