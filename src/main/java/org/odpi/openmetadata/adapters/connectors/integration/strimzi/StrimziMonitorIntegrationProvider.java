/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */

package org.odpi.openmetadata.adapters.connectors.integration.strimzi;

import org.odpi.openmetadata.frameworks.connectors.ConnectorProviderBase;
import org.odpi.openmetadata.frameworks.connectors.properties.beans.ConnectorType;

import java.util.ArrayList;
import java.util.List;


/**
 * StrimziMonitorIntegrationProvider is the connector provider for the kafka integration connector that extracts topic names from the broker.
 */
public class StrimziMonitorIntegrationProvider extends ConnectorProviderBase
{
    private static final String connectorTypeGUID          = "1a18d893-ea0d-4615-82b9-b4860e53af05";
    private static final String connectorTypeQualifiedName = "Strimzi Monitor Integration Connector";
    private static final String connectorTypeDisplayName   = "Strimzi Monitor Integration Connector";
    private static final String connectorTypeDescription   = "Connector maintains a list of Strimzi Topic assets associated with an event broker.";

    //TODO do we need this ?
    static final String TEMPLATE_QUALIFIED_NAME_CONFIGURATION_PROPERTY = "templateQualifiedName";
    static final String TOKEN_PROPERTY = "token";
    static final String TOPIC_NAME_PREFIX = "topicNamePrefix";

    /**
     * Constructor used to initialize the ConnectorProvider with the Java class name of the specific
     * store implementation.
     */
    public StrimziMonitorIntegrationProvider()
    {
        super();

        super.setConnectorClassName(StrimziMonitorIntegrationConnector.class.getName());

        ConnectorType connectorType = new ConnectorType();
        connectorType.setType(ConnectorType.getConnectorTypeType());
        connectorType.setGUID(connectorTypeGUID);
        connectorType.setQualifiedName(connectorTypeQualifiedName);
        connectorType.setDisplayName(connectorTypeDisplayName);
        connectorType.setDescription(connectorTypeDescription);
        connectorType.setConnectorProviderClassName(this.getClass().getName());

        List<String> recognizedConfigurationProperties = new ArrayList<>();
        recognizedConfigurationProperties.add(TEMPLATE_QUALIFIED_NAME_CONFIGURATION_PROPERTY);
        recognizedConfigurationProperties.add(TOKEN_PROPERTY);
        recognizedConfigurationProperties.add(TOPIC_NAME_PREFIX);

        connectorType.setRecognizedConfigurationProperties(recognizedConfigurationProperties);

        super.connectorTypeBean = connectorType;
    }
}
