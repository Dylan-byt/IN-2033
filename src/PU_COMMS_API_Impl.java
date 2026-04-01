import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

/**
 * Email service implementation for sending notifications
 * 
 * ⚠️  SECURITY WARNING: Password is hardcoded in this file!
 * This works for development/testing but should NOT be used in production.
 * For production, use environment variables instead.
 */
public class PU_COMMS_API_Impl implements PU_COMMS_API {

    // Email configuration
    private String senderEmail;
    private String senderPassword;
    private String smtpHost;
    private int smtpPort;

    /**
     * Constructor with hardcoded Gmail credentials
     */
    public PU_COMMS_API_Impl() {
        this(
            "inproject2033@gmail.com",
            "xhrkbaipnfptxaom",        
            "smtp.gmail.com",
            587
        );
    }

    /**
     * Constructor with custom SMTP configuration
     *
     * @param senderEmail Email address to send from
     * @param senderPassword Email password or app password
     * @param smtpHost SMTP server host (e.g., smtp.gmail.com)
     * @param smtpPort SMTP server port (e.g., 587 for TLS)
     */
    public PU_COMMS_API_Impl(String senderEmail, String senderPassword, String smtpHost, int smtpPort) {
        this.senderEmail = senderEmail;
        this.senderPassword = senderPassword;
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
    }

    /**
     * Send an email to the specified recipient
     *
     * @param recipient Email address of recipient
     * @param subject Subject line
     * @param content Email body content
     * @return true if email sent successfully, false otherwise
     */
    @Override
    public boolean sendEmail(String recipient, String subject, String content) {
        try {
            // Set up SMTP properties
            Properties props = new Properties();
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", String.valueOf(smtpPort));
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");

            // Create session with authentication
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(senderEmail, senderPassword);
                }
            });

            // Create message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject(subject);
            message.setText(content);

            // Send the email
            Transport.send(message);

            System.out.println("✓ Email sent successfully to: " + recipient);
            return true;

        } catch (MessagingException e) {
            System.out.println("✗ Failed to send email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Send HTML email
     *
     * @param recipient Email address of recipient
     * @param subject Subject line
     * @param htmlContent HTML formatted email body
     * @return true if email sent successfully, false otherwise
     */
    public boolean sendHtmlEmail(String recipient, String subject, String htmlContent) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", String.valueOf(smtpPort));
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(senderEmail, senderPassword);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject(subject);

            // Set content as HTML
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlContent, "text/html; charset=utf-8");

            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(htmlPart);

            message.setContent(multipart);

            Transport.send(message);

            System.out.println("✓ HTML email sent successfully to: " + recipient);
            return true;

        } catch (MessagingException e) {
            System.out.println("✗ Failed to send HTML email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Getters and setters for configuration
    public void setCredentials(String email, String password) {
        this.senderEmail = email;
        this.senderPassword = password;
    }

    public void setSmtpServer(String host, int port) {
        this.smtpHost = host;
        this.smtpPort = port;
    }
}
