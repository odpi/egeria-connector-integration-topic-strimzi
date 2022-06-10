/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */

package org.odpi.openmetadata.adapters.connectors.integration.strimzi;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.TrustStrategy;
import org.odpi.openmetadata.accessservices.datamanager.metadataelements.TopicElement;
import org.odpi.openmetadata.accessservices.datamanager.properties.TemplateProperties;
import org.odpi.openmetadata.accessservices.datamanager.properties.TopicProperties;
import org.odpi.openmetadata.adapters.connectors.integration.strimzi.ffdc.StrimziIntegrationConnectorAuditCode;
import org.odpi.openmetadata.adapters.connectors.integration.strimzi.ffdc.StrimziIntegrationConnectorErrorCode;
import org.odpi.openmetadata.frameworks.auditlog.messagesets.AuditLogMessageDefinition;
import org.odpi.openmetadata.frameworks.connectors.ffdc.ConnectorCheckedException;
import org.odpi.openmetadata.frameworks.connectors.ffdc.InvalidParameterException;
import org.odpi.openmetadata.frameworks.connectors.ffdc.PropertyServerException;
import org.odpi.openmetadata.frameworks.connectors.ffdc.UserNotAuthorizedException;
import org.odpi.openmetadata.frameworks.connectors.properties.EndpointProperties;
import org.odpi.openmetadata.integrationservices.topic.connector.TopicIntegratorConnector;
import org.odpi.openmetadata.integrationservices.topic.connector.TopicIntegratorContext;
import org.odpi.openmetadata.frameworks.auditlog.messagesets.ExceptionMessageDefinition;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;

import static org.odpi.openmetadata.adapters.connectors.integration.strimzi.StrimziMonitorIntegrationProvider.DESCRIPTION_ANNOTATION_FIELD;


/**
 * StrimziMonitorIntegrationConnector catalogues active topics in a Strimzi broker.
 * <p>
 * The topics include the qualified name, description, partitions and replicas.
 */
public class StrimziMonitorIntegrationConnector extends TopicIntegratorConnector {
    public static final String REPLICAS = "replicas";
    public static final String PARTITIONS = "partitions";

    private String templateQualifiedName = null;
    private String token = null;
    private String topicNamePrefix = null;
    private String templateGUID = null;
    private String targetURL = null;
    private Set<String> addTopicNamesSet = new HashSet<>();
    private Map<String, String> deleteTopicNameToGuidMap = new HashMap<>();
    private Map<String, String> updateTopicNameToGuidMap = new HashMap<>();

    private List<TopicElement> cataloguedTopics;

    private TopicIntegratorContext myContext = null;

    public void setDescriptionAnnotationField(Object descriptionAnnotationField) {
        this.descriptionAnnotationField = descriptionAnnotationField;
    }

    private Object descriptionAnnotationField;

