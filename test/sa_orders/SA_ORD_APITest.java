/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sa_orders;

/**
 *
 * @author laraashour
 */
import database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class SA_ORD_APITest {

    private Connection conn;
    private SA_ORD_API ordApi;
    private String testOrderId;
    private int validProductId;

    @Before
    public void setUp() throws Exception {
        conn = DBConnection.getConnection();
        assertNotNull("Database connection should not be null", conn);

        ordApi = new SA_ORD_API(conn);
        validProductId = getAnyExistingProductId();
        assertTrue("A valid product ID should exist in ca_products", validProductId > 0);

        testOrderId = ordApi.newOrder();
        assertNotNull("newOrder() should return an order ID", testOrderId);
        assertFalse("Order ID should not be blank", testOrderId.trim().isEmpty());
    }

    @After
    public void tearDown() throws Exception {
        if (conn != null) {
            if (testOrderId != null) {
                try (PreparedStatement ps1 =
                             conn.prepareStatement("DELETE FROM ca_online_order_items WHERE online_order_id = ?");
                     PreparedStatement ps2 =
                             conn.prepareStatement("DELETE FROM ca_online_orders WHERE online_order_id = ?")) {

                    ps1.setString(1, testOrderId);
                    ps1.executeUpdate();

                    ps2.setString(1, testOrderId);
                    ps2.executeUpdate();
                }
            }
            conn.close();
        }
    }

    @Test
    public void testNewOrderCreatesOrderRecord() throws Exception {
        String sql = "SELECT online_order_id, processed FROM ca_online_orders WHERE online_order_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, testOrderId);

            try (ResultSet rs = ps.executeQuery()) {
                assertTrue("Inserted order should exist in ca_online_orders", rs.next());
                assertEquals("Stored order ID should match created order ID",
                        testOrderId, rs.getString("online_order_id"));
                assertFalse("New order should be unprocessed by default", rs.getBoolean("processed"));
            }
        }
    }

    @Test
    public void testAddItemsStoresOrderItems() throws Exception {
        ordApi.addItems(testOrderId, new int[]{validProductId}, new int[]{3});

        String sql = "SELECT quantity FROM ca_online_order_items WHERE online_order_id = ? AND product_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, testOrderId);
            ps.setInt(2, validProductId);

            try (ResultSet rs = ps.executeQuery()) {
                assertTrue("Order item should be inserted", rs.next());
                assertEquals("Inserted quantity should match", 3, rs.getInt("quantity"));
            }
        }
    }

    @Test
    public void testGetOrderItemsReturnsInsertedItems() throws Exception {
        ordApi.addItems(testOrderId, new int[]{validProductId}, new int[]{2});

        Map<Integer, Integer> items = ordApi.getOrderItems(testOrderId);

        assertNotNull("getOrderItems() should not return null", items);
        assertTrue("Returned map should contain inserted product ID", items.containsKey(validProductId));
        assertEquals("Returned quantity should match inserted quantity",
                Integer.valueOf(2), items.get(validProductId));
    }

    @Test
    public void testViewOrderReturnsReadableProductNameAndQuantity() throws Exception {
        ordApi.addItems(testOrderId, new int[]{validProductId}, new int[]{4});

        Map<String, Integer> view = ordApi.viewOrder(testOrderId);

        assertNotNull("viewOrder() should not return null", view);
        assertFalse("viewOrder() should contain at least one entry after adding items", view.isEmpty());

        String expectedProductName = getProductName(validProductId);
        assertTrue("viewOrder() should contain the product name as a key",
                view.containsKey(expectedProductName));
        assertEquals("viewOrder() should return the correct quantity",
                Integer.valueOf(4), view.get(expectedProductName));
    }

    @Test
    public void testUpdateOrderStatusMarksOrderAsDelivered() throws Exception {
        boolean updated = ordApi.updateOrderStatus(testOrderId, "delivered");
        assertTrue("updateOrderStatus() should return true for an existing order", updated);

        String localStatus = ordApi.getLocalOrderStatus(testOrderId);
        assertEquals("Local order status should become delivered", "delivered", localStatus);
    }

    private int getAnyExistingProductId() throws Exception {
        String sql = "SELECT product_id FROM ca_products LIMIT 1";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("product_id");
            }
        }
        return -1;
    }

    private String getProductName(int productId) throws Exception {
        String sql = "SELECT product_name FROM ca_products WHERE product_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("product_name");
                }
            }
        }
        return null;
    }
}