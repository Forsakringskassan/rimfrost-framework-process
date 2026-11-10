package se.fk.rimfrost.framework.process.processvy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ProcessvyControllerTest
{
   @Test
   public void should_return_steg_for_known_aktivitet()
   {
      given()
            .queryParam("aktivitetId", "5a6b7c8d-9e0f-4a1b-8c2d-3e4f5a6b7c8d")
            .when()
            .get("/processvy/steg")
            .then()
            .statusCode(200)
            .body("processId", equalTo("test"))
            .body("processnamn", equalTo("Testprocess"))
            .body("steg.id", contains("kontrollera-ratt-till-forsakring", "berakna-ersattning", "bekrafta-beslut"))
            .body("steg.status", contains("KLAR", "PAGAR", "KOMMANDE"));
   }

   @Test
   public void should_return_404_for_unknown_aktivitet()
   {
      given()
            .queryParam("aktivitetId", "0f0f0f0f-0000-4000-8000-000000000001")
            .when()
            .get("/processvy/steg")
            .then()
            .statusCode(404)
            .body("code", equalTo(404))
            .body("message", equalTo(ProcessvyController.AKTIVITET_SAKNAS));
   }

   @Test
   public void should_not_handle_other_paths()
   {
      given()
            .when()
            .get("/processvy/okand")
            .then()
            .statusCode(404);
   }

   @Test
   public void should_return_400_when_aktivitet_is_missing()
   {
      given()
            .when()
            .get("/processvy/steg")
            .then()
            .statusCode(400)
            .body("code", equalTo(400))
            .body("message", equalTo(ProcessvyController.AKTIVITET_OGILTIG));
   }

   @Test
   public void should_return_400_when_aktivitet_is_not_a_uuid()
   {
      given()
            .queryParam("aktivitetId", "inte-ett-uuid")
            .when()
            .get("/processvy/steg")
            .then()
            .statusCode(400)
            .body("code", equalTo(400))
            .body("message", equalTo(ProcessvyController.AKTIVITET_OGILTIG));
   }
}