    /**
     * Indicates that the connector is completely configured and can begin processing.
     * This call can be used to register with non-blocking services.
     *
     * @throws ConnectorCheckedException there is a problem within the connector.
     */
    @Override
    public synchronized void start() throws ConnectorCheckedException {
        super.start();

        final String methodName = "start";

        myContext = super.getContext();

        if (connectionProperties != null) {

            descriptionAnnotationField = connectionProperties.getConfigurationProperties().get(StrimziMonitorIntegrationProvider.DESCRIPTION_ANNOTATION_FIELD);
            EndpointProperties endpoint = connectionProperties.getEndpoint();

            if (endpoint != null) {
                targetURL = endpoint.getAddress();
                try {
                    new URI(targetURL);
                } catch (URISyntaxException e) {
                    throwException(StrimziIntegrationConnectorErrorCode.INVALID_URL_IN_CONFIGURATION,"Endpoint address" );
                }

            }
            Map<String, Object> configurationProperties = connectionProperties.getConfigurationProperties();
            templateQualifiedName = (String) configurationProperties.get(StrimziMonitorIntegrationProvider.TEMPLATE_QUALIFIED_NAME_CONFIGURATION_PROPERTY);
            // TODO check that this exists - manditory ???
            token = (String) configurationProperties.get(StrimziMonitorIntegrationProvider.TOKEN_PROPERTY);
            topicNamePrefix = (String) configurationProperties.get(StrimziMonitorIntegrationProvider.TOPIC_NAME_PREFIX);
            /*
             * Record the configuration
             */
            if (auditLog != null) {
                // do not record the token in the log which could be sensitive
                auditLog.logMessage(methodName,
                                    StrimziIntegrationConnectorAuditCode.CONNECTOR_CONFIGURATION.getMessageDefinition(connectorName,
                                                                                                                      targetURL,
                                                                                                                      templateQualifiedName
                                                                                                                     ));
            }
        } else {
            if (auditLog != null) {
                auditLog.logMessage(methodName,
                                    StrimziIntegrationConnectorAuditCode.NO_CONNECTION_PROPERTIES.getMessageDefinition(connectorName,
                                                                                                                       targetURL));
            }
            throwException(StrimziIntegrationConnectorErrorCode.NO_CONNECTION_CONFIGURATION, StrimziMonitorIntegrationProvider.TOKEN_PROPERTY);
        }



        /*
         * Retrieve the template if one has been requested
         */
        if (templateQualifiedName != null) {
            try {
                List<TopicElement> templateElements = myContext.getTopicsByName(templateQualifiedName, 0, 0);

                if (templateElements != null) {
                    for (TopicElement templateElement : templateElements) {
                        String qualifiedName = templateElement.getProperties().getQualifiedName();

                        if (templateQualifiedName.equals(qualifiedName)) {
                            templateGUID = templateElement.getElementHeader().getGUID();
                        }
                    }
                }
            } catch (Exception error) {
                if (auditLog != null) {
                    auditLog.logException(methodName,
                                          StrimziIntegrationConnectorAuditCode.MISSING_TEMPLATE.getMessageDefinition(connectorName, templateQualifiedName),
                                          error);
                }

            }
        }
    }

    /**
     * Throw an exception based on the supplied error code.
     *
     * @param errorCode    error code describing the problem
     * @param propertyName property name
     */
    private void throwException(StrimziIntegrationConnectorErrorCode errorCode,
                                String propertyName) throws ConnectorCheckedException {
        final String methodName = "throwException";

        ExceptionMessageDefinition messageDefinition = errorCode.getMessageDefinition(propertyName);
        // create a subclass
        ConnectorCheckedException error = new ConnectorCheckedException(messageDefinition,
                                                                        this.getClass().getName(),
                                                                        methodName);

        if (auditLog != null) {
            AuditLogMessageDefinition auditLogMessageDefinitionMessage =
                    StrimziIntegrationConnectorAuditCode.BAD_CONFIGURATION.getMessageDefinition(connectorName,
                                                                                                "Configuration error", targetURL, methodName, error.getMessage());

            auditLog.logException(methodName,
                                  auditLogMessageDefinitionMessage,
                                  error);
        }

        throw error;
    }


