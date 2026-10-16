package expressionwrapper;

public class InterpreterFailedException extends Exception {

  public final Exception wrappedException;

  public InterpreterFailedException(Exception e) {
    wrappedException = e;
  }

  @Override
  public synchronized Throwable getCause() {
    return wrappedException.getCause();
  }

  @Override
  public String getMessage() {
    return "Interpreter failed caused by: "+wrappedException.getMessage();
  }

  @Override
  public StackTraceElement[] getStackTrace() {
    return wrappedException.getStackTrace();
  }
}
