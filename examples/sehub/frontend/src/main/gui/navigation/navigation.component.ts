/* (c) https://github.com/MontiCore/monticore */
import { Component, ChangeDetectionStrategy} from "@angular/core";
import {SehubSecurityManager} from "sehub/SehubSecurityManager";
import {User} from 'sehub/User';
import { RouterModule } from '@angular/router';
import {CommonModule} from '@angular/common';
@Component({
  changeDetection: ChangeDetectionStrategy.Eager,
  imports:[RouterModule,CommonModule],
  selector: "navigation",
  templateUrl: "./navigation.component.html",
  styleUrls: ["./navigation.component.scss"]
})
export class Navigation {

  readonly items: { url: string, title: string }[] = [

    { url: 'gui/SehubMainPage', title: 'Projects' },
    { url: 'gui/issues/IssueTrackerPage', title: 'Issue Tracker' },
    { url: 'gui/usersettings/UserSettings/1', title: 'User Settings' },
    { url: 'sehub/CD2GUIDashboard', title: 'CD2GUI'},
  ];

  getCurrentUserName(): string {
    return SehubSecurityManager.getCurrentUserName();
  }

  signOutCurrentUser(): void {
    fetch("/umlp/api/logout")
    SehubSecurityManager.signOut();
  }
  isSignedIn(): boolean {
    return SehubSecurityManager.isSignedIn();
  }


}
