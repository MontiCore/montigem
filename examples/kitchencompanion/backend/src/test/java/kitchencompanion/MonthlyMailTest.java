package kitchencompanion;

import kitchencompanion.test_infrastructure.KitchenCompanionMockTestSetup;
import org.junit.jupiter.api.Test;
import umlp.test_rte.ClientSetup;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Who gets the monthly invoice mail and what the invoice and admin overview show.
 * Sending itself is not tested, it needs the real SMTP server.
 */
public class MonthlyMailTest extends KitchenCompanionMockTestSetup {

  public MonthlyMailTest() {
    super(ClientSetup.of(0));
  }

  private static User user(String name, String lastName, double balance) {
    return KitchenCompanionManager.userBuilder()
        .name(name)
        .lastName(lastName)
        .nickname(name.substring(0, 1) + lastName.substring(0, 2))
        .email(name.toLowerCase() + "@example.com")
        .balance(balance)
        .role(KitchenCompanionRole.USER)
        .build()
        .get();
  }

  private static User userWithReceipt(String name, String lastName, double sum, double debt,
                                      double pastBalance, LocalDate date) {
    User u = user(name, lastName, 0.0);
    u.addReceipt(KitchenCompanionManager.receiptBuilder()
        .sum(sum)
        .debt(debt)
        .pastBalance(pastBalance)
        .amountPaid(0.0)
        .confirmed(false)
        .paid(false)
        .date(date)
        .build()
        .get());
    return u;
  }

  private static User tomWithReceipt(double sum, double debt, double pastBalance) {
    return userWithReceipt("Tom", "Hanks", sum, debt, pastBalance, LocalDate.now().minusMonths(1));
  }

  @Test
  public void everyoneWithAReceiptFromLastMonthGetsAnInvoice() {
    // paid completely from the balance: used to get no mail at all
    assertTrue(MailContentService.shouldReceiveInvoice(tomWithReceipt(1.5, 0.0, 10.0)));
    assertTrue(MailContentService.shouldReceiveInvoice(tomWithReceipt(1.5, 0.0, 0.0)));
  }

  @Test
  public void nobodyWithoutAReceiptFromLastMonthGetsAnInvoice() {
    assertFalse(MailContentService.shouldReceiveInvoice(user("Chris", "Evans", 5.0)));
    User old = userWithReceipt("Meryl", "Streep", 1.5, 0.0, 0.0, LocalDate.now().minusMonths(2));
    assertFalse(MailContentService.shouldReceiveInvoice(old));
  }

  @Test
  public void invoiceShowsPurchasesAndTotalWithTwoDecimals() {
    String mail = MailContentService.getMailContent(tomWithReceipt(1.5, 0.0, 0.0));

    assertTrue(mail.contains("Hallo Tom Hanks,"), mail);
    assertTrue(mail.contains("Hello Tom Hanks,"), mail);
    assertTrue(mail.contains("Ihre KitchenCompanion-Rechnung vom " + MailContentService.getDate("deu")), mail);
    assertTrue(mail.contains("Diesen Monat gekauft: <strong>1,50 €"), mail);
    assertTrue(mail.contains("Gesamtbetrag: <strong>1,50 €"), mail);
    assertTrue(mail.contains("Purchased this month: <strong>1.50 €"), mail);
    assertTrue(mail.contains("Total amount: <strong>1.50 €"), mail);
    assertFalse(mail.contains("Guthaben"), mail);
    assertFalse(mail.contains("Schulden"), mail);
  }

  @Test
  public void invoicePaidFromTheBalanceShowsTheRemainingCreditInsteadOfANegativeTotal() {
    String mail = MailContentService.getMailContent(tomWithReceipt(1.5, 0.0, 10.0));

    assertTrue(mail.contains("Abzüglich Guthaben: <strong>10,00 €"), mail);
    assertTrue(mail.contains("Vom Guthaben bezahlt, Restguthaben: <strong>8,50 €"), mail);
    assertTrue(mail.contains("Less credit balance: <strong>10.00 €"), mail);
    assertTrue(mail.contains("Paid from your credit balance, remaining credit: <strong>8.50 €"), mail);
    assertFalse(mail.contains("Gesamtbetrag"), mail);
    assertFalse(mail.contains("<strong>-"), mail);
  }

  @Test
  public void invoicePaidWithTheExactBalanceLeavesNoCredit() {
    // 0.1 + 0.2 is not exactly 0.3 as a double, the total is still treated as 0
    String mail = MailContentService.getMailContent(tomWithReceipt(0.1 + 0.2, 0.0, 0.3));

    assertTrue(mail.contains("Vom Guthaben bezahlt, Restguthaben: <strong>0,00 €"), mail);
    assertFalse(mail.contains("Gesamtbetrag"), mail);
  }

