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
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import merchant.SA_Merchant_API;
import sa_orders.SA_ORD_API;
import stock.CA_Stock_API_Impl;

public class CA_OnlineOrderAPI_ImplTest extends TestCase {

    private Connection conn;
    private SA_ORD_API ordApi;
    private CA_Stock_API_Impl stockApi;
    private SA_Merchant_API merchantApiStub;
    private CA_OnlineOrderAPI_Impl onlineOrderApi;

    private final int testProductId = 9911;
    private final String testProductName = "Paracetmol";
    private final double testPrice = 9.99;

    @Override
    protected void setUp() throws Exception {
        conn = DBConnection.getConnection();
        assertNotNull("Database connection should not be null", conn);

        ordApi = new SA_ORD_API(conn);
        stockApi = new CA_Stock_API_Impl(conn);

        merchantApiStub = new SA_Merchant_API() {
            @Override
            public double getCustomerBalance(int customerID) {
                return 0;
            }

            @Override
            public boolean setCreditLimit(int customerID, double newLimit) {
                return false;
            }

            @Override
            public boolean updateAccountStatus(int customerID, String status) {
                return false;
            }

            @Override
            public boolean processCreditPayment(int customerID, double amount) {
                return true;
            }

            @Override
            public boolean processCardPayment(String orderID, String cardNumber, String expiry, double amount) {
                return true;
            }

            @Override
            public boolean processCashPayment(String orderID, double amount) {
                return true;
            }

            @Override
            public boolean autoSuspendAccount(int customerID) {
                return false;
            }

            @Override
            public boolean autoMoveToDefault(int customerID) {
                return false;
            }

            @Override
            public boolean autoRestoreAccount(int customerID) {
                return false;
            }

            @Override
            public boolean managerReactivateAccount(int customerID) {
                return false;
            }

            @Override
            public boolean recordAccountPayment(int customerID, double amount) {
                return false;
            }

            @Override
            public boolean recordCustomerPurchase(int customerID, List<Object[]> saleItems, double totalAmount, String paymentMethod) {
                return false;
            }

            @Override
            public List<String> generateStatements() {
                return new ArrayList<>();
            }

            @Override
            public List<String> generateReminders() {
                return new ArrayList<>();
            }

            @Override
            public void checkAndAutoUpdateAllAccounts() {
            }

            @Override
            public double getTotalSales() {
                return 0;
            }

            @Override
            public int getTransactionCount() {
                return 0;
            }

            @Override
            public int getOrdersPlacedCount() {
                return 0;
            }

            @Override
            public List<Object[]> getTopSellingProducts() {
                return new ArrayList<>();
            }

            @Override
            public List<Object[]> getSalesReportRows() {
                return new ArrayList<>();
            }
        };

        onlineOrderApi = new CA_OnlineOrderAPI_Impl(ordApi, merchantApiStub, stockApi, conn);

        cleanupTestData();

        PreparedStatement psProduct = conn.prepareStatement(
            "INSERT INTO ca_products (product_id, product_name, price, vat_rate, product_type) VALUES (?, ?, ?, ?, ?)"
        );
        psProduct.setInt(1, testProductId);
        psProduct.setString(2, testProductName);
        psProduct.setDouble(3, testPrice);
        psProduct.setDouble(4, 0.20);
        psProduct.setString(5, "TEST");
        psProduct.executeUpdate();

        PreparedStatement psStock = conn.prepareStatement(
            "INSERT INTO ca_stock (product_id, quantity, low_stock_threshold) VALUES (?, ?, ?)"
        );
        psStock.setInt(1, testProductId);
        psStock.setInt(2, 25);
        psStock.setInt(3, 5);
        psStock.executeUpdate();
    }

    @Override
    protected void tearDown() throws Exception {
        cleanupTestData();
    }

    private void cleanupTestData() throws Exception {
        if (conn == null) {
            return;
        }

        PreparedStatement ps1 = conn.prepareStatement(
            "DELETE FROM ca_online_order_items WHERE product_id = ?"
        );
        ps1.setInt(1, testProductId);
        ps1.executeUpdate();

        PreparedStatement ps2 = conn.prepareStatement(
            "DELETE FROM ca_online_orders WHERE online_order_id LIKE 'ONL-%'"
        );
        ps2.executeUpdate();

        PreparedStatement ps3 = conn.prepareStatement(
            "DELETE FROM ca_stock WHERE product_id = ?"
        );
        ps3.setInt(1, testProductId);
        ps3.executeUpdate();

        PreparedStatement ps4 = conn.prepareStatement(
            "DELETE FROM ca_products WHERE product_id = ?"
        );
        ps4.setInt(1, testProductId);
        ps4.executeUpdate();
    }

    public void testCheckProductStockReturnsCorrectQuantity() {
        int actual = onlineOrderApi.checkProductStock(String.valueOf(testProductId));
        assertEquals(25, actual);
    }

    public void testGetMerchantCatalogueReturnsInsertedProduct() {
        String[] results = onlineOrderApi.getMerchantCatalogue("para");

        boolean found = false;
        for (String item : results) {
            if (item.contains(String.valueOf(testProductId)) && item.contains(testProductName)) {
                found = true;
                break;
            }
        }

        assertTrue(found);
    }

    public void testCreateOrderReturnsOrderIdAndCreatesOrder() throws Exception {
        String orderId = onlineOrderApi.createOrder();

        assertNotNull(orderId);
        assertFalse(orderId.trim().isEmpty());

        String status = onlineOrderApi.getOrderStatus(orderId);
        assertEquals("CREATED", status);
    }

    public void testGenerateReceiptReturnsMessageWhenOrderNotProcessed() throws Exception {
        String orderId = onlineOrderApi.createOrder();

        String receipt = onlineOrderApi.generateReceipt(orderId);

        assertEquals("Cannot generate receipt: order not processed.", receipt);
    }

    public void testGetOrderStatusReturnsProcessedAfterProcessingOrder() throws Exception {
        String orderId = onlineOrderApi.createOrder();

        onlineOrderApi.processOnlineOrder(orderId, testProductId + ":2");

        String status = onlineOrderApi.getOrderStatus(orderId);

        assertEquals("PROCESSED", status);
    }
}