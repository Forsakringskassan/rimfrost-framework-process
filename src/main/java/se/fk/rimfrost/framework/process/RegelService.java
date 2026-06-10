package se.fk.rimfrost.framework.process;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import se.fk.rimfrost.framework.regel.RegelRequestMessagePayloadData;
import se.fk.rimfrost.framework.regel.RegelResponseMessagePayloadData;

@ApplicationScoped
public class RegelService
{

   @ConfigProperty(name = "RESPONSE_TOPIC_NAME") 
   String reponseTopicName;

   private static final Logger LOGGER = LoggerFactory.getLogger(RegelService.class);

   public RegelProcessResult onRegelResponse(RegelResponseMessagePayloadData response)
   {
      LOGGER.info("Received regel response: {}", response.toString());
      return new RegelProcessResult(response.getUtfall(), response.getError());
   }

   public RegelRequestMessagePayloadData createRegelRequest(String handlaggningId, String aktivitetId)
   {
      LOGGER.info("Created RegelRequest with handlaggningId: {} and aktivitetId: {}", handlaggningId, aktivitetId);
      RegelRequestMessagePayloadData requestMessageData = new RegelRequestMessagePayloadData();

      requestMessageData.setHandlaggningId(handlaggningId);
      requestMessageData.setAktivitetId(aktivitetId);
      requestMessageData.setReplyTo(reponseTopicName);
      return requestMessageData;
   }

}
