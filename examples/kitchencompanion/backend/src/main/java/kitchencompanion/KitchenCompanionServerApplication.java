package kitchencompanion;

import org.springframework.boot.SpringApplication;
import jakarta.annotation.PostConstruct;

import java.io.IOException;

public class KitchenCompanionServerApplication extends KitchenCompanionServerApplicationTOP {

  public static void main(String[] args) {
    SpringApplication.run(KitchenCompanionServerApplication.class, args);
  }

  @PostConstruct
  @Override
  public void init() throws IOException {
    super.init();
    loadDummyDataIfNotDeployed();
  }

  private void loadDummyDataIfNotDeployed() {
    if (DeploymentService.isDevEnvironment()) {
      createDummyUsers();
      createDummyOffers();
    }
  }

  private void createDummyUsers() {
    if (KitchenCompanionManager.getUserList().isEmpty()) {
      // VIP Users
      createUser("Tom", "Hanks", "hanks@example.com", "THa", 0.0, KitchenCompanionRole.VIP);
      createUser("Meryl", "Streep", "streep@example.com", "MSt", 0.0, KitchenCompanionRole.VIP);
      //Admin Users
      createUser("Denzel", "Washington", "washington@example.com", "DWa", 0.0, KitchenCompanionRole.ADMIN);
      //Normal Users
      createUser("Timothée", "Chalamet", "chalamet@example.com", "TCh", 0.0, KitchenCompanionRole.USER);
      createUser("Tom", "Holland", "holland@example.com", "THo", 0.0, KitchenCompanionRole.USER);
      createUser("Margot", "Robbie", "robbie@example.com", "MRo", 0.0, KitchenCompanionRole.USER);
      createUser("Ryan", "Gosling", "gosling@example.com", "RGo", 0.0, KitchenCompanionRole.USER);
      createUser("Emma", "Stone", "stone@example.com", "ESt", 0.0, KitchenCompanionRole.USER);
      createUser("Emma", "Watson", "watson@example.com", "EWa", 0.0, KitchenCompanionRole.USER);
      createUser("Daniel", "Radcliffe", "radcliffe@example.com", "DRa", 0.0, KitchenCompanionRole.USER);
      createUser("Florence", "Pugh", "pugh@example.com", "FPu", 0.0, KitchenCompanionRole.USER);
      createUser("Chris", "Hemsworth", "hemsworth@example.com", "CHe", 0.0, KitchenCompanionRole.USER);
      createUser("Chris", "Evans", "evans@example.com", "CEv", 0.0, KitchenCompanionRole.USER);
      createUser("Scarlett", "Johansson", "johansson@example.com", "SJo", 0.0, KitchenCompanionRole.USER);
      createUser("Robert", "Downey", "downey@example.com", "RDo", 0.0, KitchenCompanionRole.USER);
      createUser("Leonardo", "DiCaprio", "dicaprio@example.com", "LDi", 0.0, KitchenCompanionRole.USER);
      createUser("Brad", "Pitt", "pitt@example.com", "BPi", 0.0, KitchenCompanionRole.USER);
      createUser("Viola", "Davis", "davis@example.com", "VDa", 0.0, KitchenCompanionRole.USER);
      createUser("Idris", "Elba", "elba@example.com", "IEl", 0.0, KitchenCompanionRole.USER);
      createUser("Mahershala", "Ali", "ali@example.com", "MAl", 0.0, KitchenCompanionRole.USER);
      createUser("Zoe", "Saldana", "saldana@example.com", "ZSa", 0.0, KitchenCompanionRole.USER);
      createUser("", "Office", "office@example.com", "Office", 0.0, KitchenCompanionRole.EXTRA);
      createUser("Chris", "Pratt", "pratt@example.com", "CPr", 0.0, KitchenCompanionRole.USER);
      createUser("Jennifer", "Lawrence", "lawrence@example.com", "JLa", 0.0, KitchenCompanionRole.USER);
      createUser("Dwayne", "Johnson", "johnson@example.com", "DJo", 0.0, KitchenCompanionRole.USER);
      createUser("Keanu", "Reeves", "reeves@example.com", "KRe", 0.0, KitchenCompanionRole.USER);
      createUser("Natalie", "Portman", "portman@example.com", "NPo", 0.0, KitchenCompanionRole.USER);
      createUser("Cate", "Blanchett", "blanchett@example.com", "CBl", 0.0, KitchenCompanionRole.USER);
      createUser("Joaquin", "Phoenix", "phoenix@example.com", "JPh", 0.0, KitchenCompanionRole.USER);
      createUser("Saoirse", "Ronan", "ronan@example.com", "SRo", 0.0, KitchenCompanionRole.USER);
      createUser("Pedro", "Pascal", "pascal@example.com", "PPa", 0.0, KitchenCompanionRole.USER);
      createUser("Millie", "Brown", "brown@example.com", "MBr", 0.0, KitchenCompanionRole.USER);
      createUser("Ryan", "Reynolds", "reynolds@example.com", "RRe", 0.0, KitchenCompanionRole.USER);
      createUser("Hugh", "Jackman", "jackman@example.com", "HJa", 0.0, KitchenCompanionRole.USER);
      createUser("Gal", "Gadot", "gadot@example.com", "GGa", 0.0, KitchenCompanionRole.USER);
      createUser("Zoë", "Kravitz", "kravitz@example.com", "ZKr", 0.0, KitchenCompanionRole.USER);
      createUser("Jenna", "Ortega", "ortega@example.com", "JOr", 0.0, KitchenCompanionRole.USER);
    }
  }

  private void createDummyOffers() {
    if (KitchenCompanionManager.getOfferList().isEmpty()) {
      createOffer("Kaffee", 0.25, "assets/images/Kaffeebohnen.png", true, ProductType.DRINK);
      createOffer("Wasser", 0.75, "assets/images/Water.png", true, ProductType.DRINK);
      createOffer("Cola/ Zero", 0.5, "assets/images/Cola.png", true, ProductType.DRINK);
      createOffer("Mate", 1.0, "assets/images/Mate.png", true, ProductType.DRINK);
      createOffer("Erdnüsse", 1.0, "assets/images/Erdnuesse.png", true, ProductType.SNACK);
      createOffer("Müsli-/ Fruchtriegel", 0.25, "assets/images/Muesli_Fruchtriegel.png", true, ProductType.SNACK);
      createOffer("Instant Nudeln", 0.40,"assets/images/InstantNudeln.png",true, ProductType.SNACK);

      for (User u : KitchenCompanionManager.getUserList()) {
        for (Offer o : KitchenCompanionManager.getOfferList()) {
          if (o.isInStock()) {
            u.addTallies(KitchenCompanionManager.tallyBuilder().count(0).offer(o).build().get());
            u.addPrevTallies(KitchenCompanionManager.tallyBuilder().count(0).offer(o).build().get());
          }
        }
      }
    }
  }

  private void createUser(String firstName, String lastName, String email, String nickname, double balance, KitchenCompanionRole role) {
    KitchenCompanionManager.userBuilder()
        .name(firstName)
        .lastName(lastName)
        .email(email)
        .nickname(nickname)
        .balance(balance)
        .role(role)
        .build();
  }

  private void createOffer(String name, double price, String imgSource, boolean inStock, ProductType productType) {
    KitchenCompanionManager.offerBuilder()
        .name(name)
        .price(price)
        .imgSource(imgSource)
        .inStock(inStock)
        .productType(productType)
        .build();
  }
}
