package kitchencompanion;

import jsweet.lang.Erased;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

@Erased
public abstract class MailService {
  
  protected static final String IDENTIFIER = "KitchenCompanion";
  protected final static Properties properties = new Properties();

  public static void sendMail(String mailText, String email, String subject) throws MessagingException {
    addToProperties("mail.smtp.host", "smarthost-tls.rwth-aachen.de");
    addToProperties("mail.smtp.port", 25);
    addToProperties("mail.smtp.auto", "false");
    addToProperties("mail.transport.protocol", "smtp");

    String SENDER = System.getenv().getOrDefault("mailAbsender", "noreply@kitchenCompanion.de");
    Session session = Session.getInstance(properties, new Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(SENDER, System.getenv().getOrDefault("mailPassword", ""));
      }
    });

    MimeMessage mail = new MimeMessage(session);
    mail.setFrom(new InternetAddress(IDENTIFIER + "<" + SENDER + ">"));
    InternetAddress[] address = {new InternetAddress(email)};
    mail.setRecipients(MimeMessage.RecipientType.TO, address);
    mail.setSubject(subject);
    mail.setText(mailText, "UTF-8", "html");

    Transport t = session.getTransport();
    t.connect();
    t.sendMessage(mail, mail.getAllRecipients());
  }

  private static void addToProperties(String key, Object defaultValue) {
    properties.put(key, (System.getenv(key) != null ? System.getenv(key) : defaultValue));
  }
}



