package se.fk.rimfrost.framework.process;

import se.fk.rimfrost.HandlaggningRequestMessageData;
import se.fk.rimfrost.HandlaggningResponseMessageData;
import se.fk.rimfrost.framework.regel.Utfall;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProcessService
{

   public String startProcess(HandlaggningRequestMessageData handlaggningRequest)
   {
      var handlaggningId = handlaggningRequest.getHandlaggningId();
      System.out.printf("Started process for handlaggningId %s%n", handlaggningId);
      return handlaggningId;
   }

   public HandlaggningResponseMessageData informAboutDecision(String handlaggningId, Utfall utfall)
   {
      System.out.printf("Process for handlaggningId %s finished with result %s%n", handlaggningId, utfall);
      HandlaggningResponseMessageData vahHandlaggningResponseMessageData = new HandlaggningResponseMessageData();
      vahHandlaggningResponseMessageData.setHandlaggningId(handlaggningId);
      vahHandlaggningResponseMessageData.setResultat(utfall == Utfall.JA ? "GODKÄND" : "EJ GODKÄND");
      return vahHandlaggningResponseMessageData;
   }

}
