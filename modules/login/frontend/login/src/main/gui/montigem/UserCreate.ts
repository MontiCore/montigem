/* (c) https://github.com/MontiCore/monticore */

import { Location } from '@angular/common';
import { Component } from '@angular/core';
import { LoginModuleManager } from '../../../../build/umlpJava2ts/loginmodule/LoginModuleManager';
import { config, UserCreateComponent } from '../../../../build/gui2java2ts/montigem/UserCreateComponent';
import { LoginModuleUser } from '../loginmodule/LoginModuleUser';
import { GemTextInputValidity } from '@umlp/input';
import { Temporal } from '@umlp/commonj2ts';

// import { ActivatedRoute, Router } from '@angular/router';
@Component({
  standalone: true,
  imports: [...config.imports],
  selector: config.selector,
  templateUrl: config.templateUrl,
  styles: config.styles,
  // styleUrls: ["./Login.scss"]
  styleUrls: config.styleUrls,
})

export class UserCreate extends UserCreateComponent {
  private error: string = "";

  public static email: RegExp = /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i;

  public constructor(protected location: Location) {
    super();
  }

  public ngOnInit(): void {
    LoginModuleManager.loginModuleUserBuilder().then(gemPromiseRes => {
      this.newUser = gemPromiseRes;
    });
  }

  public override getUsernameValidity(user: LoginModuleUser): GemTextInputValidity {
    return user.getValidityUsername();
  }

  public override getEmailValidity(user: LoginModuleUser): GemTextInputValidity {
    return user.getValidityEmail();
  }

  public override navigateBack(): (arg: void) => void {
    return $ => this.location.back();
  }

  public override createUser(): (arg: void) => void {
    return $ => {
      this.error = "";
      if (!this.getNewUser().getUsername() || !this.getNewUser().getPassword() || !this.getNewUser().getEmail()) {
        return;
      }
      this.validateUser().then(errorMessage => {
        if (!errorMessage) {
          // this.user.activated(ActivationStatus.AKTIVIERT);
          this.newUser.setRegistrationDate(Temporal.Now.plainDateTimeISO());
          // this.newUser.setRole(DtcockpitRole.USER);
          this.newUser.build();
          this._router.navigate(['gui', 'loginmodule', 'LoginPage']);
        } else {
          this.error = errorMessage;
        }
      });
    }
  }

  public override getErrorValue(): string {
    return this.error;
  }

  private validateUser(): Promise<string> {
    // Check Email format
    if (!UserCreate.email.test(this.getNewUser().getEmail())) {
      return Promise.resolve('Wrong format for Email address!');
    }

    return this.users.toArrayDeepSync().then(users => {
      let res = '';

      users.forEach(user => {
        if (this.getNewUser().getUsername() === user.getUsername()) {
          res += ` Account "${this.getNewUser().getUsername()}"`;
        }

        if (this.getNewUser().getEmail() === user.getEmail()) {
          res += (res.length === 0 ? '' : ' und ') +
            ` E-mail "${this.getNewUser().getEmail()}"`;
        }
      });

      return res + (res.length === 0 ? '' : ' already being used!');
    });
  }

}
