/* (c) https://github.com/MontiCore/monticore */

import {Component,ChangeDetectionStrategy} from '@angular/core';
import {UserLoginComponent,config} from 'gui/login/UserLoginComponent';
import {User} from 'sehub/User';
import {SehubSecurityManager} from "sehub/SehubSecurityManager";

@Component({
  changeDetection: ChangeDetectionStrategy.Eager,
  imports:[...config.imports],
  selector: config.selector,
  templateUrl: config.templateUrl,
  styles: config.styles,
  styleUrls: ["./Login.scss"]
})
export class UserLogin extends UserLoginComponent {

  public ngOnInit(): void {
    if (SehubSecurityManager.isSignedIn()) {
      this._router.navigate(['gui/Start']);
    }

    if (this.loginUser == null) {
      this.loginUser = new User();
    }
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
        return;
      }
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
                }else if (result.reason) {
                  alert("Login Failed: " + result.reason)
                }else{
                  alert("Login Failed: Unknown")
                }
              })
              .catch(error => {
                console.warn(error);
                alert("Login Failed: Unable to communicate");
              });
    }
  }

  public override getErrorValue(): string {
    return this.error;
  }
}
