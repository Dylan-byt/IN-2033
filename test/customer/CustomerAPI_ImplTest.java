/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package customer;

/**
 *
 * @author laraashour
 */
import database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class CustomerAPI_ImplTest {

    private CustomerAPI_Impl api;

    @Before
    public void setUp() throws Exception {
        api = new CustomerAPI_Impl();
        clearTestData();
    }

    @After
    public void tearDown() throws Exception {
        clearTestData();
    }

    private void clearTestData() throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            assertNotNull("Database connection should not be null", conn);

            // Delete discount rows first if they depend on customers
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM ca_customer_discounts WHERE customer_id >= 900")) {
                ps.executeUpdate();
            } catch (Exception ignored) {
                // Ignore if table is missing in this schema version
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM ca_payment_reminders WHERE customer_id >= 900")) {
                ps.executeUpdate();
            } catch (Exception ignored) {
                // Ignore if table is missing in this schema version
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM ca_customers WHERE customer_id >= 900")) {
                ps.executeUpdate();
            }
        }
    }

    private void insertCustomer(int customerId,
                                String firstName,
                                String surname,
                                String email,
                                double creditLimit,
                                double outstandingBalance,
                                String accountStatus,
                                int accountHolder) throws Exception {

        String sql = "INSERT INTO ca_customers " +
                     "(customer_id, firstname, surname, dob, email, phone, houseNumber, postcode, " +
                     "credit_limit, outstanding_balance, account_status, account_holder) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            assertNotNull("Database connection should not be null", conn);

            ps.setInt(1, customerId);
            ps.setString(2, firstName);
            ps.setString(3, surname);
            ps.setString(4, "2000-01-01");
            ps.setString(5, email);
            ps.setString(6, "07123456789");
            ps.setInt(7, 1);
            ps.setString(8, "E1 1AA");
            ps.setDouble(9, creditLimit);
            ps.setDouble(10, outstandingBalance);
            ps.setString(11, accountStatus);
            ps.setInt(12, accountHolder);

            ps.executeUpdate();
        }
    }

    private String getCustomerStatus(int customerId) throws Exception {
        String sql = "SELECT account_status FROM ca_customers WHERE customer_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("account_status");
                }
            }
        }

        return null;
    }

    private boolean discountPlanExists(int customerId) throws Exception {
        String sql = "SELECT 1 FROM ca_customer_discounts WHERE customer_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private String getDiscountPlanRow(int customerId) throws Exception {
        String sql = "SELECT plan_type, discount_value FROM ca_customer_discounts WHERE customer_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("plan_type") + " - " + rs.getDouble("discount_value");
                }
            }
        }

        return null;
    }

    private int countRemindersForCustomer(int customerId) throws Exception {
        String sql = "SELECT COUNT(*) AS total FROM ca_payment_reminders WHERE customer_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }

        return 0;
    }

    @Test
    public void testAddCustomerValid() throws Exception {
        boolean result = api.addCustomer(
                "Lara",
                "Ashour",
                "2000-01-01",
                "lara_test@example.com",
                "07111111111",
                10,
                "E1 1AA",
                250.0
        );

        assertTrue("Customer should be added successfully", result);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM ca_customers WHERE email = ?")) {

            ps.setString(1, "lara_test@example.com");

            try (ResultSet rs = ps.executeQuery()) {
                assertTrue("Inserted customer should exist in database", rs.next());
                assertEquals("Lara", rs.getString("firstname"));
                assertEquals("Ashour", rs.getString("surname"));
            }
        }
    }

    @Test
    public void testUpdateAccountStatusesSetsSuspended() throws Exception {
        insertCustomer(
                901,
                "John",
                "Suspended",
                "john901@test.com",
                500.0,
                100.0,
                "NORMAL",
                1
        );

        api.updateAccountStatuses();

        String status = getCustomerStatus(901);
        assertEquals("Customer should become SUSPENDED", "SUSPENDED", status);
    }

    @Test
    public void testUpdateAccountStatusesSetsInDefault() throws Exception {
        insertCustomer(
                902,
                "Amy",
                "Default",
                "amy902@test.com",
                200.0,
                350.0,
                "NORMAL",
                1
        );

        api.updateAccountStatuses();

        String status = getCustomerStatus(902);
        assertEquals("Customer should become IN_DEFAULT", "IN_DEFAULT", status);
    }

    @Test
    public void testModifyDiscountPlanUpdatesExistingPlan() throws Exception {
        insertCustomer(
                903,
                "Mia",
                "Discount",
                "mia903@test.com",
                300.0,
                0.0,
                "NORMAL",
                1
        );

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO ca_customer_discounts (customer_id, plan_type, discount_value) VALUES (?, ?, ?)")) {
            ps.setInt(1, 903);
            ps.setString(2, "STANDARD");
            ps.setDouble(3, 5.0);
            ps.executeUpdate();
        }

        boolean updated = api.modifyDiscountPlan("ACC903", "PREMIUM", 12.5);

        assertTrue("Discount plan should be updated", updated);

        String plan = getDiscountPlanRow(903);
        assertEquals("PREMIUM - 12.5", plan);
    }

    @Test
    public void testGenerateRemindersCreatesReminderForSuspendedCustomer() throws Exception {
        insertCustomer(
                904,
                "Noah",
                "Reminder",
                "noah904@test.com",
                500.0,
                120.0,
                "SUSPENDED",
                1
        );

        int before = countRemindersForCustomer(904);
        int generated = api.generateReminders();
        int after = countRemindersForCustomer(904);

        assertEquals("Customer should have no reminders before test", 0, before);
        assertTrue("At least one reminder should be generated", generated >= 1);
        assertEquals("One reminder should be created for suspended customer", 1, after);
    }
}