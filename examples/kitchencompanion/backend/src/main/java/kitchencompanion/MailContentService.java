package kitchencompanion;

import jsweet.lang.Erased;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
@Erased
public abstract class MailContentService {

  private static final String DEU = "deu";
  private static final String ENG = "eng";
  private static final String PAYPAL_ME = "https://paypal.me/sekaffeekasse/";

  public static Receipt lastMonthReceipt(User user) {
    if (user.getReceiptsList() == null || user.getReceiptsList().isEmpty()) {
      return null;
    }
    Receipt receipt = user.getReceipts(0);
    if (receipt.getDate() == null
        || !YearMonth.from(receipt.getDate()).equals(YearMonth.now().minusMonths(1))) {
      return null;
    }
    return receipt;
  }

  public static boolean shouldReceiveInvoice(User user) {
    return lastMonthReceipt(user) != null;
  }

  public static String getMailContent(User user) {
    Receipt receipt = user.getReceipts(0);
    return "/* English version below */ <br><br>" +
        invoice(user, receipt, DEU) +
        "######################################################################## <br><br>" +
        invoice(user, receipt, ENG);
  }

  private static String invoice(User user, Receipt receipt, String lang) {
    boolean deu = !ENG.equals(lang);
    String text = (deu ? "Hallo " : "Hello ") + user.getName() + " " + user.getLastName() + ",<br>" +
        (deu ? "Ihre KitchenCompanion-Rechnung vom " + getDate(DEU) + " ist verfügbar.<br><br>"
            : "Your KitchenCompanion invoice from " + getDate(ENG) + " is now available.<br><br>");
    if (receipt.getDebt() > 0) {
      text += line(deu ? "Offene Schulden aus dem Vormonat" : "Outstanding debt from last month",
          receipt.getDebt(), lang);
    }
    text += line(deu ? "Diesen Monat gekauft" : "Purchased this month", receipt.getSum(), lang);
    if (receipt.getPastBalance() > 0) {
      text += line(deu ? "Abzüglich Guthaben" : "Less credit balance", receipt.getPastBalance(), lang);
    }
    long totalCents = totalCents(receipt);
    if (totalCents > 0) {
      text += line(deu ? "Gesamtbetrag" : "Total amount", totalCents / 100.0, lang);
      String link = payPalLink(totalCents);
      text += (deu ? "Jetzt per PayPal bezahlen: " : "Pay now via PayPal: ")
          + "<a href='" + link + "'>" + link + "</a><br>"
          + "<small>" + (deu
              ? "Hinweis: Bitte prüfen Sie vor dem Bezahlen, ob in PayPal der richtige Betrag eingetragen ist."
              : "Note: Please check that PayPal shows the correct amount before you pay.")
          + "</small><br><br>";
    } else {
      text += line(deu ? "Vom Guthaben bezahlt, Restguthaben" : "Paid from your credit balance, remaining credit",
          -totalCents / 100.0, lang);
    }
    return text + (deu
        ? "Mit freundlichen Grüßen<br>" +
          "Ihr KitchenCompanion-System<br><br>" +
          "---<br>" +
          "Diese E-Mail wurde automatisch generiert. Bitte antworten Sie nicht auf diese Adresse.<br>" +
          "---<br>"
        : "Kind regards,<br>" +
          "Your KitchenCompanion System<br><br>" +
          "---<br>" +
          "This email was generated automatically. Please do not reply to this address.<br>");
  }

  private static String line(String label, double value, String lang) {
    return label + ": <strong>" + formatEuro(value, lang) + "</strong><br><br>";
  }

  public static String payPalLink(long cents) {
    return PAYPAL_ME + String.format(Locale.ROOT, "%d,%02d", cents / 100, cents % 100) + "EUR";
  }

  private static long totalCents(Receipt receipt) {
    return Math.round((receipt.getSum() + receipt.getDebt() - receipt.getPastBalance()) * 100);
  }

  public static String formatEuro(double value, String lang) {
    NumberFormat format = NumberFormat.getNumberInstance(getLocale(lang));
    format.setMinimumFractionDigits(2);
    format.setMaximumFractionDigits(2);
    return format.format(value) + " €";
  }

  public static String getAdminMailContent() {
    return "/* English version below */ <br><br>" +
        "// Deutsch <br>" +
        "Monatliche KitchenCompanion-Abrechnung vom " + getDate(DEU) + " – Admin-Übersicht<br><br>" +
        "Gesamte Übersicht aller Nutzer und Beträge:<br><br>" +
        getUserTable(DEU) + "<br><br>" +
        "Mit freundlichen Grüßen<br>" +
        "Ihr KitchenCompanion System<br><br>" +
        "---<br>" +
        "Diese E-Mail wurde automatisch generiert. Bitte antworten Sie nicht auf diese Adresse.<br>" +
        "---<br><br>" +
        "######################################################################## <br><br><br><br>" +
        "// English<br>" +
        "Monthly KitchenCompanion Billing for " + getDate(ENG) + " – Admin Overview<br><br>" +
        "Complete overview of all users and their amounts:<br><br>" +
        getUserTable(ENG) + "<br><br>" +
        "Best regards<br>" +
        "Your KitchenCompanion System<br><br>" +
        "---<br>" +
        "This email was generated automatically. Please do not reply.<br>" +
        "---<br>";
  }

  private static String getUserTable(String lang) {
    String adminTable = "<table border='1' cellspacing='0' cellpadding='6'>";
    if (!ENG.equals(lang)) {
      adminTable += "<tr><th>Name</th><th>Gekauft</th><th>Schulden</th><th>Guthaben</th>"
          + "<th>Zu zahlen</th><th>Restguthaben</th></tr>";
    } else {
      adminTable += "<tr><th>Name</th><th>Purchased</th><th>Debt</th><th>Balance</th>"
          + "<th>To pay</th><th>Remaining credit</th></tr>";
    }
    for (User u : sortUsersList(KitchenCompanionManager.getUserList())) {
      Receipt receipt = lastMonthReceipt(u);
      double sum = 0;
      double debt = 0;
      double balance = u.getBalance();
      double toPay = 0;
      double remaining = u.getBalance();
      if (receipt != null) {
        long totalCents = totalCents(receipt);
        sum = receipt.getSum();
        debt = receipt.getDebt();
        balance = receipt.getPastBalance();
        toPay = Math.max(0, totalCents) / 100.0;
        remaining = Math.max(0, -totalCents) / 100.0;
      }
      adminTable += "<tr><td>" + u.getName() + " " + u.getLastName() + "</td>"
          + cell(sum, lang) + cell(debt, lang) + cell(balance, lang)
          + cell(toPay, lang) + cell(remaining, lang) + "</tr>";
    }
    return adminTable + "</table>";
  }

  private static String cell(double value, String lang) {
    return "<td>" + formatEuro(value, lang) + "</td>";
  }

  public static String getDate(String lang) {
    LocalDateTime date = LocalDateTime.now().minusMonths(1);
    return date.getMonth().getDisplayName(TextStyle.FULL, getLocale(lang)) + " " + date.getYear();
  }

  private static Locale getLocale(String lang) {
    if (ENG.equals(lang)) {
      return Locale.ENGLISH;
    } else {
      return Locale.GERMAN;
    }
  }

  private static List<User> sortUsersList(List<User> userList) {
    List<User> users = new ArrayList<>(userList);
    User.sortForDisplay(users);
    return users;
  }
}
