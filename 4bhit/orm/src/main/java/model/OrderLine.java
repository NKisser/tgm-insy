package model;

import java.io.Serializable;

/**
 * @author Andreas Suender
 * @version 11-07-2022
 */
public class OrderLine implements Serializable {
    private int id;
    private Article article;
    private Order order;
    private long amount;

    public OrderLine() {
    }

    public OrderLine(Article article, Order order, long amount) {
        this.article = article;
        this.order = order;
        this.amount = amount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Article getArticle() {
        return article;
    }

    public void setArticle(Article article) {
        this.article = article;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }
}
