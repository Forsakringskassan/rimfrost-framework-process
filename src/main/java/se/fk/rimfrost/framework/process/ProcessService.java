package se.fk.rimfrost.framework.process;

import se.fk.rimfrost.HandlaggningErrorInformation;
import se.fk.rimfrost.HandlaggningRequestMessageData;
import se.fk.rimfrost.HandlaggningResponseMessageData;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.RegelErrorInformation;
import se.fk.rimfrost.framework.regel.error.RegelFelkod;

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

   public HandlaggningResponseMessageData endProcess(String handlaggningId, RegelProcessResult regelProcessResult)
   {
      LOGGER.info("Process for handlaggningId {} finished with regelProcessResult {}", handlaggningId,
            regelProcessResult.getUtfall());
      HandlaggningResponseMessageData response = new HandlaggningResponseMessageData();
      response.setHandlaggningId(handlaggningId);
      response.setResultat(regelProcessResult.getUtfall() == Utfall.JA ? "GODKÄND" : "EJ GODKÄND");
      return response;
   }

   public HandlaggningResponseMessageData endProcessWithError(String handlaggningId, RegelErrorInformation regelErrorInformation)
   {
      String felkod = RegelFelkod.RIMFROST_OTHER;
      String felmeddelande = "Unknown error";
      if (regelErrorInformation != null)
      {
         if (regelErrorInformation.getFelkod() != null)
         {
            felkod = regelErrorInformation.getFelkod();
         }
         if (regelErrorInformation.getFelmeddelande() != null)
         {
            felmeddelande = regelErrorInformation.getFelmeddelande();
         }
      }
      LOGGER.error("Process for handlaggningId {} failed with error {}: {}", handlaggningId, felkod,
            felmeddelande);
      HandlaggningErrorInformation errorInfo = new HandlaggningErrorInformation();
      errorInfo.setFelkod(felkod);
      errorInfo.setFelmeddelande(felmeddelande);
      HandlaggningResponseMessageData response = new HandlaggningResponseMessageData();
      response.setHandlaggningId(handlaggningId);
      response.setResultat("FEL");
      response.setError(errorInfo);
      return response;
   }
}
