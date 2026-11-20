package kitchencompanion;

import kitchencompanion.test_infrastructure.KitchenCompanionMockTestSetup;
import org.junit.jupiter.api.Test;
import umlp.test_rte.ClientSetup;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

import static kitchencompanion.KitchenCompanionTestData.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contents of the monthly invoice mail and the admin overview mail. Sending itself is not tested.
 */
public class MailContentServiceTest extends KitchenCompanionMockTestSetup {

  public MailContentServiceTest() {
    super(ClientSetup.of(0));
  }

  private static User userWithReceipt(double sum, double debt, double pastBalance) {
    User u = user("Tom", "Hanks", "THa", KitchenCompanionRole.USER);
    u.addReceipt(KitchenCompanionManager.receiptBuilder()
        .sum(sum)
        .debt(debt)
        .pastBalance(pastBalance)
        .amountPaid(0.0)
        .confirmed(false)
        .paid(false)
        .date(LocalDate.now().minusMonths(1))
        .build()
        .get());
    return u;
  }

  @Test
  public void dateIsThePreviousMonthInTheRequestedLanguage() {
    LocalDate lastMonth = LocalDate.now().minusMonths(1);

    assertEquals(lastMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.GERMAN) + " "
        + lastMonth.getYear(), MailContentService.getDate("deu"));
    assertEquals(lastMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " "
        + lastMonth.getYear(), MailContentService.getDate("eng"));
  }

  @Test
  public void invoiceGreetsTheUserInBothLanguages() {
    String mail = MailContentService.getMailContent(userWithReceipt(1.5, 0.0, 0.0));

    assertTrue(mail.contains("Hallo Tom Hanks,"), mail);
    assertTrue(mail.contains("Hello Tom Hanks,"), mail);
    assertTrue(mail.contains(MailContentService.getDate("deu")), mail);
    assertTrue(mail.contains(MailContentService.getDate("eng")), mail);
  }

  @Test
  public void invoiceShowsPurchasesAndTotalWithLocaleFormatting() {
    String mail = MailContentService.getMailContent(userWithReceipt(1.5, 0.0, 0.0));

    assertTrue(mail.contains("Diesen Monat gekauft: <strong>1,5€"), mail);
    assertTrue(mail.contains("Gesamtbetrag: <strong>1,5€"), mail);
    assertTrue(mail.contains("Purchased this month: <strong>1.5€"), mail);
    assertTrue(mail.contains("Total amount: <strong>1.5€"), mail);
    assertFalse(mail.contains("Guthaben"), mail);
    assertFalse(mail.contains("Schulden"), mail);
  }

  @Test
  public void invoiceMentionsTheCreditBalanceAndANegativeTotal() {
    String mail = MailContentService.getMailContent(userWithReceipt(1.5, 0.0, 10.0));

    assertTrue(mail.contains("Guthaben: <strong>-10€"), mail);
    assertTrue(mail.contains("Credit balance: <strong>-10€"), mail);
    assertTrue(mail.contains("Gesamtbetrag: <strong>-8,5€"), mail);
    assertTrue(mail.contains("Total amount: <strong>-8.5€"), mail);
    assertFalse(mail.contains("Schulden"), mail);
  }

  @Test
  public void invoiceMentionsOutstandingDebtAndAddsItToTheTotal() {
    String mail = MailContentService.getMailContent(userWithReceipt(1.5, 2.0, 0.0));

    assertTrue(mail.contains("Noch nicht getilgte Schulden"), mail);
    assertTrue(mail.contains("Outstanding debt"), mail);
    assertTrue(mail.contains("Gesamtbetrag: <strong>3,5€"), mail);
    assertTrue(mail.contains("Total amount: <strong>3.5€"), mail);
    assertFalse(mail.contains("Guthaben"), mail);
  }

  @Test
  public void adminOverviewListsEveryUserInDisplayOrder() {
    userWithReceipt(1.5, 0.0, 0.0);
    user("Chris", "Evans", "CEv", KitchenCompanionRole.USER);
    user("Meryl", "Streep", "MSt", KitchenCompanionRole.VIP);

    String mail = MailContentService.getAdminMailContent();

    assertTrue(mail.contains("<th>Betrag</th>"), mail);
    assertTrue(mail.contains("<th>Value</th>"), mail);
    // German table: VIP first, then alphabetically by last name
    int streep = mail.indexOf("<td>Meryl Streep</td>");
    int evans = mail.indexOf("<td>Chris Evans</td>");
    int hanks = mail.indexOf("<td>Tom Hanks</td>");
    assertTrue(streep >= 0 && evans >= 0 && hanks >= 0, mail);
    assertTrue(streep < evans && evans < hanks, mail);
    // the user with a receipt shows its sum, the others zero
    assertTrue(mail.contains("<td>Tom Hanks</td><td>1,5 €</td>"), mail);
    assertTrue(mail.contains("<td>Chris Evans</td><td>0 €</td>"), mail);
    assertTrue(mail.contains("<td>Tom Hanks</td><td>1.5 €</td>"), mail);
  }
}