    /**
     * Requests that the connector does a comparison of the metadata in the third party technology and open metadata repositories.
     * Refresh is called when the integration connector first starts and then at intervals defined in the connector's configuration
     * as well as any external REST API calls to explicitly refresh the connector.
     * <p>
     * This method performs two sweeps.  It first retrieves the files in the directory and validates that are in the
     * catalog - adding or updating them if necessary.  The second sweep is to ensure that all of the assets catalogued
     * in this directory actually exist on the file system.
     *
     * @throws ConnectorCheckedException there is a problem with the connector.  It is not able to refresh the metadata.
     */
    @Override
    public void refresh() throws ConnectorCheckedException {
        final String methodName = "refresh";
        if (auditLog != null) {
            auditLog.logMessage(methodName,
                                StrimziIntegrationConnectorAuditCode.REFRESH_CALLED.getMessageDefinition(connectorName));
        }
        //clear out the maps
        updateTopicNameToGuidMap = new HashMap<>();
        deleteTopicNameToGuidMap  = new HashMap<>();
        // clear the set
        addTopicNamesSet = new HashSet<>();
        try {
            /*
             * Retrieve the list of active topics from Strimzi.
             */
            Map<String, TopicProperties> strimziTopicElements = getStrimziTopicElements();

            /*
             * Retrieve the topics that are catalogued for this event broker.
             * Remove the topics from the catalog that are no longer present in the event broker.
             * Remove the names of the topics that are cataloged from the active topic names.
             * At the end of this loop, the active topic names will just contain the names of the
             * topics that are not catalogued.
             */
            int startFrom = 0;
            cataloguedTopics = myContext.getMyTopics(startFrom, 0);

            // populate the delete map the update map and the add set
            determineMutations(cataloguedTopics, strimziTopicElements);

            Set<String> updateTopicNames = updateTopicNameToGuidMap.keySet();
            /*
             * Update topics to the catalog.
             */
            for (String topicName : updateTopicNames) {
                TopicProperties topicProperties = strimziTopicElements.get(topicName);
                // Assume not a merge update.
                myContext.updateTopic(updateTopicNameToGuidMap.get(topicName), false, topicProperties);
            }
            Set<String> deleteTopicNames = deleteTopicNameToGuidMap.keySet();
            /*
             * Delete topics to the catalog.
             */
            for (String topicName : deleteTopicNames) {
                myContext.removeTopic(deleteTopicNameToGuidMap.get(topicName), topicName);
            }
            /*
             * Add topics
             */
            for (String topicName : addTopicNamesSet) {
                String topicGUID;
                if (templateGUID == null) {

                    TopicProperties topicProperties = strimziTopicElements.get(topicName);
                    topicGUID = myContext.createTopic(topicProperties);

                    if (topicGUID != null) {
                        if (auditLog != null) {
                            auditLog.logMessage(methodName,
                                                StrimziIntegrationConnectorAuditCode.TOPIC_CREATED.getMessageDefinition(connectorName,
                                                                                                                        topicName,
                                                                                                                        topicGUID));
                        }
                    }
                } else {
                    TemplateProperties templateProperties = new TemplateProperties();

                    templateProperties.setQualifiedName(topicName);
                    topicGUID = myContext.createTopicFromTemplate(templateGUID, templateProperties);
                    if (topicGUID != null) {
                        if (auditLog != null) {
                            auditLog.logMessage(methodName,
                                                StrimziIntegrationConnectorAuditCode.TOPIC_CREATED_FROM_TEMPLATE.getMessageDefinition(connectorName,
                                                                                                                                      topicName,
                                                                                                                                      topicGUID,
                                                                                                                                      templateQualifiedName,
                                                                                                                                      templateGUID));
                        }
                    }
                }
            }
        } catch (Exception error) {
            if (auditLog != null) {
                auditLog.logException(methodName,
                                      StrimziIntegrationConnectorAuditCode.UNABLE_TO_RETRIEVE_TOPICS.getMessageDefinition(connectorName,
                                                                                                                          "localhost:9092",
                                                                                                                          error.getClass().getName(),
                                                                                                                          error.getMessage()),
                                      error);


            }

            throw new ConnectorCheckedException(StrimziIntegrationConnectorErrorCode.UNEXPECTED_EXCEPTION.getMessageDefinition(connectorName,
                                                                                                                               error.getClass().getName(),
                                                                                                                               error.getMessage()),
                                                this.getClass().getName(),
                                                methodName,
                                                error);
        }
    }

