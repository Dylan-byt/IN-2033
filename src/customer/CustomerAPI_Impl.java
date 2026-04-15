/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package customer;

import database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import templates.TemplateAPI;
import templates.TemplateAPI_Impl;

/**
 *
 * @author laraashour
 */
public class CustomerAPI_Impl implements CustomerAPI  {
    
    private int parseCustomerId(String accountId) throws Exception {
        if (accountId == null || !accountId.matches("ACC\\d+")) {
            throw new Exception("Invalid account ID format. Expected ACC followed by numbers.");
        }
        return Integer.parseInt(accountId.substring(3));
    }

    private String formatAccountId(int customerId) {
        return String.format("ACC%03d", customerId);
    }

@Override
public boolean addCustomer(String firstName,
                           String surname,
                           String dob,
                           String email,
                           String phone,
                           int houseNumber,
                           String postcode,
                           double creditLimit) throws Exception {

    String sql = "INSERT INTO ca_customers " +
                 "(firstname, surname, dob, email, phone, houseNumber, postcode, credit_limit, outstanding_balance, account_status, account_holder) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        if (conn == null) {
            throw new Exception("Database connection failed.");
        }

        ps.setString(1, firstName);
        ps.setString(2, surname);
        ps.setString(3, dob);
        ps.setString(4, email);
        ps.setString(5, phone);
        ps.setInt(6, houseNumber);
        ps.setString(7, postcode);
        ps.setDouble(8, creditLimit);
        ps.setDouble(9, 0.0);
        ps.setString(10, "NORMAL");
        ps.setInt(11, 1);

        return ps.executeUpdate() > 0;
    }
}

    @Override
    public List<Customer> getAllCustomers() throws Exception {
        List<Customer> customers = new ArrayList<>();


        
        String sql = "SELECT customer_id, firstname, surname, email, phone, credit_limit, " +
             "outstanding_balance, account_status " +
             "FROM ca_customers";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (conn == null) {
                throw new Exception("Database connection failed.");
            }

            while (rs.next()) {
                int customerId = rs.getInt("customer_id");

                customers.add(new Customer(
                    formatAccountId(customerId),
                    rs.getString("firstname"),
                    rs.getString("surname"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getDouble("credit_limit"),
                    rs.getString("account_status"),
                    rs.getDouble("outstanding_balance")
                ));
            }
        }

        return customers;
    }

    @Override
    public boolean deleteCustomer(String accountId) throws Exception {
        int customerId = parseCustomerId(accountId);

        String sql = "DELETE FROM ca_customers WHERE customer_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (conn == null) {
                throw new Exception("Database connection failed.");
            }

            ps.setInt(1, customerId);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean customerExists(String accountId) throws Exception {
        int customerId = parseCustomerId(accountId);

        String sql = "SELECT 1 FROM ca_customers WHERE customer_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (conn == null) {
                throw new Exception("Database connection failed.");
            }

            ps.setInt(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
        
        
    public void normaliseStatuses() throws Exception {
    String sql1 = "UPDATE ca_customers SET account_status = 'NORMAL' WHERE account_status = 'ACTIVE'";
    String sql2 = "UPDATE ca_customers SET account_status = 'IN_DEFAULT' WHERE account_status = 'CLOSED'";

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps1 = conn.prepareStatement(sql1);
         PreparedStatement ps2 = conn.prepareStatement(sql2)) {

        if (conn == null) {
            throw new Exception("Database connection failed.");
        }

        ps1.executeUpdate();
        ps2.executeUpdate();
   
        }
    }
    
    @Override
public void updateAccountStatuses() throws Exception {
    String suspendSql =
        "UPDATE ca_customers " +
        "SET account_status = 'SUSPENDED' " +
        "WHERE account_holder = 1 " +
        "AND outstanding_balance > 0 " +
        "AND outstanding_balance <= credit_limit";

    String defaultSql =
        "UPDATE ca_customers " +
        "SET account_status = 'IN_DEFAULT' " +
        "WHERE account_holder = 1 " +
        "AND outstanding_balance > credit_limit";

    String normalSql =
        "UPDATE ca_customers " +
        "SET account_status = 'NORMAL' " +
        "WHERE account_holder = 1 " +
        "AND outstanding_balance <= 0";

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement psSuspend = conn.prepareStatement(suspendSql);
         PreparedStatement psDefault = conn.prepareStatement(defaultSql);
         PreparedStatement psNormal = conn.prepareStatement(normalSql)) {

        if (conn == null) {
            throw new Exception("Database connection failed.");
        }

        psSuspend.executeUpdate();
        psDefault.executeUpdate();
        psNormal.executeUpdate();
    }
}
    
    @Override
