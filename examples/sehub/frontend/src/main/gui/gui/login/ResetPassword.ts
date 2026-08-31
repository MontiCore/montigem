/* (c) https://github.com/MontiCore/monticore */

import {Component,ChangeDetectionStrategy} from '@angular/core';
import {Location} from '@angular/common';
import {User} from 'sehub/User';
import {ResetPasswordComponent,config} from 'gui/login/ResetPasswordComponent';

@Component({
  changeDetection: ChangeDetectionStrategy.Eager,
  imports:[...config.imports],
  selector: config.selector,
  templateUrl: config.templateUrl
})
export class ResetPassword extends ResetPasswordComponent {

  private error: string = "";

  public static email: RegExp = /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i;

  public constructor(protected location: Location) {
    super();
  }

  public override async init(): Promise<void> {
    if (this.user == null) {
      this.user = await new User().__UserConstructor();
    }

    await super.init();
  }

  public override passwordReset(): (arg: void) => void {
    return $ => { void this.passwordResetAsync(); };
  }

  private async passwordResetAsync(): Promise<void> {
    const user = this.getUser();
    if (!user || !user.getEmail()) {
      this.setError("Please enter your e-mail address.");
      return;
    }
    if (!ResetPassword.email.test(user.getEmail())) {
      this.setError("Wrong format for Email address!");
      return;
    }
    this.setError("");

    try {
      const response = await fetch("/umlp/api/public/reset_password", {
        method: "POST",
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({email: user.getEmail()})
      });
      const result = await response.json();
      if (result.success) {
        this.setError("If that address has an account, a new password was sent. "
            + "In this example it is written to the server log.");
      } else {
        this.setError(result.reason ? result.reason : "The password could not be reset.");
      }
    } catch (error) {
      console.warn(error);
      this.setError("Password reset failed: unable to reach the server.");
    }
  }

  public override getErrorValue(): string {
    return this.error;
  }

  protected setError(message: string): void {
    this.error = message;
    this.getErrorValueForAngular__update();
  }

  public override navigateBack(): (arg: void) => void {
    return $ => this.location.back();
  }

}
