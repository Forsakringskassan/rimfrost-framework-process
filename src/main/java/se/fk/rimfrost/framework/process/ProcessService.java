package se.fk.rimfrost.framework.process;

import se.fk.rimfrost.HandlaggningRequestMessageData;
import se.fk.rimfrost.HandlaggningResponseMessageData;
import se.fk.rimfrost.framework.regel.Utfall;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProcessService
{

   private static final Logger LOGGER = LoggerFactory.getLogger(ProcessService.class);

   public String startProcess(HandlaggningRequestMessageData handlaggningRequest)
   {
      var handlaggningId = handlaggningRequest.getHandlaggningId();
      LOGGER.info("Started process for handlaggningId {}", handlaggningId);
      return handlaggningId;
   }

   public HandlaggningResponseMessageData endProcess(String handlaggningId, Utfall utfall)
   {
      LOGGER.info("Process for handlaggningId {} finished with result {}", handlaggningId, utfall);
      HandlaggningResponseMessageData vahHandlaggningResponseMessageData = new HandlaggningResponseMessageData();
      vahHandlaggningResponseMessageData.setHandlaggningId(handlaggningId);
      vahHandlaggningResponseMessageData.setResultat(utfall == Utfall.JA ? "GODKÄND" : "EJ GODKÄND");
      return vahHandlaggningResponseMessageData;
   }

}
