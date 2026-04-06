/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package test;

/**
 *
 * @author laraashour
 */

import database.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CustomerTest {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection()) {

            System.out.println("ca_customers columns:");
            PreparedStatement ps1 = conn.prepareStatement("PRAGMA table_info(ca_customers)");
            ResultSet rs1 = ps1.executeQuery();

            while (rs1.next()) {
                System.out.println(
                    rs1.getInt("cid") + " | " +
                    rs1.getString("name") + " | " +
                    rs1.getString("type")
                );
            }

            System.out.println("\nSample rows:");
            PreparedStatement ps2 = conn.prepareStatement("SELECT * FROM ca_customers LIMIT 5");
            ResultSet rs2 = ps2.executeQuery();

            while (rs2.next()) {
                System.out.println("customer_id = " + rs2.getString("customer_id"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
