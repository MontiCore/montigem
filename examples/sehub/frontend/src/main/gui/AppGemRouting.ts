/* (c) https://github.com/MontiCore/monticore */
import { inject } from "@angular/core";
import { CanActivateFn, Router, Routes } from "@angular/router";
import { SehubSecurityManager } from "sehub/SehubSecurityManager";
import { routes as routesTOP } from "AppGemRoutingTOP"

/**
 * The CD2GUI-generated pages under /sehub have no login check of their own,
 * so the whole generated CRUD surface is guarded here in one place.
 */
export const signedInGuard: CanActivateFn = () => {
  if (SehubSecurityManager.isSignedIn()) {
    return true;
  }
  return inject(Router).createUrlTree(["gui/login/LoginPage"]);
};

export const routes: Routes = [
  {
    path: "",
    pathMatch: "full",
    redirectTo: "gui/login/LoginPage"
  },
  ...routesTOP.map(route =>
    route.path === "sehub" ? { ...route, canActivate: [signedInGuard] } : route)
]
