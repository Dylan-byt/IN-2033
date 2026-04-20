/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package login;

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
import static org.junit.Assert.*;

public class SA_LOGIN_APITest {

    private SA_LOGIN_API api;

    @Before
    public void setUp() throws Exception {
        api = new SA_LOGIN_API();

        try (Connection conn = DBConnection.getConnection()) {
            assertNotNull("Database connection should not be null", conn);

            // remove any leftover test staff account if it exists
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM ca_users WHERE username = ?")) {
                ps.setString(1, "junitstaff");
                ps.executeUpdate();
            }

            // make sure Admin role exists because createStaff uses role lookup
            ensureRoleExists(conn, 6, "Admin");
        }

        api.logout();
    }

    @After
    public void tearDown() throws Exception {
        api.logout();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM ca_users WHERE username = ?")) {
            ps.setString(1, "junitstaff");
            ps.executeUpdate();
        }
    }

    @Test
    public void testLoginValidCredentials() {
        boolean result = api.login("manager", "Get_it_done");

        assertTrue("Login should succeed with correct seeded credentials", result);
        assertEquals("manager", api.getCurrentLoggedInUsername());
    }

    @Test
    public void testLoginWrongPassword() {
        boolean result = api.login("manager", "wrongpass");

        assertFalse("Login should fail with incorrect password", result);
    }

    @Test
    public void testGetUserRoleForSeededAdmin() {
        String role = api.getUserRole("sysdba");

        assertEquals("Admin", role);
    }

    @Test
    public void testCreateStaffWithAdminRole() {
        boolean created = api.createStaff("junitstaff", "staffpass", "Admin");

        assertTrue("Staff user should be created successfully", created);
        assertEquals("Admin", api.getUserRole("junitstaff"));
    }

    @Test
    public void testRemoveStaff() {
        api.createStaff("junitstaff", "staffpass", "Admin");

        boolean removed = api.removeStaff("junitstaff");

        assertTrue("Staff user should be removed successfully", removed);
        assertNull("Removed user should no longer exist", api.getUserRole("junitstaff"));
    }

    private void ensureRoleExists(Connection conn, int roleId, String roleName) throws Exception {
        try (PreparedStatement check = conn.prepareStatement(
                "SELECT 1 FROM ca_roles WHERE lower(role_name) = lower(?)")) {
            check.setString(1, roleName);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }

        try (PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO ca_roles (role_id, role_name) VALUES (?, ?)")) {
            insert.setInt(1, roleId);
            insert.setString(2, roleName);
            insert.executeUpdate();
        }
    }
}