/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */

package org.odpi.openmetadata.adapters.connectors.integration.strimzi;

import org.junit.jupiter.api.Test;
import org.odpi.openmetadata.accessservices.datamanager.metadataelements.ElementHeader;
import org.odpi.openmetadata.accessservices.datamanager.metadataelements.TopicElement;
import org.odpi.openmetadata.accessservices.datamanager.properties.TopicProperties;
import org.odpi.openmetadata.frameworks.connectors.ffdc.ConnectorCheckedException;
import org.odpi.openmetadata.frameworks.connectors.properties.ConnectionProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;


/**
 * KafkaMonitorIntegrationConnector catalogues active topics in a Strimzi broker.
 */
public class StrimziMonitorIntegrationConnectorTest
{

    public static final String[] EXPECTED_NAMES = new String[] {
            "kafka.vertriebskunde-services.agree-vertragsrollen",
            "kafka.vertriebskunde-services.kundenloeschung",
            "kafka.vertriebskunde-services.kundendaten-replikation",
            "kafka.vertriebskunde-services.kundenloeschung-vertriebskunde",
            "kafka.vertriebskunde-services.kundendaten-angereichert-compacted",
            "kafka.vertriebskunde-services.kundendaten-intake-compacted",
            "kafka.vertriebskunde-services.kundenproxy-pkfkregelwerk",
            "kafka.vertriebskunde-services.agree-pkfkregelwerk",
            "kafka.vertriebskunde-services.kundendaten-replikation-dlq",
            "kafka.vertriebskunde-services.kundenproxy-errors",
            "kafka.vertriebskunde-services.agree-personenrollen",
            "kafka.vertriebskunde-services.agree-kundendaten",
            "kafka.vertriebskunde-services.personenrollen",
            "kafka.vertriebskunde-services.dgraph-kundendaten-intake-compacted",
            "kafka.prefix-vertriebskunde-services.neo4j-kundendaten-intake-compacted",
            "kafka.vertriebskunde-services.kundenloeschung-agree21",
            "kafka.vertriebskunde-services.agree-kundendaten-compacted",
            "kafka.vertriebskunde-services.kundenproxy-kundendaten-compacted" };
    public static final Set<String> EXPECTED_NAMES_SET = new HashSet<>(Arrays.asList(EXPECTED_NAMES));


    
   @Test
   void testconvertStringToTopicMap() throws IOException, ConnectorCheckedException {
       String textPath = "src/test/resources/SampleGetResponse.json";
       Path path = Paths.get(textPath);
       String content = Files.readString(path);
       StrimziMonitorIntegrationConnector  conn = new StrimziMonitorIntegrationConnector();
       conn.setDescriptionAnnotationField("topic-description");

       Map<String, TopicProperties> map = conn.convertStringToTopicMap(content);
       assertEquals(18, map.size());

       for (String topicName:map.keySet()) {
           TopicProperties topicProperties = map.get(topicName);
           assertFalse(topicName.startsWith("__"));
           assertTrue(EXPECTED_NAMES_SET.contains(topicName), "This topic name is not excepted: " + topicName);
           Map<String,Object> extendedProperties = topicProperties.getExtendedProperties();
           if (extendedProperties !=null) {
               assertTrue(extendedProperties.get("partitions").equals(1));
               assertTrue(extendedProperties.get("replicas").equals(1));
           }
           assertFalse(topicProperties.getDescription() == null);
       }
       // test error
        conn = new StrimziMonitorIntegrationConnector();
        conn.setTargetURL("{}");
        try {
            conn.convertStringToTopicMap(content);
        } catch (ConnectorCheckedException cce) {
            String msg = cce.getMessage();
            // check that there are no unfilled inserts
            assertFalse(msg.contains("{"));
            assertFalse(msg.contains("}"));
       }
   }

    @Test
    void testTopixPrefix() throws IOException, ConnectorCheckedException {
        String textPath = "src/test/resources/SampleGetResponse.json";
        Path path = Paths.get(textPath);
        String content = Files.readString(path);
        StrimziMonitorIntegrationConnector conn = new StrimziMonitorIntegrationConnector();
        conn.setTopicNamePrefix("prefix");
        Map<String, TopicProperties> map = conn.convertStringToTopicMap(content);
        assertEquals(1, map.size());

        conn.setTopicNamePrefix("vertriebskunde-services");
        map = conn.convertStringToTopicMap(content);
        assertEquals(17, map.size());

        conn.setTopicNamePrefix("vertriebskunde-services,prefix");
        map = conn.convertStringToTopicMap(content);
        assertEquals(18, map.size());

        conn.setTopicNamePrefix("prefix");
        map = conn.convertStringToTopicMap(content);
        assertEquals(1, map.size());

    }

