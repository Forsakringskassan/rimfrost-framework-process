package se.fk.rimfrost.framework.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import se.fk.rimfrost.framework.regel.RegelRequestMessagePayloadData;
import se.fk.rimfrost.framework.regel.RegelResponseMessagePayloadData;
import se.fk.rimfrost.framework.regel.Utfall;

@ApplicationScoped
public class RegelService
{

   private static final Logger LOGGER = LoggerFactory.getLogger(RegelService.class);

   public Utfall onRegelResponse(RegelResponseMessagePayloadData response)
   {
      LOGGER.info("Received regel response: {}", response.toString());
      return response.getUtfall();
   }

   public RegelRequestMessagePayloadData createRegelRequest(String handlaggningId, String aktivitetId)
   {
      LOGGER.info("Created RegelRequest with handlaggningId: {} and aktivitetId: {}", handlaggningId, aktivitetId);
      RegelRequestMessagePayloadData requestMessageData = new RegelRequestMessagePayloadData();
      requestMessageData.setHandlaggningId(handlaggningId);
      requestMessageData.setAktivitetId(aktivitetId);
      return requestMessageData;
   }

}
