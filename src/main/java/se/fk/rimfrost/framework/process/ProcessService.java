package se.fk.rimfrost.framework.process;

import se.fk.rimfrost.HandlaggningErrorInformation;
import se.fk.rimfrost.HandlaggningRequestMessageData;
import se.fk.rimfrost.HandlaggningResponseMessageData;

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

   public HandlaggningResponseMessageData endProcessWithError(String handlaggningId, RegelProcessResult result)
   {
      se.fk.rimfrost.framework.regel.RegelErrorInformation error = result != null ? result.getError() : null;
      se.fk.rimfrost.framework.regel.RegelFelkod felkodEnum = error != null ? error.getFelkod() : null;
      String felkod = felkodEnum != null ? felkodEnum.getValue() : "OKAND";
      String felmeddelande = error != null && error.getFelmeddelande() != null ? error.getFelmeddelande() : "Okant fel";
      LOGGER.error("Process for handlaggningId {} failed with error {}: {}", handlaggningId, felkod, felmeddelande);
      HandlaggningErrorInformation errorInfo = new HandlaggningErrorInformation();
      errorInfo.setFelkod(felkod);
      errorInfo.setFelmeddelande(felmeddelande);
      HandlaggningResponseMessageData response = new HandlaggningResponseMessageData();
      response.setHandlaggningId(handlaggningId);
      response.setResultat("FEL");
      response.setError(errorInfo);
      return response;
   }

   public HandlaggningResponseMessageData endProcessWithError(String handlaggningId, String felkod, String felmeddelande)
   {
      LOGGER.error("Process for handlaggningId {} failed with error {}: {}", handlaggningId, felkod, felmeddelande);
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