    @Test
    void testdetermineMutations() throws IOException, ConnectorCheckedException {
        String textPath = "src/test/resources/SampleGetResponse.json";
        Path path = Paths.get(textPath);
        String content = Files.readString(path);
        StrimziMonitorIntegrationConnector  conn = new StrimziMonitorIntegrationConnector();
        Map<String, TopicProperties> topicPropertiesMap = conn.convertStringToTopicMap(content);
        List<TopicElement> topicElementList = convertTopicPropertiesMapToTopicElementList(topicPropertiesMap);
        conn.determineMutations(topicElementList, topicPropertiesMap);
        assertTrue(conn.getUpdateTopicNameToGuidMap().keySet().isEmpty());
        assertTrue(conn.getDeleteTopicNameToGuidMap().keySet().isEmpty());
        assertTrue(conn.getAddTopicNamesSet().isEmpty());

        // test add
        List<TopicElement> topicElementListReduced = new ArrayList<>();
        topicElementListReduced.add(topicElementList.get(0));

        conn.determineMutations(topicElementListReduced, topicPropertiesMap);
        assertTrue(conn.getUpdateTopicNameToGuidMap().keySet().isEmpty());
        assertTrue(conn.getDeleteTopicNameToGuidMap().keySet().isEmpty());
        assertEquals(17, conn.getAddTopicNamesSet().size());

        // reset connection to clean
        conn = new StrimziMonitorIntegrationConnector();
        topicPropertiesMap = conn.convertStringToTopicMap(content);
        // test delete
        TopicElement newTopicElement = new TopicElement();
        TopicProperties properties = new TopicProperties();
        properties.setQualifiedName("aaa");
        properties.setDescription("bbb");

        Map<String, Object> extendedProperties = new HashMap<>();
        extendedProperties.put(StrimziMonitorIntegrationConnector.PARTITIONS,Integer.valueOf(10));
        extendedProperties.put(StrimziMonitorIntegrationConnector.REPLICAS,Integer.valueOf(20));
        properties.setExtendedProperties(extendedProperties);
        newTopicElement.setProperties(properties);
        ElementHeader elementHeader = new ElementHeader();
        elementHeader.setGUID("New guid");
        newTopicElement.setElementHeader(elementHeader);
        topicElementList.add(newTopicElement);

        conn.determineMutations(topicElementList, topicPropertiesMap);
        assertTrue(conn.getUpdateTopicNameToGuidMap().keySet().isEmpty());
        assertTrue(conn.getDeleteTopicNameToGuidMap().keySet().size() == 1);
        assertTrue(conn.getAddTopicNamesSet().isEmpty());

        // reset connection to clean
        conn = new StrimziMonitorIntegrationConnector();
        topicPropertiesMap = conn.convertStringToTopicMap(content);
        TopicProperties topicProperties = new TopicProperties();
        topicProperties.setDescription("bbb bbb");   //changed description
        topicProperties.setDisplayName("aaa");
        topicProperties.setQualifiedName("aaa");
        topicProperties.setExtendedProperties(extendedProperties);
        topicPropertiesMap.put("aaa", topicProperties);

        conn.determineMutations(topicElementList, topicPropertiesMap);
        assertTrue(conn.getUpdateTopicNameToGuidMap().keySet().size() == 1);
        assertTrue(conn.getDeleteTopicNameToGuidMap().keySet().isEmpty());
        assertTrue(conn.getAddTopicNamesSet().isEmpty());


    }

    private List<TopicElement> convertTopicPropertiesMapToTopicElementList(Map<String, TopicProperties> topicPropertiesMap) {
        List<TopicElement> topicElementList = new ArrayList<>();
        int guid= 1;
        for (String topicName:topicPropertiesMap.keySet()) {
           TopicElement topicElement = new TopicElement();
           TopicProperties topicProperties = topicPropertiesMap.get(topicName);
           topicElement.setProperties(topicProperties);
           ElementHeader elementHeader = new ElementHeader();
           elementHeader.setGUID("" + guid);
           guid++;
           topicElement.setElementHeader(elementHeader);
           topicElementList.add(topicElement);
       }
        return topicElementList;
    }
}
