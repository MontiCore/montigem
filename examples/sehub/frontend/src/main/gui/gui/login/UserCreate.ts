/* (c) https://github.com/MontiCore/monticore */

import {Component,ChangeDetectionStrategy} from '@angular/core';
import {Location} from '@angular/common';
import {User} from 'sehub/User';
import {SehubRole} from 'sehub/SehubRole';
import {SehubManager} from 'sehub/SehubManager';
import {BoardMapper} from 'sehub/BoardMapper';
import {BingoEntry} from 'sehub/BingoEntry';
import {GemList,AsyncList} from '@umlp/commonj2ts';
import {UserCreateComponent,config} from 'gui/login/UserCreateComponent';

@Component({
  changeDetection: ChangeDetectionStrategy.Eager,
  imports:[...config.imports],
  selector: config.selector,
  templateUrl: config.templateUrl,
  styles: config.styles,
  styleUrls: ["./Login.scss"]
})
export class UserCreate extends UserCreateComponent {

  private error: string = "";

  protected users: GemList<User>;

  public static email: RegExp = /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i;

  private permutation : number[];
  private entries : BingoEntry[];

  public constructor(protected location: Location) {
    super();
  }

  public override async init(): Promise<void> {
    super.init();
    return SehubManager.getUserList().then(gemPromiseRes => {
      this.users = gemPromiseRes;
    });
  }

  public override createUser(): (arg: void) => void {
    return $ => SehubManager.getBingoEntryList().then(entryList => {
      entryList.toArrayDeepSync().then(e => {
        this.entries = e;
        this.error = "";
        if (!this.getNewUser().getUsername() || !this.getNewUser().getPassword() || !this.getNewUser().getEmail()) {
          return;
        }
        this.validateUser().then(errorMessage => {
          if (!errorMessage) {
            this.permutation = this.getBoardPermutation();
            this.buildMapping().then(mapping => {
              //this.user.activated(ActivationStatus.AKTIVIERT);
              this.getNewUser().setRegistrationDate(new Date());
              this.getNewUser().setRole(SehubRole.DEVELOPER);
              // this.getNewUser().setGridEntry1(mapping);
              this.getNewUser().build();
              this._router.navigate(['/Login']);
            });
          } else {
            this.error = errorMessage;
          }
        });
      });
    });
  }

  async buildMapping(): Promise<Set<BoardMapper>>  {
    let gridEntrySet = new Set<BoardMapper>()

    for (let i = 0; i <= 24; i++) {
      let builder = await SehubManager.boardMapperBuilder();
      let boardMapper = await (await
          builder.bingoBoardEntry(this.entries[this.permutation[i]])
              .then(b => b.isChecked(false))).build();
      gridEntrySet.add(boardMapper);
    }
    console.log(gridEntrySet)
    return gridEntrySet;
  }


  public getBoardPermutation(): number[] {
    const arr: number[] = Array.from({ length: 25 }, (_, i) => i);
    for (let i = arr.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [arr[i], arr[j]] = [arr[j], arr[i]]; // Swap elements
    }
    return arr;
  }

  public override getErrorValue(): string {
    return this.error;
  }

  private async validateUser(): Promise<string> {
    let res: string = '';
    // Check Email format
    if (!UserCreate.email.test(this.getNewUser().getEmail())) {
      return 'Wrong format for Email address!';
    }

    // Check if user is unique
    this.users.stream().forEach((user : User) => {
      if (this.getNewUser().getUsername() === user.getUsername()) {
        res += ' Account "' + this.getNewUser().getUsername() + '"';
      }
      if (this.getNewUser().getEmail() === user.getEmail()) {
        res += (res.length === 0? '': ' und ') + ' E-mail "' + this.getNewUser().getEmail() + '"';
      }
    });
    return res + (res.length === 0? '': ' already being used!');
  }

  public override navigateBack(): (arg: void) => void {
    return $ => this.location.back();
  }

}
