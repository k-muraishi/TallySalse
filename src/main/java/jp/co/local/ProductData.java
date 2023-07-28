package jp.co.local;

import java.util.Date;

public class ProductData {
    private int id;
    private String productName;
    private Date salesDate;
    private int quantity;
    private double sales;
    private String store;

    public ProductData(int id, String productName, Date salesDate, int quantity, double sales, String store) {
        this.id = id;
        this.productName = productName;
        this.salesDate = salesDate;
        this.quantity = quantity;
        this.sales = sales;
        this.store = store;
    }

    public int getId() {
        return id;
    }

    public String getProductName() {
        return productName;
    }

    public Date getSalesDate() {
        return salesDate;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getSales() {
        return sales;
    }

    public String getStore() {
        return store;
    }
}