  @Test
  public void debtIsPositiveInBothLanguagesAndAddedToTheTotal() {
    String mail = MailContentService.getMailContent(tomWithReceipt(1.5, 2.0, 0.0));

    assertTrue(mail.contains("Offene Schulden aus dem Vormonat: <strong>2,00 €"), mail);
    assertTrue(mail.contains("Outstanding debt from last month: <strong>2.00 €"), mail);
    assertTrue(mail.contains("Gesamtbetrag: <strong>3,50 €"), mail);
    assertTrue(mail.contains("Total amount: <strong>3.50 €"), mail);
    assertFalse(mail.contains("Guthaben"), mail);
  }

  @Test
  public void debtAndBalanceTogetherAreBothListedSoTheLinesAddUp() {
    // 2 € debt + 3 € bought - 1 € balance = 4 €
    String mail = MailContentService.getMailContent(tomWithReceipt(3.0, 2.0, 1.0));

    assertTrue(mail.contains("Offene Schulden aus dem Vormonat: <strong>2,00 €"), mail);
    assertTrue(mail.contains("Diesen Monat gekauft: <strong>3,00 €"), mail);
    assertTrue(mail.contains("Abzüglich Guthaben: <strong>1,00 €"), mail);
    assertTrue(mail.contains("Gesamtbetrag: <strong>4,00 €"), mail);
  }

  @Test
  public void adminOverviewShowsLastMonthOnlyAndPositiveBalances() {
    tomWithReceipt(1.5, 0.0, 10.0);
    // receipt from two months ago must not show up as this month's numbers
    User chris = userWithReceipt("Chris", "Evans", 7.0, 1.0, 0.0, LocalDate.now().minusMonths(2));
    chris.setBalance(5.0);

    String mail = MailContentService.getAdminMailContent();

    assertTrue(mail.contains("<th>Name</th><th>Gekauft</th><th>Schulden</th><th>Guthaben</th>"
        + "<th>Zu zahlen</th><th>Restguthaben</th>"), mail);
    assertTrue(mail.contains("<td>Tom Hanks</td><td>1,50 €</td><td>0,00 €</td><td>10,00 €</td>"
        + "<td>0,00 €</td><td>8,50 €</td>"), mail);
    assertTrue(mail.contains("<td>Chris Evans</td><td>0,00 €</td><td>0,00 €</td><td>5,00 €</td>"
        + "<td>0,00 €</td><td>5,00 €</td>"), mail);
    assertTrue(mail.contains("<td>Tom Hanks</td><td>1.50 €</td><td>0.00 €</td><td>10.00 €</td>"
        + "<td>0.00 €</td><td>8.50 €</td>"), mail);
    assertFalse(mail.contains("<td>-"), mail);
  }

  @Test
  public void invoiceWithAnOpenAmountLinksToPayPalWithTheAmountFilledIn() {
    String mail = MailContentService.getMailContent(tomWithReceipt(1.5, 2.0, 0.0));

    String link = "https://paypal.me/sekaffeekasse/3,50EUR";
    assertTrue(mail.contains("Jetzt per PayPal bezahlen: <a href='" + link + "'>" + link + "</a>"), mail);
    assertTrue(mail.contains("Pay now via PayPal: <a href='" + link + "'>"), mail);
    // the amount is only prefilled, the user is asked to double-check it
    assertTrue(mail.contains("Hinweis: Bitte prüfen Sie vor dem Bezahlen, ob in PayPal der richtige Betrag eingetragen ist."), mail);
    assertTrue(mail.contains("Note: Please check that PayPal shows the correct amount before you pay."), mail);
  }

  @Test
  public void invoicePaidFromTheBalanceHasNoPayPalLink() {
    String mail = MailContentService.getMailContent(tomWithReceipt(1.5, 0.0, 10.0));

    assertFalse(mail.contains("paypal.me"), mail);
    assertFalse(mail.contains("Hinweis"), mail);
  }

  @Test
  public void payPalLinkAlwaysUsesACommaAndTwoDecimals() {
    // PayPal.me needs "4,05" for this account, also in the English part of the mail
    assertEquals("https://paypal.me/sekaffeekasse/4,05EUR", MailContentService.payPalLink(405));
    assertEquals("https://paypal.me/sekaffeekasse/0,30EUR", MailContentService.payPalLink(30));
    assertEquals("https://paypal.me/sekaffeekasse/1234,00EUR", MailContentService.payPalLink(123400));
  }

  @Test
  public void mailTextsHaveNoTypos() {
    String invoice = MailContentService.getMailContent(tomWithReceipt(1.5, 0.0, 0.0));
    String admin = MailContentService.getAdminMailContent();

    assertFalse(invoice.contains("Rechnung  vom"), invoice);
    assertFalse(admin.contains("antworten antworten"), admin);
  }
}