public boolean setDiscountPlan(String accountId, String planType, double discountValue) throws Exception {
    int customerId = parseCustomerId(accountId);

    String checkSql = "SELECT 1 FROM ca_customer_discounts WHERE customer_id = ?";
    String insertSql = "INSERT INTO ca_customer_discounts (customer_id, plan_type, discount_value) VALUES (?, ?, ?)";

    try (Connection conn = DBConnection.getConnection()) {
        if (conn == null) {
            throw new Exception("Database connection failed.");
        }

        try (PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
            psCheck.setInt(1, customerId);
            try (ResultSet rs = psCheck.executeQuery()) {
                if (rs.next()) {
                    throw new Exception("This customer already has a discount plan. Use modify instead.");
                }
            }
        }

        try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
            psInsert.setInt(1, customerId);
            psInsert.setString(2, planType.toUpperCase());
            psInsert.setDouble(3, discountValue);
            return psInsert.executeUpdate() > 0;
        }
    }
}

    @Override
public boolean modifyDiscountPlan(String accountId, String planType, double discountValue) throws Exception {
    int customerId = parseCustomerId(accountId);

String sql = "UPDATE ca_customer_discounts SET plan_type = ?, discount_value = ? WHERE customer_id = ?";
    
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        if (conn == null) {
            throw new Exception("Database connection failed.");
        }

        ps.setString(1, planType.toUpperCase());
        ps.setDouble(2, discountValue);
        ps.setInt(3, customerId);

        return ps.executeUpdate() > 0;
    }
}

@Override
public boolean deleteDiscountPlan(String accountId) throws Exception {
    int customerId = parseCustomerId(accountId);

    String sql = "DELETE FROM ca_customer_discounts WHERE customer_id = ?";

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        if (conn == null) {
            throw new Exception("Database connection failed.");
        }

        ps.setInt(1, customerId);
        return ps.executeUpdate() > 0;
    }
  }

@Override
public String getDiscountPlan(String accountId) throws Exception {
    int customerId = parseCustomerId(accountId);

    String sql = "SELECT plan_type, discount_value FROM ca_customer_discounts WHERE customer_id = ?";

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        if (conn == null) {
            throw new Exception("Database connection failed.");
        }

        ps.setInt(1, customerId);

        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getString("plan_type") + " - " + rs.getDouble("discount_value");
            }
        }
    }

    return "No discount plan";
}

@Override
public void updateReminderStatuses() throws Exception {
    // Reminder state is tracked only in ca_payment_reminders.
    // This method is retained for compatibility but does not rely on database fields
    // that are not present in the current schema.
}

@Override
public int generateReminders() throws Exception {
    int count = 0;
    TemplateAPI templateAPI = new TemplateAPI_Impl();
    String firstTemplate = templateAPI.getTemplate("reminder_first");
    String secondTemplate = templateAPI.getTemplate("reminder_second");
    String pharmacyName = templateAPI.getTemplate("pharmacy_name");
    String pharmacyAddress = safeTemplateFetch(templateAPI, "pharmacy_address");
    String pharmacyEmail = safeTemplateFetch(templateAPI, "pharmacy_email");
    String pharmacyPhone = safeTemplateFetch(templateAPI, "pharmacy_phone");

    String sql =
        "SELECT customer_id, firstname, surname, outstanding_balance, account_status " +
        "FROM ca_customers " +
        "WHERE outstanding_balance > 0 " +
        "AND account_status IN ('SUSPENDED', 'IN_DEFAULT')";

    String maxIdSql = "SELECT COALESCE(MAX(reminder_id), 0) + 1 AS next_id FROM ca_payment_reminders";

    try (Connection conn = DBConnection.getConnection()) {
        if (conn == null) {
            throw new Exception("Database connection failed.");
        }

        int nextId = 1;
        try (PreparedStatement ps = conn.prepareStatement(maxIdSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                nextId = rs.getInt("next_id");
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int customerId = rs.getInt("customer_id");
                String status = rs.getString("account_status");
                String customerName = rs.getString("firstname") + " " + rs.getString("surname");
                double outstandingBalance = rs.getDouble("outstanding_balance");

                if ("SUSPENDED".equalsIgnoreCase(status)) {
                    if (!reminderExists(conn, customerId, "FIRST")) {
                        insertReminder(conn, nextId++, customerId, "FIRST");
                        count++;
                    }
                } else if ("IN_DEFAULT".equalsIgnoreCase(status)) {
                    if (!reminderExists(conn, customerId, "SECOND")) {
                        insertReminder(conn, nextId++, customerId, "SECOND");
                        count++;
                    }
                }
            }
        }
    }

    return count;
}

