/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.adapters.connectors.integration.strimzi.ffdc;

import org.odpi.openmetadata.frameworks.auditlog.messagesets.ExceptionMessageDefinition;
import org.odpi.openmetadata.frameworks.auditlog.messagesets.ExceptionMessageSet;
import org.odpi.openmetadata.repositoryservices.auditlog.OMRSAuditLogRecordSeverity;

/**
 * The StrimziIntegrationConnectorErrorCode is used to define first failure data capture (FFDC) for errors that occur when working with
 * the Strimzi monitor integration connector.  It is used in conjunction with both Checked and Runtime (unchecked) exceptions.
 *
 * The 5 fields in the enum are:
 * <ul>
 *     <li>HTTP Error Code - for translating between REST and JAVA - Typically the numbers used are:</li>
 *     <li><ul>
 *         <li>500 - internal error</li>
 *         <li>400 - invalid parameters</li>
 *         <li>404 - not found</li>
 *         <li>409 - data conflict errors - eg item already defined</li>
 *     </ul></li>
 *     <li>Error Message Id - to uniquely identify the message</li>
 *     <li>Error Message Text - includes placeholder to allow additional values to be captured</li>
 *     <li>SystemAction - describes the result of the error</li>
 *     <li>UserAction - describes how a consumer should correct the error</li>
 * </ul>
 */
public enum StrimziIntegrationConnectorErrorCode implements ExceptionMessageSet
{
    ERROR_PARSING_REST_RESPONSE(400, "STRIMZI-INTEGRATION-CONNECTOR-400-001",
                         "The {0} integration connector got a {1} Exception, for rest target URL {2} when then processing the json in the rest response, The message was: {3}",
                         "The connector is unable to catalog one or more topics.",
                         "Use the details from the error message to determine the cause of the error and retry the request once it is resolved."),

    NO_CONNECTION_CONFIGURATION(400, "STRIMZI-INTEGRATION-CONNECTOR-400-002",
                             "The {0} integration connector has been initialized to monitor event broker at URL {1} but there is no configuration properties supplied",
                             "The connector requires the authenication token (property name is token) to be specified in the connection properties.",
                             "Supply the appropriate connection properties including the authentication token."),

    ERROR_ON_STRIMZI_REST_CALL(400, "STRIMZI-INTEGRATION-CONNECTOR-400-003",
                               "The {0} integration connector got a {1} Exception, for rest target URL {2}, The message was: {3}",
                               "The connector is unable to catalog one or more topics as the rest call to Strimzi failed.",
                               "Use the details from the rest failure error message, to amend the endpoint and token to match the running Strimzi system and retry."),
    INVALID_URL_IN_CONFIGURATION(400, "STRIMZI-INTEGRATION-CONNECTOR-400-004",
                                "The {0} integration connector has been initialized to monitor event broker at URL {1} but the endpoint address is not a valid URL",
                                "The connector requires the a valid endpoint address to be specified in the connection properties.",
                                "Supply a well formed endpoint address as a connection properties ."),

    UNEXPECTED_EXCEPTION(500, "STRIMZI-INTEGRATION-CONNECTOR-500-001",
             "The {0} integration connector received an unexpected exception {1} when cataloguing topics; the error message was: {2}",
             "The connector is unable to catalog one or more topics.",
             "Use the details from the error message to determine the cause of the error and retry the request once it is resolved."),
    ;

    private ExceptionMessageDefinition messageDefinition;


    /**
     * The constructor for StrimziIntegrationConnectorErrorCode expects to be passed one of the enumeration rows defined in
     * StrimziIntegrationConnectorErrorCode above.   For example:
     *
     *     StrimziIntegrationConnectorErrorCode   errorCode = StrimziIntegrationConnectorErrorCode.ERROR_SENDING_EVENT;
     *
     * This will expand out to the 5 parameters shown below.
     *
     *
     * @param httpErrorCode   error code to use over REST calls
     * @param errorMessageId   unique Id for the message
     * @param errorMessage   text for the message
     * @param systemAction   description of the action taken by the system when the error condition happened
     * @param userAction   instructions for resolving the error
     */
    StrimziIntegrationConnectorErrorCode(int  httpErrorCode, String errorMessageId, String errorMessage, String systemAction, String userAction)
    {
        this.messageDefinition = new ExceptionMessageDefinition(httpErrorCode,
                                                                errorMessageId,
                                                                errorMessage,
                                                                systemAction,
                                                                userAction);
    }


    /**
     * Retrieve a message definition object for an exception.  This method is used when there are no message inserts.
     *
     * @return message definition object.
     */
    @Override
    public ExceptionMessageDefinition getMessageDefinition()
    {
        return messageDefinition;
    }


    /**
     * Retrieve a message definition object for an exception.  This method is used when there are values to be inserted into the message.
     *
     * @param params array of parameters (all strings).  They are inserted into the message according to the numbering in the message text.
     * @return message definition object.
     */
    @Override
    public ExceptionMessageDefinition getMessageDefinition(String... params)
    {
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
        return "StrimziIntegrationConnectorErrorCode{" +
                       "messageDefinition=" + messageDefinition +
                       '}';
    }
}
