package stock;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import database.DBConnection;

public class CA_Stock_API_Impl {

    private Connection conn;

    public CA_Stock_API_Impl(Connection conn) {
        this.conn = conn;
    }

    /**
     * ADD NEW STOCK ITEM
     */
    public boolean addStock(int productId, int quantity, int lowerBound) {
        try {
            String sql = "INSERT INTO ca_stock (product_id, quantity, lower_bound) VALUES (?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, productId);
            ps.setInt(2, quantity);
            ps.setInt(3, lowerBound);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * MODIFY STOCK QUANTITY so +10 or -2
     */
    public boolean updateStockQuantity(int productId, int newQuantity) {

        if (newQuantity < 0) {
            System.out.println("Stock cannot be negative");
            return false;
        }

        try {
            String sql = "UPDATE ca_stock SET quantity = ? WHERE product_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, newQuantity);
            ps.setInt(2, productId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
    /**
     * REMOVE STOCK ITEM
     */
    public boolean removeStock(int productId) {
        try {
            String sql = "DELETE FROM ca_stock WHERE product_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, productId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * LIST ITEMS BELOW LOWER BOUND
     */
    public List<String> getLowStockItems() {

        List<String> lowStock = new ArrayList<>();

        try {
            String sql = 
                "SELECT s.product_id, p.product_name, s.quantity, s.lower_bound " +
                "FROM ca_stock s " +
                "JOIN ca_products p ON s.product_id = p.product_id " +
                "WHERE s.quantity < s.lower_bound";

            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String item = String.format(
                    "Product: %s (ID: %d) | Qty: %d | Min: %d",
                    rs.getString("product_name"),
                    rs.getInt("product_id"),
                    rs.getInt("quantity"),
                    rs.getInt("lower_bound")
                );

                lowStock.add(item);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lowStock;
    }

    /**
     * RECORD DELIVERY (increase stock from SA)
     */
    public boolean recordDelivery(int productId, int quantity) {

    // Basic validation
        if (quantity <= 0) {
            System.out.println("Invalid delivery quantity");
            return false;
        }

        try {
            String sql = "UPDATE ca_stock SET quantity = quantity + ? WHERE product_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, quantity);
            ps.setInt(2, productId);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Delivery recorded: +" + quantity + " for product " + productId);
                return true;
            } else {
                System.out.println("Product not found");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * GET CURRENT STOCK LEVEL
     */
    public int getStockLevel(int productId) {
        try {
            String sql = "SELECT quantity FROM ca_stock WHERE product_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, productId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("quantity");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }
}