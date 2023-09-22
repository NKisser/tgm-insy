package model;

import java.io.Serializable;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Set;

/**
 * @author Andreas Suender
 * @version 11-07-2022
 */
public class Order implements Serializable {
    private int id;
    private Timestamp createdAt;
    private Client client;
    private Set<OrderLine> orderLines;

    public Order() {
    }

    public Order(Timestamp createdAt, Client client, Set<OrderLine> orderLines) {
        this.createdAt = createdAt;
        this.client = client;
        this.orderLines = orderLines;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Set<OrderLine> getOrderLines() {
        return orderLines;
    }

    public void setOrderLines(Set<OrderLine> orderLines) {
        this.orderLines = orderLines;
    }
}
