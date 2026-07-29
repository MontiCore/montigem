package string;

import resources.domain.Person;

public class TestStringTranspilation {

  String value = null;

  public void testOnGlobalVariable(Person person) {
    value.equals("This is another String");
  }

  public void test(String myString) {
    myString.equals("This is another String");
  }

  public void testOnPersonName(Person person) {
    person.getName().equals("This is another String");
  }

  public void testAsArgument(String myString){
    this.booleanInput(myString.equals("A String"));
  }

  public Boolean testAsReturn(String myString){
    return myString.equals("A String");
  }

  public void booleanInput(Boolean bool) {
    // Do nothing
  }

  public String[] testSplitRegex1() {
    return "a|b|c".split("\\|");
  }

  public String[] testSplitRegex2() {
    return "azbZcczdddZeee".split("(?i)z", 4);
  }
}
