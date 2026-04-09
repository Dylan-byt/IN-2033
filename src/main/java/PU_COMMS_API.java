package main.java;

public interface PU_COMMS_API {

    /**
     * Sends card payment details securely to the PU subsystem.
     *
     * @param cardNumber Full card number
     * @param expiry Expiry date (MM/YY)
     * @param amount Payment amount
     * @param orderID Associated order ID
     */
    boolean processCardPayment(String cardNumber, String expiry, double amount, String orderID);

    boolean sendEmail(String recipient, String subject, String content);
}
