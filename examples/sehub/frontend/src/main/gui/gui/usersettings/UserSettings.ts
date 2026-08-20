/* (c) https://github.com/MontiCore/monticore */

import {Component,ChangeDetectionStrategy} from '@angular/core';
import {UserSettingsComponent,config} from 'gui/usersettings/UserSettingsComponent';
import {SehubSecurityManager} from "sehub/SehubSecurityManager";
import {SehubManager} from 'sehub/SehubManager';

@Component({
  changeDetection: ChangeDetectionStrategy.Eager,
  imports:[...config.imports],
  selector: config.selector,
  templateUrl: config.templateUrl,
  styles: config.styles,
  styleUrls: config.styleUrls
})
export class UserSettings extends UserSettingsComponent {

  private changePasswordValue = "";

  public ngOnInit(): void {
    if (!SehubSecurityManager.isSignedIn()) {
      this._router.navigate(['gui/login/LoginPage']);
    }
    this.gui_usersettings_UserSettings_currentUser_RoutingId = SehubSecurityManager.getCurrentUserIdOrNull();

    super.ngOnInit();
  }

  public inviteUserLeftClick(): (arg: void) => void {
    return $ => this._router.navigate(['gui/login/RegistrationPage']);
  }
}