    void determineMutations(List<TopicElement> cataloguedTopics, Map<String, TopicProperties> strimziTopicElements) {
        // existing strimzi topics
        Set<String> strimziTopicNames;
        Map<String, TopicElement> cataloguedTopicMap = new HashMap<>();
        if (cataloguedTopics != null) {
            for (TopicElement topicElement : cataloguedTopics) {
                String topicName = topicElement.getProperties().getQualifiedName();
                // restrict to the topic names we care about.
                if (includeTopicBasedOnName(topicName)) {
                    cataloguedTopicMap.put(topicName, topicElement);
                }
            }

            /*
             * Loop through catalogued topics to decide whether to update or delete by populating the maps.
             * The delete and update maps need the Egeria guid to be able to action the mutation.
             */
            if (cataloguedTopics.size() > 0) {
                // uncomment and implement more code if we need to consider paging
                // startFrom = startFrom + cataloguedTopics.size();
                if (strimziTopicElements == null) {
                    strimziTopicNames = new HashSet<>();
                } else {
                    strimziTopicNames = strimziTopicElements.keySet();
                }
                for (TopicElement cataloguedTopic : cataloguedTopics) {
                    String cataloguedTopicName = cataloguedTopic.getProperties().getQualifiedName();
                    String cataloguedEgeriaTopicGUID = cataloguedTopic.getElementHeader().getGUID();

                    if (!strimziTopicNames.contains(cataloguedTopicName)) {
                        /*
                         * The topic no longer exists so delete it from the catalog.
                         */

                        deleteTopicNameToGuidMap.put(cataloguedTopicName, cataloguedEgeriaTopicGUID);
                    } else {
                        // we have 2 topics of the same name in Strimzi and Egeria
                        if (updateRequired(strimziTopicElements.get(cataloguedTopicName), cataloguedTopicMap.get(cataloguedTopicName))) {
                            updateTopicNameToGuidMap.put(cataloguedTopicName, cataloguedEgeriaTopicGUID);
                        }
                    }
                }
            }
        }
        /*
         * loop through Strimzi topics to determine what we need to add. The add is made without a guid, as the guid
         * does not exist yet.
         */
        if (strimziTopicElements != null && strimziTopicElements.size() > 0) {
            strimziTopicNames = strimziTopicElements.keySet();
            for (String strimziTopicName : strimziTopicNames) {
                Set<String> cataloguedTopicNames = cataloguedTopicMap.keySet();
                if (!cataloguedTopicNames.contains(strimziTopicName)) {
                    addTopicNamesSet.add(strimziTopicName);
                }
            }
        }
    }

    /**
     * Determine is an update is required.
     * @param strimziTopicProperties strimzi topic information
     * @param cataloguedTopicElement cataloged topic information
     * @return true if update required
     */
    boolean updateRequired(TopicProperties strimziTopicProperties, TopicElement cataloguedTopicElement) {
        boolean doUpdate = !Objects.equals(strimziTopicProperties.getDescription(), cataloguedTopicElement.getProperties().getDescription());


        Map<String, Object> strimziExtendedProperties = strimziTopicProperties.getExtendedProperties();
        Map<String, Object> cataloguedExtendedProperties = cataloguedTopicElement.getProperties().getExtendedProperties();
        if (strimziExtendedProperties != null && cataloguedExtendedProperties != null) {
            if (!Objects.equals(strimziExtendedProperties.get(REPLICAS),
                                cataloguedExtendedProperties.get(REPLICAS))) {
                doUpdate = true;
            }
            if (!Objects.equals(strimziExtendedProperties.get(PARTITIONS),
                                cataloguedExtendedProperties.get(PARTITIONS))) {
                doUpdate = true;
            }
        } else if (strimziExtendedProperties != null) {
            if (strimziExtendedProperties.get(REPLICAS) != null) {
                doUpdate = true;
            }
            if (strimziExtendedProperties.get(PARTITIONS) != null) {
                doUpdate = true;
            }
        } else if (cataloguedExtendedProperties != null) {
            if (cataloguedExtendedProperties.get(REPLICAS) != null) {
                doUpdate = true;
            }
            if (cataloguedExtendedProperties.get(PARTITIONS) != null) {
                doUpdate = true;
            }
        }
        return doUpdate;
    }

