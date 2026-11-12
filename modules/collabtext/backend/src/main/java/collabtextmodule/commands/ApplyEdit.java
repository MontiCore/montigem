/* (c) https://github.com/MontiCore/monticore */
package collabtextmodule.commands;

import collabtextmodule.CollabTextModuleManager;
import collabtextmodule.CollabTextSync;
import collabtextmodule.TextEdit;
import umlp.backendrte.command.StatusResult;
import umlp.jsweet.extension.annotation.BackendOnly;
import umlp.jsweet.extension.annotation.BackendOnlyModes;

public class ApplyEdit extends ApplyEditTOP {
  public ApplyEdit(long textGemId, long authorGemId, int baseRevision, String edit) {
    super(textGemId, authorGemId, baseRevision, edit);
  }

  @Override
  @BackendOnly(BackendOnlyModes.THROW_ERROR)
  public StatusResult doAction() {
    boolean applied = CollabTextSync.apply(CollabTextModuleManager.getCollabTextSure(getTextGemId()),
        getAuthorGemId(), getBaseRevision(), TextEdit.decode(getEdit()));
    return applied ? replySuccess() : replyFail();
  }
}
