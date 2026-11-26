package kitchencompanion.commands;

import kitchencompanion.KitchenCompanionManager;
import kitchencompanion.KitchenCompanionRole;
import kitchencompanion.MailContentService;
import kitchencompanion.MailService;
import kitchencompanion.User;
import umlp.backendrte.command.StatusResult;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

public class SendEmail extends SendEmailTOP {

  @Override
  @BackendOnly(BackendOnlyModes.THROW_ERROR)
  public StatusResult doAction() {
    String date = MailContentService.getDate("deu");
    boolean allSent = true;
    for (User u : KitchenCompanionManager.getUserList()) {
      if (MailContentService.shouldReceiveInvoice(u)) {
        allSent = MailService.trySendMail(MailContentService.getMailContent(u), u.getEmail(),
            "KitchenCompanion Rechnung vom " + date) && allSent;
      }
      if (u.getRole() == KitchenCompanionRole.ADMIN) {
        allSent = MailService.trySendMail(MailContentService.getAdminMailContent(), u.getEmail(),
            "KitchenCompanion Admin Übersicht vom " + date) && allSent;
      }
    }
    return allSent ? StatusResult.ok(getId()) : StatusResult.error(getId());
  }
}
