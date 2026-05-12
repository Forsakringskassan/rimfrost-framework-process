package se.fk.rimfrost.framework.process;

import se.fk.rimfrost.framework.regel.RegelErrorInformation;
import se.fk.rimfrost.framework.regel.RegelFelkod;
import se.fk.rimfrost.framework.regel.Utfall;

public class RegelProcessResult
{

   private final Utfall utfall;
   private final RegelFelkod felkod;
   private final String felmeddelande;

   public RegelProcessResult(Utfall utfall, RegelErrorInformation error)
   {
      this.utfall = utfall;
      this.felkod = error != null ? error.getFelkod() : null;
      this.felmeddelande = error != null ? error.getFelmeddelande() : null;
   }

   public Utfall getUtfall()
   {
      return utfall;
   }

   public RegelFelkod getFelkod()
   {
      return felkod;
   }

   public String getFelmeddelande()
   {
      return felmeddelande;
   }
}
