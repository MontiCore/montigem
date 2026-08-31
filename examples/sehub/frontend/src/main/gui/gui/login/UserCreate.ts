/* (c) https://github.com/MontiCore/monticore */

import {Component,ChangeDetectionStrategy} from '@angular/core';
import {Location} from '@angular/common';
import {User} from 'sehub/User';
import {UserCreateComponent,config} from 'gui/login/UserCreateComponent';

@Component({
  changeDetection: ChangeDetectionStrategy.Eager,
  imports:[...config.imports],
  selector: config.selector,
  templateUrl: config.templateUrl
})
export class UserCreate extends UserCreateComponent {

  private error: string = "";

  public static email: RegExp = /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i;

  public constructor(protected location: Location) {
    super();
  }

  public override async init(): Promise<void> {
    if (this.newUser == null) {
      this.newUser = await new User().__UserConstructor();
    }

    await super.init();
  }

  public override createUser(): (arg: void) => void {
    return $ => { void this.createUserAsync(); };
  }

  private async createUserAsync(): Promise<void> {
    const newUser = this.getNewUser();
    if (!newUser || !newUser.getUsername() || !newUser.getPassword() || !newUser.getEmail()) {
      this.setError("Please fill in account name, password and e-mail address.");
      return;
    }
    if (!UserCreate.email.test(newUser.getEmail())) {
      this.setError("Wrong format for Email address!");
      return;
    }
    this.setError("");

    try {
      const response = await fetch("/umlp/api/public/signup", {
        method: "POST",
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          username: newUser.getUsername(),
          password: newUser.getPassword(),
          email: newUser.getEmail()
        })
      });
      const result = await response.json();
      if (result.success) {
        this._router.navigate(['gui/login/LoginPage']);
      } else {
        this.setError(result.reason ? result.reason : "The account could not be created.");
      }
    } catch (error) {
      console.warn(error);
      this.setError("Registration failed: unable to reach the server.");
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
