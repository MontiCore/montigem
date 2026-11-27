/* (c) https://github.com/MontiCore/monticore */

package pojos.commands;

import pojos.FooDomain;
import pojos.PojosManager;
import pojos.TestPojo;
import umlp.backendrte.command.ListResult;

import java.util.ArrayList;
import java.util.List;

public class GetPojoTests extends GetPojoTestsTOP{

  public GetPojoTests(int count){
    super(count);
  }

  @Override
  public ListResult<TestPojo, List<TestPojo>> doAction() {
    // Create a domain entities of FooDomain
    FooDomain foo1 = PojosManager.fooDomainBuilder().a(42).build().get();
    FooDomain foo2 = PojosManager.fooDomainBuilder().a(43).build().get();
    List<FooDomain> foosList = new ArrayList<>();
    foosList.add(foo1);
    foosList.add(foo2);

    List<TestPojo> result = new ArrayList<>();
    for(int i = 0; i < getCount(); i++){
      TestPojo pojo = PojosManager.testPojoBuilder()
          .a(i)
          .fooDomains(foosList)
          .build().get();
      result.add(pojo);
    }
    return ListResult.of(this.getId(), result, TestPojo.class.getName());
  }

}
