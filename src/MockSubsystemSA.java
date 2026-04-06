import io.javalin.Javalin;
import java.sql.Connection;
import java.util.Map;
import stock.CA_Stock_API_Impl;
import database.DBConnection;

public class MockSubsystemSA {

        public static void main(String[] args) {

        System.out.println("Starting CA Stock API on port 8083...");

        Connection conn = DBConnection.getConnection();
        CA_Stock_API_Impl stockApi = new CA_Stock_API_Impl(conn);

        Javalin app = Javalin.create().start(8083);

        app.post("/api/stock/delivery", ctx -> {

            Map<String, Object> body = ctx.bodyAsClass(Map.class);

            int productId = (int) body.get("product_id");
            int quantity = (int) body.get("quantity");

            boolean result = stockApi.recordDelivery(productId, quantity);

            if (result) {
                ctx.json(Map.of("status", "success"));
            } else {
                ctx.status(400).json(Map.of("status", "failed"));
            }
        });
    }
}