    private void deleteFromContext(String methodName, String cataloguedTopicName, String cataloguedEgeriaTopicGUID) throws InvalidParameterException, UserNotAuthorizedException, PropertyServerException {
        myContext.removeTopic(cataloguedEgeriaTopicGUID, cataloguedTopicName);

        if (auditLog != null) {
            auditLog.logMessage(methodName,
                                StrimziIntegrationConnectorAuditCode.TOPIC_DELETED.getMessageDefinition(connectorName,
                                                                                                        cataloguedTopicName,
                                                                                                        cataloguedEgeriaTopicGUID));
        }
    }


    public RestTemplate restTemplate()
    throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

        SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();

        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(csf)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory();

        requestFactory.setHttpClient(httpClient);
        return new RestTemplate(requestFactory);
    }

    /**
     * Issue rest call to Strimzi, the url is obtained from the config.
     * Return map with the key of topic name with the topic properties as the value
     * The topicProperties should contain the qualifiedName as the topic name , the topic description and
     * the additional properties.
     * Probably should pass the additional properties through as configuration parameters.
     *
     * @return map with the key of topic name with the topic properties as the value.
     */
    private Map<String, TopicProperties> getStrimziTopicElements() throws ConnectorCheckedException {
        String methodName = "getStrimziTopicElements";
        // set authentication
        HttpHeaders authHeaders = new HttpHeaders();

        authHeaders.setContentType(MediaType.APPLICATION_JSON);
        authHeaders.add("Authorization", "Bearer " + token);

        HttpEntity<?> request = new HttpEntity<>(authHeaders);
        RestTemplate restTemplate;

        try {
            restTemplate = restTemplate();
        } catch (Exception error) {
            throw new ConnectorCheckedException(StrimziIntegrationConnectorErrorCode.ERROR_ON_STRIMZI_REST_CALL.getMessageDefinition(connectorName,
                                                                                                                                      targetURL, error.getClass().getName(), error.getMessage()),
                                                this.getClass().getName(),
                                                methodName,
                                                error);
        }
        ResponseEntity<String> responseEntity = restTemplate.exchange(targetURL, HttpMethod.GET, request, String.class);

        String jsonString = responseEntity.getBody();

        //System.err.println("Rest call got this response " + jsonString);

        return convertStringToTopicMap(jsonString);

    }

    public Map<String, TopicProperties> convertStringToTopicMap(String jsonString) throws ConnectorCheckedException {
        String methodName = "convertStringToTopicMap";
        // a map of topicProperties keys by topic name for easy retrieval.
        Map<String, TopicProperties> topicMap = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();

        JsonNode root;
        try {
            root = mapper.readTree(jsonString);
        } catch (JsonProcessingException error) {
            throw new ConnectorCheckedException(StrimziIntegrationConnectorErrorCode.ERROR_PARSING_REST_RESPONSE.getMessageDefinition(connectorName,
                                                                                                                                      targetURL, error.getClass().getName(), error.getMessage()),
                                                this.getClass().getName(),
                                                methodName,
                                                error);
        }
        JsonNode items = root.path("items");
        if (items.isArray()) {
            for (JsonNode node : items) {
                String topicName = null;
                String statusTopicName = null;
                String description = null;
                Integer partitions = null;
                Integer replicas = null;
                JsonNode statusNode = node.path("status");
                if (statusNode.isObject()) {
                    statusTopicName = statusNode.path("topicName").asText();
                }
                JsonNode metadataNode = node.path("metadata");
                if (metadataNode.isObject()) {
                    topicName = metadataNode.path("name").asText();

                }
                if (includeBasedOnStatusTopicName(statusTopicName) && includeTopicBasedOnName(topicName)) {
                    JsonNode specNode = node.path("spec");
                    if (specNode.isObject()) {
                        try {
                            partitions = Integer.parseInt(String.valueOf(specNode.path(PARTITIONS)));
                        } catch (NumberFormatException nfe) {
                            // catch the exception and leave the variable as null so it will not be included in the topic properties
                        }
                        try {
                        replicas = Integer.parseInt(String.valueOf(specNode.path(REPLICAS)));
                        } catch (NumberFormatException nfe) {
                            // catch the exception and leave the variable as null so it will not be included in the topic properties
                        }
                    }

//                    JsonNode metadataNode
//                    = node.path("metadata");
                    if (metadataNode.isObject()) {
                        JsonNode annotationsNode = metadataNode.path("annotations");
                        if( descriptionAnnotationField != null) {
                            description = annotationsNode.path(descriptionAnnotationField.toString()).asText();
                            if( description == null || description == "" ) {
                                description = getDefaultDescription(topicName);
                            }
                        } else {
                            description = getDefaultDescription(topicName);
                        }
//                        description = annotationsNode.path("topic.description").asText();
                    }

                    TopicProperties topicProperties = new TopicProperties();

                    topicProperties.setDescription(description);
                    topicProperties.setQualifiedName(topicName);
                    topicProperties.setDisplayName(topicName);
                    // specify the type name
                    topicProperties.setTypeName("KafkaTopic");
                    // the KafkaTopic has the attributes maximumPartitions and maximumReplicas
                    // KafkaTopic extends the Topic type, these attributes need to be sent through as extendedProperties
                    Map<String, Object> extendedProperties = new HashMap<>();
                    if (partitions != null) {
                        extendedProperties.put(PARTITIONS, partitions);
                    }
                    if (replicas != null) {
                        extendedProperties.put(REPLICAS, replicas);
                    }
                    topicProperties.setExtendedProperties(extendedProperties);
                    topicMap.put(topicName, topicProperties);
                }
            }
        }
        if (auditLog != null) {
            auditLog.logMessage(methodName,
                                StrimziIntegrationConnectorAuditCode.RETRIEVED_TOPICS.getMessageDefinition(connectorName,
                                                                                                           Integer.toString(topicMap.size()),
                                                                                                           targetURL));
        }
        return topicMap;
    }

    /**
     * Provide a default description for the given Topic.
     *
     * @param topicName to retrieve the default description for
     * @return the default description
     */
    private String getDefaultDescription(String topicName) {
        return String.format("No description available for the topic '%s'.", topicName);
    }

    private boolean includeBasedOnStatusTopicName(String statusTopicName) {
        boolean include = true;
        if (statusTopicName != null && statusTopicName.startsWith("__")) {
            include = false;
        }
        return include;
    }

    /**
     * Include this topic name only if it is not null, not empty, and starts with the
     * requested prefix if there is one.
     *
     * @param topicName to check
     * @return whether to include this topicName
     */
    private boolean includeTopicBasedOnName(String topicName) {
        boolean include = false;
        if (topicName != null) {
            if (topicNamePrefix == null || topicName.length() == 0) {
                include = true;
            } else if (topicName.startsWith(topicNamePrefix)) {
                include = true;
            }
        }
        return include;
    }


    /**
     * Shutdown Strimzi monitoring
     *
     * @throws ConnectorCheckedException something failed in the super class
     */
    @Override
    public synchronized void disconnect() throws ConnectorCheckedException {
        final String methodName = "disconnect";


        if (auditLog != null) {
            auditLog.logMessage(methodName,
                                StrimziIntegrationConnectorAuditCode.CONNECTOR_STOPPING.getMessageDefinition(connectorName));
        }

        super.disconnect();
    }

    /**
     * Used for testing
     */
    void setTargetURL(String targetURL) {
        this.targetURL = targetURL;
    }
    Set<String> getaddTopicNamesSet() {
        return addTopicNamesSet;
    }
    Map<String, String> getdeleteTopicNameToGuidMap() {
        return deleteTopicNameToGuidMap;
    }
    Map<String, String> getupdateTopicNameToGuidMap() {
        return updateTopicNameToGuidMap;
    }

}
