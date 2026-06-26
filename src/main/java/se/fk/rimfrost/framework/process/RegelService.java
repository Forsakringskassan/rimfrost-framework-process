package se.fk.rimfrost.framework.process;

import org.eclipse.microprofile.config.Config;
import org.kie.kogito.internal.process.runtime.KogitoProcessContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import se.fk.rimfrost.framework.regel.RegelErrorInformation;
import se.fk.rimfrost.framework.regel.RegelRequestMessagePayloadData;
import se.fk.rimfrost.framework.regel.RegelResponseMessagePayloadData;
import se.fk.rimfrost.framework.regel.Utfall;

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

   public RegelProcessResult handleError(String handlaggningId, RegelResponseMessagePayloadData response)
   {
      LOGGER.error("Received error response for handlaggningId: {}, with error: {}", handlaggningId, response.getError());
      return new RegelProcessResult(Utfall.ERROR, response.getError());
   }

   public RegelProcessResult handleTimeout(String handlaggningId, RegelRequestMessagePayloadData request)
   {
      LOGGER.error("Timeout for handlaggningId: {}, on the request: {}", handlaggningId, request);
      RegelErrorInformation regelErrorInformation = new RegelErrorInformation();
      regelErrorInformation.setFelkod("TIMEOUT");
      regelErrorInformation.setFelmeddelande("Timeout while waiting for response from regel");
      return new RegelProcessResult(Utfall.ERROR, regelErrorInformation);
   }

   public void init(KogitoProcessContext kcontext)
   {
      kcontext.setVariable("attempts", 0);
   }
}