@Override
public java.util.List<String> generateReminders(String accountId) throws Exception {
    int customerId = parseCustomerId(accountId);
    TemplateAPI templateAPI = new TemplateAPI_Impl();
    String firstTemplate = templateAPI.getTemplate("reminder_first");
    String secondTemplate = templateAPI.getTemplate("reminder_second");
    String pharmacyName = templateAPI.getTemplate("pharmacy_name");
    String pharmacyAddress = safeTemplateFetch(templateAPI, "pharmacy_address");
    String pharmacyEmail = safeTemplateFetch(templateAPI, "pharmacy_email");
    String pharmacyPhone = safeTemplateFetch(templateAPI, "pharmacy_phone");

    List<String> reminders = new ArrayList<>();

    String customerSql =
        "SELECT firstname, surname, outstanding_balance, account_status " +
        "FROM ca_customers WHERE customer_id = ?";

    String maxIdSql = "SELECT COALESCE(MAX(reminder_id), 0) + 1 AS next_id FROM ca_payment_reminders";

    try (Connection conn = DBConnection.getConnection()) {
        if (conn == null) {
            throw new Exception("Database connection failed.");
        }

        int nextId = 1;
        try (PreparedStatement ps = conn.prepareStatement(maxIdSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                nextId = rs.getInt("next_id");
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(customerSql)) {
            ps.setInt(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String customerName = rs.getString("firstname") + " " + rs.getString("surname");
                    double outstandingBalance = rs.getDouble("outstanding_balance");
                    String accountStatus = rs.getString("account_status");

                    if ("SUSPENDED".equalsIgnoreCase(accountStatus)) {
                        if (!reminderExists(conn, customerId, "FIRST")) {
                            insertReminder(conn, nextId++, customerId, "FIRST");
                            reminders.add(buildReminderText(
                                firstTemplate,
                                customerName,
                                formatAccountId(customerId),
                                outstandingBalance,
                                pharmacyName,
                                pharmacyAddress,
                                pharmacyEmail,
                                pharmacyPhone,
                                LocalDate.now().plusDays(7).toString()));
                        }
                    } else if ("IN_DEFAULT".equalsIgnoreCase(accountStatus)) {
                        if (!reminderExists(conn, customerId, "SECOND")) {
                            insertReminder(conn, nextId++, customerId, "SECOND");
                            reminders.add(buildReminderText(
                                secondTemplate,
                                customerName,
                                formatAccountId(customerId),
                                outstandingBalance,
                                pharmacyName,
                                pharmacyAddress,
                                pharmacyEmail,
                                pharmacyPhone,
                                LocalDate.now().toString()));
                        }
                    }
                }
            }
        }
    }

    return reminders;
}

private String buildReminderText(String template,
                                 String customerName,
                                 String accountId,
                                 double outstandingBalance,
                                 String pharmacyName,
                                 String pharmacyAddress,
                                 String pharmacyEmail,
                                 String pharmacyPhone,
                                 String dueDate) {
    if (template == null) {
        template = "";
    }

    String balanceValue = String.format("£%.2f", outstandingBalance);

    return template
        .replace("{customer_name}", customerName)
        .replace("{account_id}", accountId)
        .replace("{balance}", balanceValue)
        .replace("{due_amount}", balanceValue)
        .replace("{pharmacy_name}", pharmacyName)
        .replace("{pharmacy_address}", pharmacyAddress)
        .replace("{pharmacy_email}", pharmacyEmail)
        .replace("{pharmacy_phone}", pharmacyPhone)
        .replace("{due_date}", dueDate == null ? "" : dueDate);
}

private boolean reminderExists(Connection conn, int customerId, String reminderType) throws Exception {
    String sql = "SELECT 1 FROM ca_payment_reminders WHERE customer_id = ? AND reminder_type = ? LIMIT 1";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, customerId);
        ps.setString(2, reminderType);

        try (ResultSet rs = ps.executeQuery()) {
            return rs.next();
        }
    }
}

private void insertReminder(Connection conn, int reminderId, int customerId, String reminderType) throws Exception {
    String insertSql = "INSERT INTO ca_payment_reminders (reminder_id, customer_id, reminder_type, generated_at, status) " +
                       "VALUES (?, ?, ?, datetime('now'), 'GENERATED')";

    try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
        ps.setInt(1, reminderId);
        ps.setInt(2, customerId);
        ps.setString(3, reminderType);
        ps.executeUpdate();
    }
}

private String safeTemplateFetch(TemplateAPI templateAPI, String key) {
    try {
        return templateAPI.getTemplate(key);
    } catch (Exception e) {
        return "";
    }
}

@Override
public void clearReminderStatusesIfPaid(String accountId) throws Exception {
    // No reminder-status columns exist in the current customer schema.
    // Reminder delivery is tracked only in ca_payment_reminders, so this method
    // is a no-op for the current database design.
}

@Override
public double getOutstandingBalanceByUsername(String username) throws Exception {
    if (username == null || username.isBlank()) {
        return 0;
    }

    String sql = "SELECT outstanding_balance FROM ca_customers WHERE lower(email) = lower(?) LIMIT 1";

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        if (conn == null) {
            throw new Exception("Database connection failed.");
        }

        ps.setString(1, username.trim());

        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble("outstanding_balance");
            }
        }
    }

    return 0;
}

}

