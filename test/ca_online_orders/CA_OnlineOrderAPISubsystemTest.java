/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca_online_orders;

/**
 *
 * @author laraashour
 */
import database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import sa_orders.SA_ORD_API;
import stock.CA_Stock_API_Impl;
import merchant.SA_Merchant_API_Impl;

import static org.junit.Assert.*;

public class CA_OnlineOrderAPISubsystemTest {

    private Connection conn;
    private SA_ORD_API ordApi;
    private CA_Stock_API_Impl stockApi;
    private SA_Merchant_API_Impl merchantApi;
    private CA_OnlineOrderAPI_Impl api;

    private final String testOrderId = "ONL-SUB-001";

    @Before
    public void setUp() throws Exception {
        conn = DBConnection.getConnection();
        assertNotNull(conn);

        ordApi = new SA_ORD_API(conn);
        stockApi = new CA_Stock_API_Impl(conn);
        merchantApi = new SA_Merchant_API_Impl(conn);
        api = new CA_OnlineOrderAPI_Impl(ordApi, merchantApi, stockApi, conn);

        // Clean old test data
        try (PreparedStatement ps1 = conn.prepareStatement(
                "DELETE FROM ca_online_order_items WHERE online_order_id = ?")) {
            ps1.setString(1, testOrderId);
            ps1.executeUpdate();
        }

        try (PreparedStatement ps2 = conn.prepareStatement(
                "DELETE FROM ca_online_orders WHERE online_order_id = ?")) {
            ps2.setString(1, testOrderId);
            ps2.executeUpdate();
        }

        // Ensure order row exists before processing
        try (PreparedStatement ps3 = conn.prepareStatement(
                "INSERT INTO ca_online_orders (online_order_id, processed) VALUES (?, FALSE)")) {
            ps3.setString(1, testOrderId);
            ps3.executeUpdate();
        }

        // Ensure stock exists for products used in test
        ensureStockRowExists(1, 50, 10);
        ensureStockRowExists(2, 50, 10);
    }

    private void ensureStockRowExists(int productId, int quantity, int threshold) throws Exception {
        try (PreparedStatement check = conn.prepareStatement(
                "SELECT 1 FROM ca_stock WHERE product_id = ?")) {
            check.setInt(1, productId);
            try (ResultSet rs = check.executeQuery()) {
                if (!rs.next()) {
                    try (PreparedStatement insert = conn.prepareStatement(
                            "INSERT INTO ca_stock (product_id, quantity, low_stock_threshold) VALUES (?, ?, ?)")) {
                        insert.setInt(1, productId);
                        insert.setInt(2, quantity);
                        insert.setInt(3, threshold);
                        insert.executeUpdate();
                    }
                } else {
                    try (PreparedStatement update = conn.prepareStatement(
                            "UPDATE ca_stock SET quantity = ?, low_stock_threshold = ? WHERE product_id = ?")) {
                        update.setInt(1, quantity);
                        update.setInt(2, threshold);
                        update.setInt(3, productId);
                        update.executeUpdate();
                    }
                }
            }
        }
    }

    @After
    public void tearDown() throws Exception {
        if (conn != null && !conn.isClosed()) {
            try (PreparedStatement ps1 = conn.prepareStatement(
                    "DELETE FROM ca_online_order_items WHERE online_order_id = ?")) {
                ps1.setString(1, testOrderId);
                ps1.executeUpdate();
            }

            try (PreparedStatement ps2 = conn.prepareStatement(
                    "DELETE FROM ca_online_orders WHERE online_order_id = ?")) {
                ps2.setString(1, testOrderId);
                ps2.executeUpdate();
            }

            conn.close();
        }
    }

    @Test
    public void testProcessOnlineOrderMarksOrderProcessed() throws Exception {
        api.processOnlineOrder(testOrderId, "1:2,2:1");

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT processed FROM ca_online_orders WHERE online_order_id = ?")) {
            ps.setString(1, testOrderId);

            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertTrue(rs.getBoolean("processed"));
            }
        }
    }

    @Test
    public void testGetOrderStatusReturnsProcessed() throws Exception {
        api.processOnlineOrder(testOrderId, "1:2,2:1");

        String status = api.getOrderStatus(testOrderId);

        assertEquals("PROCESSED", status);
    }
}