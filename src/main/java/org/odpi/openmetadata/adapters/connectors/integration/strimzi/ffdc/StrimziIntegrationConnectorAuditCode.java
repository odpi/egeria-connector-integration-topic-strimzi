/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.adapters.connectors.integration.strimzi.ffdc;

import org.odpi.openmetadata.frameworks.auditlog.messagesets.AuditLogMessageDefinition;
import org.odpi.openmetadata.frameworks.auditlog.messagesets.AuditLogMessageSet;
import org.odpi.openmetadata.repositoryservices.auditlog.OMRSAuditLogRecordSeverity;


/**
 * The StrimziIntegrationConnectorAuditCode is used to define the message content for the OMRS Audit Log.
 *
 * The 5 fields in the enum are:
 * <ul>
 *     <li>Log Message Id - to uniquely identify the message</li>
 *     <li>Severity - is this an event, decision, action, error or exception</li>
 *     <li>Log Message Text - includes placeholder to allow additional values to be captured</li>
 *     <li>Additional Information - further parameters and data relating to the audit message (optional)</li>
 *     <li>SystemAction - describes the result of the situation</li>
 *     <li>UserAction - describes how a user should correct the situation</li>
 * </ul>
 */
public enum StrimziIntegrationConnectorAuditCode implements AuditLogMessageSet
{
    CONNECTOR_CONFIGURATION("STRIMZI-INTEGRATION-CONNECTOR-0001",
                          OMRSAuditLogRecordSeverity.INFO,
                          "The {0} integration connector has been initialized to monitor event broker at URL {1}",
                          "The connector is designed to monitor changes to the topics managed by the event broker.",
                          "No specific action is required.  This message is to confirm the configuration for the integration connector."),

    BAD_CONFIGURATION("STRIMZI-INTEGRATION-CONNECTOR-0002",
                          OMRSAuditLogRecordSeverity.EXCEPTION,
                          "The {0} integration connector encountered an {1} exception when event broker at URL {1} during the {3} method.  The exception message included was {4}",
                          "The exception is passed back to the Topic Integrator OMIS in the integration daemon that is hosting " +
                                  "this connector to enable it to perform error handling.  More messages are likely to follow describing the " +
                                  "error handling that was performed.  These can help to determine how to recover from this error",
                          "This message contains the exception that was the original cause of the problem. Use the information from the " +
                                  "exception stack trace to determine why the connector is not able to access the event broker and resolve that issue.  " +
                                  "Use the messages that where subsequently logged during the error handling to discover how to restart the " +
                                  "connector in the integration daemon once the original cause of the error has been corrected."),

    UNABLE_TO_RETRIEVE_TOPICS("STRIMZI-INTEGRATION-CONNECTOR-0003",
                            OMRSAuditLogRecordSeverity.EXCEPTION,
                            "The {0} integration connector received an unexpected {2} exception when retrieving topics from event broker {1}.  The error message was {3}",
                                     "The exception is returned to the integration daemon that is hosting this connector to enable it to perform error handling.",
                                     "Use the message in the nested exception to determine the root cause of the error. Once this is " +
                                             "resolved, follow the instructions in the messages produced by the integration daemon to restart this connector."),

    RETRIEVED_TOPICS("STRIMZI-INTEGRATION-CONNECTOR-0004",
                              OMRSAuditLogRecordSeverity.INFO,
                              "The {0} integration connector has retrieved {1} topics from {3}. {2} topics are matching the topic prefix.",
                              "The connector will maintain these topics as assets.",
                              "No action is required unless there are errors that follow indicating that the topics can not be maintained."),

    CONNECTOR_STOPPING("STRIMZI-INTEGRATION-CONNECTOR-0009",
                                  OMRSAuditLogRecordSeverity.INFO,
                                  "The {0} integration connector has stopped its topic monitoring and is shutting down",
                                  "The connector is disconnecting.",
                                  "No action is required unless there are errors that follow indicating that there were problems shutting down."),

    BAD_TOPIC_ELEMENT("STRIMZI-INTEGRATION-CONNECTOR-0013",
                       OMRSAuditLogRecordSeverity.ERROR,
                       "The {0} integration connector retrieved an incomplete Topic asset: {1}",
                       "The metadata element for the topic that was retrieved from the open metadata repositories has missing " +
                               "information.  This is likely to be a logic error in the Topic Integrator OMIS or Data Manager OMAS.",
                       "Look for errors in the audit logs for the integration daemon where the connector and Topic Integrator OMIS are " +
                               "running and the metadata server where the Data Manager OMAS is running.  Collect these diagnostics and " +
                               "ask the Egeria community for help to determine why the Topic element is incomplete."),

