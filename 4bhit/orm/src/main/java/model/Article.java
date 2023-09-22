package model;

import java.io.Serializable;

/**
 * @author Andreas Suender
 * @version 11-07-2022
 */
public class Article implements Serializable {
    private int id;
    private String description;
    private long price;
    private long amount;

    public Article() { }

    public Article(String description, long price, long amount) {
        this.description = description;
        this.price = price;
        this.amount = amount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }
}
