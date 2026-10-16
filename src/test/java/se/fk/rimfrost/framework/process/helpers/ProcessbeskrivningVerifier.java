package se.fk.rimfrost.framework.process.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import se.fk.rimfrost.framework.process.processvy.Processbeskrivning;
import se.fk.rimfrost.framework.process.processvy.ProcessbeskrivningReader;

/**
 * Testhjälpmedel för processer: kontrollerar att varje aktivitets-ID i en BPMN-fil finns i processbeskrivningen, så
 * att bygget går sönder om BPMN och processbeskrivning glider isär.
 *
 * <pre>
 * &#64;Test
 * void processbeskrivning_should_contain_all_aktiviteter()
 * {
 *    ProcessbeskrivningVerifier.assertAllAktiviteterInProcessbeskrivning("vah.bpmn");
 * }
 * </pre>
 */
public final class ProcessbeskrivningVerifier
{
   static final String BPMN_NAMESPACE = "http://www.omg.org/spec/BPMN/20100524/MODEL";

   static final String AKTIVITET_ID_INPUT = "aktivitetId";

   private ProcessbeskrivningVerifier()
   {
   }

   /**
    * Kontrollerar BPMN-filen mot processens processbeskrivning ({@value ProcessbeskrivningReader#RESOURCE}).
    */
   public static void assertAllAktiviteterInProcessbeskrivning(String bpmnResource)
   {
      assertAllAktiviteterInProcessbeskrivning(bpmnResource, ProcessbeskrivningReader.RESOURCE);
   }

   public static void assertAllAktiviteterInProcessbeskrivning(String bpmnResource, String processbeskrivningResource)
   {
      Processbeskrivning processbeskrivning = ProcessbeskrivningReader.readResource(processbeskrivningResource);
      Set<String> beskrivnaAktiviteter = new HashSet<>();
      processbeskrivning.steg()
            .forEach(steg -> steg.aktiviteter().forEach(aktivitet -> beskrivnaAktiviteter.add(normalize(aktivitet))));

      Set<String> bpmnAktiviteter = readAktivitetIds(bpmnResource);
      if (bpmnAktiviteter.isEmpty())
      {
         throw new AssertionError("Found no aktivitets-ID in " + bpmnResource
               + ": expected call activities with an '" + AKTIVITET_ID_INPUT + "' input");
      }

      List<String> saknade = bpmnAktiviteter.stream()
            .filter(aktivitet -> !beskrivnaAktiviteter.contains(normalize(aktivitet)))
            .toList();
      if (!saknade.isEmpty())
      {
         throw new AssertionError("Aktivitets-ID in " + bpmnResource + " missing in " + processbeskrivningResource
               + ": " + String.join(", ", saknade));
      }
   }

   /**
    * Läser alla aktivitets-ID:n i BPMN-filen, det vill säga de fasta värden som tilldelas aktiviteternas
    * {@code aktivitetId}-input.
    */
   public static Set<String> readAktivitetIds(String bpmnResource)
   {
      Document document = parse(bpmnResource);

      Set<String> aktivitetInputIds = new HashSet<>();
      NodeList dataInputs = document.getElementsByTagNameNS(BPMN_NAMESPACE, "dataInput");
      for (int i = 0; i < dataInputs.getLength(); i++)
      {
         Element dataInput = (Element) dataInputs.item(i);
         if (AKTIVITET_ID_INPUT.equals(dataInput.getAttribute("name")))
         {
            aktivitetInputIds.add(dataInput.getAttribute("id"));
         }
      }

      Set<String> aktivitetIds = new LinkedHashSet<>();
      List<String> utanFastVarde = new ArrayList<>();
      NodeList associations = document.getElementsByTagNameNS(BPMN_NAMESPACE, "dataInputAssociation");
      for (int i = 0; i < associations.getLength(); i++)
      {
         Element association = (Element) associations.item(i);
         String targetRef = childText(association, "targetRef");
         if (targetRef == null || !aktivitetInputIds.contains(targetRef))
         {
            continue;
         }
         NodeList froms = association.getElementsByTagNameNS(BPMN_NAMESPACE, "from");
         String value = froms.getLength() > 0 ? froms.item(0).getTextContent().trim() : "";
         if (value.isEmpty())
         {
            utanFastVarde.add(targetRef);
         }
         else
         {
            aktivitetIds.add(value);
         }
      }
      if (!utanFastVarde.isEmpty())
      {
         throw new AssertionError("'" + AKTIVITET_ID_INPUT + "' in " + bpmnResource
               + " must be assigned a fixed aktivitets-ID, but is not for data inputs: " + String.join(", ", utanFastVarde));
      }
      return aktivitetIds;
   }

   private static Document parse(String bpmnResource)
   {
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      try (InputStream inputStream = classLoader.getResourceAsStream(bpmnResource))
      {
         if (inputStream == null)
         {
            throw new AssertionError("Could not find " + bpmnResource + " in the process resources");
         }
         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         factory.setNamespaceAware(true);
         factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
         factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
         return factory.newDocumentBuilder().parse(inputStream);
      }
      catch (IOException | ParserConfigurationException | SAXException e)
      {
         throw new AssertionError("Could not read " + bpmnResource + ": " + e.getMessage(), e);
      }
   }

   private static String childText(Element parent, String localName)
   {
      NodeList children = parent.getElementsByTagNameNS(BPMN_NAMESPACE, localName);
      return children.getLength() > 0 ? children.item(0).getTextContent().trim() : null;
   }

   private static String normalize(String aktivitetId)
   {
      return aktivitetId.trim().toLowerCase(Locale.ROOT);
   }
}
