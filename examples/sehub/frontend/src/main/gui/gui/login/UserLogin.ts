/* (c) https://github.com/MontiCore/monticore */

import {Component,ChangeDetectionStrategy} from '@angular/core';
import {UserLoginComponent,config} from 'gui/login/UserLoginComponent';
import {User} from 'sehub/User';
import {SehubSecurityManager} from "sehub/SehubSecurityManager";

@Component({
  changeDetection: ChangeDetectionStrategy.Eager,
  imports:[...config.imports],
  selector: config.selector,
  templateUrl: config.templateUrl
})
export class UserLogin extends UserLoginComponent {

  public override async init(): Promise<void> {
    if (this.loginUser == null) {
      this.loginUser = await new User().__UserConstructor();
    }

    if (SehubSecurityManager.isSignedIn()) {
      this._router.navigate(['gui/Start']);
    }

    await super.init();
  }

  protected error: string = "";

  public override loginOnEnter(): (arg: KeyboardEvent) => void {
    return (event: KeyboardEvent) => {
      if (event.key === "Enter") {
        this.login();
      }
    }
  }

  public override login(): (arg: void) => void {
    return $ => {
      if (!this.loginUser || !this.loginUser.getUsername() || !this.loginUser.getPassword()) {
        this.setError("Please enter an account name and a password.");
        return;
      }
      this.setError("");

      const form = new FormData();
      form.append('username', this.loginUser.getUsername());
      form.append('password', this.loginUser.getPassword());

      const requestOptions: RequestInit = {
        method: "POST",
        body: form,
        headers: {
          'Accept': 'application/json'
        }
      };
      fetch("/umlp/api/login", requestOptions)
        .then(response => response.json())
        .then(result => {
          if (result.success) {
            SehubSecurityManager.signIn(this.loginUser);
            window.location.href = 'gui/Start';
          } else if (result.reason) {
            this.setError("Login failed: " + result.reason);
          } else {
            this.setError("Login failed.");
          }
        })
        .catch(error => {
          console.warn(error);
          this.setError("Login failed: unable to reach the server.");
        });
    }
  }

  public override getErrorValue(): string {
    return this.error;
  }

  protected setError(message: string): void {
    this.error = message;
    this.getErrorValueForAngular__update();
  }
}
