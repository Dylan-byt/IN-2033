import io.javalin.Javalin;
import java.sql.Connection;
import java.util.Map;
import stock.CA_Stock_API_Impl;
import database.DBConnection;

public class MockSubsystemSA {

    public static void main(String[] args) {

        final int port = Integer.parseInt(System.getenv().getOrDefault("SA_API_PORT", "8083"));

        System.out.println("Starting Mock Subsystem SA (Stock Delivery API) on port " + port + "...");

        Connection conn = DBConnection.getConnection();
        CA_Stock_API_Impl stockApi = new CA_Stock_API_Impl(conn);

        Javalin app = Javalin.create().start(port);

        app.get("/health", ctx -> ctx.json(Map.of("status", "ok")));
        app.get("/api/ipos_sa/health", ctx -> ctx.json(Map.of("status", "ok")));

        // Namespaced SA route (similar style to other subsystem mocks)
        app.post("/api/ipos_sa/delivery", ctx -> {
            handleDelivery(ctx.bodyAsClass(Map.class), stockApi, ctx);
        });

        // Backward-compatible route already used in your tests/curl commands
        app.post("/api/stock/delivery", ctx -> {
            handleDelivery(ctx.bodyAsClass(Map.class), stockApi, ctx);
        });
    }

    private static void handleDelivery(Map<String, Object> body, CA_Stock_API_Impl stockApi, io.javalin.http.Context ctx) {
        try {
            Object productObj = body.get("product_id");
            Object quantityObj = body.get("quantity");

            if (!(productObj instanceof Number) || !(quantityObj instanceof Number)) {
                ctx.status(400).json(Map.of("status", "failed", "error", "product_id and quantity must be numeric"));
                return;
            }

            int productId = ((Number) productObj).intValue();
            int quantity = ((Number) quantityObj).intValue();

            if (quantity <= 0) {
                ctx.status(400).json(Map.of("status", "failed", "error", "quantity must be > 0"));
                return;
            }

            boolean result = stockApi.recordDelivery(productId, quantity);

            if (result) {
                ctx.json(Map.of("status", "success"));
            } else {
                ctx.status(400).json(Map.of("status", "failed", "error", "product not found or invalid data"));
            }
        } catch (Exception e) {
            ctx.status(400).json(Map.of("status", "failed", "error", "invalid payload"));
        }
    }
}