    UNEXPECTED_EXC_TOPIC_UPDATE("STRIMZI-INTEGRATION-CONNECTOR-0014",
                                 OMRSAuditLogRecordSeverity.EXCEPTION,
                                 "An unexpected {0} exception was returned to the {1} integration connector when it tried to update the " +
                                         "Topic in the metadata repositories for topic {2}.  The error message was {3}",
                                 "The exception is logged and the integration connector continues to synchronize metadata.  " +
                                         "This topic is not catalogued at this time but may succeed later.",
                                 "Use the message in the unexpected exception to determine the root cause of the error and fix it."),

    TOPIC_CREATED("STRIMZI-INTEGRATION-CONNECTOR-0016",
                      OMRSAuditLogRecordSeverity.INFO,
                     "The {0} integration connector created the Topic {1} ({2}) for a new real-world topic",
                     "The connector created the Topic as part of its monitoring of the topics in the event broker.",
                     "No action is required.  This message is to record the reason why the Topic was created."),

    TOPIC_UPDATED("STRIMZI-INTEGRATION-CONNECTOR-0018",
                      OMRSAuditLogRecordSeverity.INFO,
                      "The {0} integration connector has updated the Topic {1} ({2}) because the real-world topic changed",
                      "The connector updated the Topic as part of its monitoring of the topics in the event broker.",
                      "No action is required.  This message is to record the reason why the Topic was updated."),

    TOPIC_DELETED("STRIMZI-INTEGRATION-CONNECTOR-0019",
                      OMRSAuditLogRecordSeverity.INFO,
                      "The {0} integration connector has deleted the Topic {1} ({2}) because the real-world topic is no longer defined in the event broker",
                      "The connector removed the Topic as part of its monitoring of the topics in the event broker.",
                      "No action is required.  This message is to record the reason why the Topic was removed."),

    NO_CONNECTION_PROPERTIES("STRIMZI-INTEGRATION-CONNECTOR-0020",
                      OMRSAuditLogRecordSeverity.ERROR,
                             "The {0} integration connector has been initialized to monitor event broker at URL {1} but there is no configuration properties supplied",
                             "The connector is designed to monitor changes to the topics managed by the event broker.",
                             "No specific action is required.  This message is to confirm the configuration for the integration connector."),
    REFRESH_CALLED("STRIMZI-INTEGRATION-CONNECTOR-0021",
                             OMRSAuditLogRecordSeverity.INFO,
                             "The {0} integration connector refresh has been called",
                             "This is normal behaviour for a polling integration connector",
                             "No specific action is required.  This message is to inform that polling is occurring."),


    ;

    private String                     logMessageId;
    private OMRSAuditLogRecordSeverity severity;
    private String                     logMessage;
    private String                     systemAction;
    private String                     userAction;


    /**
     * The constructor for StrimziIntegrationConnectorAuditCode expects to be passed one of the enumeration rows defined in
     * StrimziIntegrationConnectorAuditCode above.   For example:
     *
     *     StrimziIntegrationConnectorAuditCode   auditCode = StrimziIntegrationConnectorAuditCode.SERVER_NOT_AVAILABLE;
     *
     * This will expand out to the 4 parameters shown below.
     *
     * @param messageId - unique Id for the message
     * @param severity - severity of the message
     * @param message - text for the message
     * @param systemAction - description of the action taken by the system when the condition happened
     * @param userAction - instructions for resolving the situation, if any
     */
    StrimziIntegrationConnectorAuditCode(String                     messageId,
                                             OMRSAuditLogRecordSeverity severity,
                                             String                     message,
                                             String                     systemAction,
                                             String                     userAction)
    {
        this.logMessageId = messageId;
        this.severity = severity;
        this.logMessage = message;
        this.systemAction = systemAction;
        this.userAction = userAction;
    }


    /**
     * Retrieve a message definition object for logging.  This method is used when there are no message inserts.
     *
     * @return message definition object.
     */
    @Override
    public AuditLogMessageDefinition getMessageDefinition()
    {
        return new AuditLogMessageDefinition(logMessageId,
                                             severity,
                                             logMessage,
                                             systemAction,
                                             userAction);
    }


    /**
     * Retrieve a message definition object for logging.  This method is used when there are values to be inserted into the message.
     *
     * @param params array of parameters (all strings).  They are inserted into the message according to the numbering in the message text.
     * @return message definition object.
     */
    @Override
    public AuditLogMessageDefinition getMessageDefinition(String ...params)
    {
        AuditLogMessageDefinition messageDefinition = new AuditLogMessageDefinition(logMessageId,
                                                                                    severity,
                                                                                    logMessage,
                                                                                    systemAction,
                                                                                    userAction);
        messageDefinition.setMessageParameters(params);
        return messageDefinition;
    }


    /**
     * JSON-style toString
     *
     * @return string of property names and values for this enum
     */
    @Override
    public String toString()
    {
        return "StrimziIntegrationConnectorAuditCode{" +
                "logMessageId='" + logMessageId + '\'' +
                ", severity=" + severity +
                ", logMessage='" + logMessage + '\'' +
                ", systemAction='" + systemAction + '\'' +
                ", userAction='" + userAction + '\'' +
                '}';
    }
}
