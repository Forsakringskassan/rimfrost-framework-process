package se.fk.rimfrost.framework.process;

import org.eclipse.microprofile.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import se.fk.rimfrost.framework.regel.RegelRequestMessagePayloadData;
import se.fk.rimfrost.framework.regel.RegelResponseMessagePayloadData;

@ApplicationScoped
public class RegelService
{

   @Inject
   Config config;

   private static final Logger LOGGER = LoggerFactory.getLogger(RegelService.class);

   public RegelProcessResult onRegelResponse(RegelResponseMessagePayloadData response)
   {
      LOGGER.info("Received regel response: {}", response.toString());
      return new RegelProcessResult(response.getUtfall(), response.getError());
   }

   public RegelRequestMessagePayloadData createRegelRequest(String handlaggningId, String aktivitetId,
         String responseTopicProperty)
   {
      LOGGER.info("Starting create RegelRequest for handlaggningId: {}, aktivitetId: {}, responseTopicProperty: {}",
            handlaggningId, aktivitetId, responseTopicProperty);
      String responseTopicName = config.getValue(responseTopicProperty, String.class);

      LOGGER.info("Created RegelRequest with handlaggningId: {} and aktivitetId: {}. replyTo: {}", handlaggningId,
            aktivitetId, responseTopicName);
      RegelRequestMessagePayloadData requestMessageData = new RegelRequestMessagePayloadData();
      requestMessageData.setHandlaggningId(handlaggningId);
      requestMessageData.setAktivitetId(aktivitetId);
      requestMessageData.setReplyTo(responseTopicName);
      return requestMessageData;
   }

}
