package sehub;/* (c) https://github.com/MontiCore/monticore */
import static def.dom.Globals.*;
import def.js.JSON;

public class SehubSecurityManager {

  public static User getCurrentUser() {
    return SehubManager.getUserSure(getCurrentUserIdOrNull());
  }

  public static String getCurrentUserName() {
    return localStorage.getItem("currentUserName").toString();
  }

  public static void setCurrentUser(User value) {
    localStorage.setItem("currentUserID", JSON.stringify(value.getGemId()));
    localStorage.setItem("currentUserName", value.getUsername());
  }

  public static Long getCurrentUserIdOrNull() {
    return Long.valueOf(localStorage.getItem("currentUserID").toString());
  }

  public static void signIn(User value) {
    setCurrentUser(value);
  }

  public static void signOut() {
    localStorage.removeItem("currentUserID");
    localStorage.removeItem("currentUserName");
  }

  public static Boolean isSignedIn() {
    return localStorage.getItem("currentUserID") != null;
  }

}


