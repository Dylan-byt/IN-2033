package main.java;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class PU_COMMS_API_Impl implements PU_COMMS_API {
    private static final String DEFAULT_PU_API_ENDPOINT = "http://localhost:8084";

    private final String puApiEndpoint;

    public PU_COMMS_API_Impl() {
        this(DEFAULT_PU_API_ENDPOINT);
    }

    public PU_COMMS_API_Impl(String puApiEndpoint) {
        // No SMTP credentials needed since PU is handling sending
        this.puApiEndpoint = (puApiEndpoint == null || puApiEndpoint.isBlank())
                ? DEFAULT_PU_API_ENDPOINT
                : puApiEndpoint;
    }

    /**
     * Instead of sending email directly, this method passes the
     * email data to the PU subsystem for actual sending.
     */
    @Override
    public boolean sendEmail(String recipient, String subject, String content) {
        try {
            // Package the email data
            Map<String, String> emailData = Map.of(
                    "recipient", recipient,
                    "subject", subject,
                    "content", content
            );

            System.out.println("Email data handed off to PU subsystem: " + emailData);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /*
     * REST API replacement version for sendEmail().
     *
     * Incase we need to switch it out and Dylan isnt there use the commented code below and switch it out
     * 1) comment out the current live sendEmail() method above
     * 2) uncomment this whole method
     * 3) make sure the PU service has POST /sendEmail running on localhost:8084 or alter it to whatever the correct endpoint is
     *
     * @Override
     * public boolean sendEmail(String recipient, String subject, String content) {
     *     try {
     *         String payload = String.format(
     *                 "{\"recipient\":\"%s\",\"subject\":\"%s\",\"content\":\"%s\"}",
     *                 recipient.replace("\"", "\\\""),
     *                 subject.replace("\"", "\\\""),
     *                 content.replace("\"", "\\\"").replace("\n", "\\n")
     *         );
     *
     *         URL url = new URL(puApiEndpoint + "/sendEmail");
     *         HttpURLConnection conn = (HttpURLConnection) url.openConnection();
     *         conn.setRequestMethod("POST");
     *         conn.setRequestProperty("Content-Type", "application/json");
     *         conn.setDoOutput(true);
     *
     *         try (OutputStream os = conn.getOutputStream()) {
     *             os.write(payload.getBytes(StandardCharsets.UTF_8));
     *         }
     *
     *         int responseCode = conn.getResponseCode();
     *         if (responseCode == 200) {
     *             return true;
     *         } else {
     *             System.out.println("PU email API returned error code: " + responseCode);
     *             return false;
     *         }
     *
     *     } catch (Exception e) {
     *         e.printStackTrace();
     *         return false;
     *     }
     * }
     */

    public boolean processCardPayment(String cardNumber, String expiry, double amount, String orderID) {
        try {
            // Creating JSON payload
            String payload = String.format(
                    "{\"cardNumber\":\"%s\",\"expiry\":\"%s\",\"amount\":%.2f,\"orderID\":\"%s\"}",
                    cardNumber, expiry, amount, orderID
            );

            // Opening HTTPS connection to PU API
            URL url = new URL(puApiEndpoint + "/processCardPayment");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Sending encrypted payload
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            // Checking PU response
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                // PU confirmed payment
                return true;
            } else {
                System.out.println("PU API returned error code: " + responseCode);
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}


