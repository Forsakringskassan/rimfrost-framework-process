package se.fk.rimfrost.framework.process.processvy;

import java.util.UUID;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import se.fk.rimfrost.framework.process.jaxrsspec.controllers.generatedsource.ProcessvyControllerApi;
import se.fk.rimfrost.framework.process.jaxrsspec.controllers.generatedsource.model.ErrorResponse;
import se.fk.rimfrost.framework.process.jaxrsspec.controllers.generatedsource.model.GetProcessvyStegResponse;

/**
 * Implementerar {@code GET /processvy/steg} enligt rimfrost-framework-process-openapi. Prefixet {@code /processvy}
 * krockar inte med Kogitos genererade endpoints, som ligger under {@code /{processId}}.
 */
@Path("/")
public class ProcessvyController implements ProcessvyControllerApi
{
   static final String AKTIVITET_SAKNAS = "Aktiviteten finns inte i processbeskrivningen";

   static final String AKTIVITET_OGILTIG = "Aktivitets-ID saknas eller är ogiltigt";

   @Inject
   ProcessvyService processvyService;

   @Override
   public GetProcessvyStegResponse getProcessvySteg(UUID aktivitetId)
   {
      if (aktivitetId == null)
      {
         throw new BadRequestException(AKTIVITET_OGILTIG);
      }
      return processvyService.getSteg(aktivitetId)
            .orElseThrow(AktivitetSaknasException::new);
   }

   /**
    * Ett aktivitets-ID som inte är ett UUID kan inte konverteras, och JAX-RS svarar då med 404. Kontraktet säger 400.
    */
   @ServerExceptionMapper
   public Response mapNotFound(NotFoundException e)
   {
      return errorResponse(Response.Status.BAD_REQUEST, AKTIVITET_OGILTIG);
   }

   @ServerExceptionMapper
   public Response mapBadRequest(BadRequestException e)
   {
      return errorResponse(Response.Status.BAD_REQUEST, AKTIVITET_OGILTIG);
   }

   @ServerExceptionMapper
   public Response mapAktivitetSaknas(AktivitetSaknasException e)
   {
      return errorResponse(Response.Status.NOT_FOUND, AKTIVITET_SAKNAS);
   }

   private static Response errorResponse(Response.Status status, String message)
   {
      ErrorResponse errorResponse = new ErrorResponse()
            .code(status.getStatusCode())
            .message(message);
      return Response.status(status).entity(errorResponse).build();
   }

   /**
    * Måste vara public: Quarkus genererar exception-mappern i processens classloader, som annars inte kommer åt
    * klassen när ramverket används som beroende (svaret blir då 500 i stället för 404).
    */
   public static class AktivitetSaknasException extends RuntimeException
   {
      public AktivitetSaknasException()
      {
         super(AKTIVITET_SAKNAS);
      }
   }
}
