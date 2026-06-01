package se.fk.rimfrost.framework.process;

import java.io.Serializable;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import se.fk.rimfrost.framework.regel.RegelErrorInformation;
import se.fk.rimfrost.framework.regel.Utfall;

public class RegelProcessResult implements Serializable
{
   private final Utfall utfall;

   @Nullable
   private final RegelErrorInformation error;

   @SuppressFBWarnings("EI_EXPOSE_REP2")
   public RegelProcessResult(Utfall utfall, RegelErrorInformation error)
   {
      this.utfall = utfall;
      this.error = error;
   }

   public Utfall getUtfall()
   {
      return utfall;
   }

   @Nullable
   @SuppressFBWarnings("EI_EXPOSE_REP")
   public RegelErrorInformation getError()
   {
      return error;
   }
}
