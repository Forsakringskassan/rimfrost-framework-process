package se.fk.rimfrost.framework.process;

import jakarta.enterprise.context.ApplicationScoped;
import se.fk.rimfrost.framework.regel.RegelRequestMessagePayloadData;
import se.fk.rimfrost.framework.regel.RegelResponseMessagePayloadData;
import se.fk.rimfrost.framework.regel.Utfall;

@ApplicationScoped
public class RegelService
{
   public Utfall onRegelResponse(RegelResponseMessagePayloadData response)
   {
      System.out.printf("Received regel response: %s", response.toString());
      return response.getUtfall();
   }

   public RegelRequestMessagePayloadData createRegelRequest(String handlaggningId, String aktivitetId)
   {
      System.out.printf("Created RegelRequest with handlaggningId: %s and aktivitetId: %s", handlaggningId, aktivitetId);
      RegelRequestMessagePayloadData requestMessageData = new RegelRequestMessagePayloadData();
      requestMessageData.setHandlaggningId(handlaggningId);
      requestMessageData.setAktivitetId(aktivitetId);
      return requestMessageData;
   }

}
