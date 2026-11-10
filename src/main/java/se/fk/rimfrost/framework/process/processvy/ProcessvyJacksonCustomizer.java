package se.fk.rimfrost.framework.process.processvy;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;
import se.fk.rimfrost.framework.process.jaxrsspec.controllers.generatedsource.model.ProcessSteg;

/**
 * Kontraktet säger att {@code steg} saknas i svaret för ett steg utan understeg. Den genererade modellen serialiserar
 * null-fält, så fältet utelämnas här när det är null. Bara {@link ProcessSteg#getSteg()} påverkas, så att svaret för en
 * platt processbeskrivning är detsamma som tidigare.
 */
@Singleton
public class ProcessvyJacksonCustomizer implements ObjectMapperCustomizer
{
   @Override
   public void customize(ObjectMapper objectMapper)
   {
      objectMapper.addMixIn(ProcessSteg.class, ProcessStegMixin.class);
   }

   abstract static class ProcessStegMixin
   {
      @JsonInclude(JsonInclude.Include.NON_NULL)
      abstract List<ProcessSteg> getSteg();
   }
}
