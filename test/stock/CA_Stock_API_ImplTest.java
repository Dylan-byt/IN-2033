/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package stock;

/**
 *
 * @author laraashour
 */
import database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class CA_Stock_API_ImplTest {

    private Connection conn;
    private CA_Stock_API_Impl stockApi;
    private final int testProductId = 9999;

    @Before
    public void setUp() throws Exception {
        conn = DBConnection.getConnection();
        stockApi = new CA_Stock_API_Impl(conn);

        // Make sure the product exists first
        PreparedStatement psProduct = conn.prepareStatement(
            "INSERT OR IGNORE INTO ca_products " +
            "(product_id, product_name, price, vat_rate, product_type, package_type, product_units, units_per_pack) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
        );
        psProduct.setInt(1, testProductId);
        psProduct.setString(2, "Test Product");
        psProduct.setDouble(3, 10.00);
        psProduct.setDouble(4, 0.20);
        psProduct.setString(5, "Tablet");
        psProduct.setString(6, "Box");
        psProduct.setString(7, "mg");
        psProduct.setInt(8, 20);
        psProduct.executeUpdate();

        // Clean any old stock row for this product before each test
        PreparedStatement psDeleteStock = conn.prepareStatement(
            "DELETE FROM ca_stock WHERE product_id = ?"
        );
        psDeleteStock.setInt(1, testProductId);
        psDeleteStock.executeUpdate();
    }

    @After
    public void tearDown() throws Exception {
        if (conn != null) {
            PreparedStatement ps1 = conn.prepareStatement("DELETE FROM ca_stock WHERE product_id = ?");
            ps1.setInt(1, testProductId);
            ps1.executeUpdate();

            PreparedStatement ps2 = conn.prepareStatement("DELETE FROM ca_products WHERE product_id = ?");
            ps2.setInt(1, testProductId);
            ps2.executeUpdate();

            conn.close();
        }
    }

    @Test
    public void testAddStock() {
        boolean result = stockApi.addStock(testProductId, 50, 10);
        assertTrue("Stock should be added successfully", result);

        int stock = stockApi.getStockLevel(testProductId);
        assertEquals("Stock level should be 50", 50, stock);
    }

    @Test
    public void testUpdateStockQuantityValid() {
        stockApi.addStock(testProductId, 20, 5);

        boolean updated = stockApi.updateStockQuantity(testProductId, 30);
        assertTrue("Stock update should succeed", updated);

        assertEquals("Stock should be updated to 30", 30, stockApi.getStockLevel(testProductId));
    }

    @Test
    public void testUpdateStockQuantityNegative() {
        stockApi.addStock(testProductId, 20, 5);

        boolean updated = stockApi.updateStockQuantity(testProductId, -10);
        assertFalse("Negative stock update should fail", updated);
    }

    @Test
    public void testRemoveStock() {
        stockApi.addStock(testProductId, 40, 10);

        boolean removed = stockApi.removeStock(testProductId);
        assertTrue("Stock should be removed", removed);

        assertEquals("Stock level should be 0 after removal", 0, stockApi.getStockLevel(testProductId));
    }

    @Test
    public void testRecordDeliveryIncreasesStock() {
        stockApi.addStock(testProductId, 10, 5);

        boolean result = stockApi.recordDelivery(testProductId, 15);
        assertTrue("Delivery should be recorded", result);

        assertEquals("Stock should increase to 25", 25, stockApi.getStockLevel(testProductId));
    }
}