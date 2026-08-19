package kitchencompanion.commands;

//import kitchencompanion.EmailService;

import kitchencompanion.KitchenCompanionManager;
import kitchencompanion.KitchenCompanionRole;
import kitchencompanion.MailContentService;
import kitchencompanion.MailService;
import kitchencompanion.User;
import umlp.backendrte.command.StatusResult;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

import javax.mail.MessagingException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;


public class SendEmail extends SendEmailTOP {

  @Override
  @BackendOnly(BackendOnlyModes.THROW_ERROR)
  public StatusResult doAction() {
    String date = MailContentService.getDate("deu");
    for (User u : KitchenCompanionManager.getUserList()) {
        try {
          if(!u.getReceiptsList().isEmpty()){
            LocalDate receiptDate = u.getReceipts(0).getDate();
            String uDate = receiptDate.getMonth().getDisplayName(TextStyle.FULL, Locale.GERMAN) + " " + receiptDate.getYear();
            if (uDate.equals(date)&&(u.hasOrderedTallies() || u.getReceipts(0).getSum() + u.getReceipts(0).getDebt() - u.getReceipts(0).getPastBalance() > 0)) {
              MailService.sendMail(MailContentService.getMailContent(u), u.getEmail(), "KitchenCompanion Rechnung vom " + date);
            }
          }
          if (u.getRole() == KitchenCompanionRole.ADMIN) {
            MailService.sendMail(MailContentService.getAdminMailContent(), u.getEmail(),"KitchenCompanion Admin Übersicht vom " + date);
          }
        } catch (MessagingException e) {
          throw new RuntimeException(e);
        }
      }
    return StatusResult.ok(getId());
  }
}
