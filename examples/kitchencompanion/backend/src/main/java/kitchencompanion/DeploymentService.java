package kitchencompanion;

import jsweet.lang.Erased;

@Erased
public class DeploymentService {

  public static boolean isDeployed() {
    String deploy = System.getenv("deploy");
    return deploy != null && deploy.equalsIgnoreCase("true");
  }

  public static boolean isDevEnvironment() {
    return !isDeployed();
  }

}