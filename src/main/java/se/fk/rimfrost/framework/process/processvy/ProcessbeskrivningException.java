package se.fk.rimfrost.framework.process.processvy;

public class ProcessbeskrivningException extends RuntimeException
{
   public ProcessbeskrivningException(String message)
   {
      super(message);
   }

   public ProcessbeskrivningException(String message, Throwable cause)
   {
      super(message, cause);
   }
}
