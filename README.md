<!-- SPDX-License-Identifier: CC-BY-4.0 -->
<!-- Copyright Contributors to the ODPi Egeria project. -->

[![CII Best Practices](https://bestpractices.coreinfrastructure.org/projects/3044/badge)](https://bestpractices.coreinfrastructure.org/projects/3044)
[![Azure](https://dev.azure.com/odpi/egeria/_apis/build/status/odpi.egeria)](https://dev.azure.com/odpi/Egeria/_build)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=odpi_egeria&metric=alert_status)](https://sonarcloud.io/dashboard?id=odpi_egeria)
[![Maven Central](https://img.shields.io/maven-central/v/org.odpi.egeria/egeria)](https://mvnrepository.com/artifact/org.odpi.egeria)


TODO most of the below information should be moved to egeria-docs.

# Egeria integration connector for Strimzi Topics

This project contains the Egeria integration connector for [Strimzi](https://strimzi.io/) topics. It obtains the Kafka
topic information by querying the custom resource definitions in Strimzi.

This integration connector needs to be configured. The following steps can be performed using the postman collection to set this connector up.
The connector takes Kafka topic information including the description, partitions and replicas and pushes that information into
Egeria.


__Important notice__

The gradle JAR step will include some of the dependencies into the connector JAR, making is a semi-Fat Jar. This makes sure that additional dependencies are automatically deployed together with the connector.


## For testing

### Build
On a terminal,
* navigate to the folder that this README.md is in.
* run ```./gradlew clean build```

### Running locally for testing

You will need to have an omag platform with the connector jar, a metadata server called cocoMDS1 defined.
The below process is how you configure the Strimzi integration connector manually.

Import the [postman collection]([Postman collection](postman/Strimzi%20integration%20connector%20configuration.postman_collection.json)) into [Postman](https://www.postman.com/)
The required values should be supplied as postman environment variables for this script. 

Then run the scripts in order. Steps 1 through 3 to configure the connector.

* Step 3 is where the integration connector configuration parameters are passed into the connector.
* Step 4 Starts the connector. 
* Step 5 shows how to retrieve the configuration

The configuration only needs to occur once (unless you want to change it), so subsequently you only need to issue step 4 to start it.

### Testing mutations
Import the [postman collection](postman/Strimzi%20REST%20calls.postman_collection.json)] into [Postman](https://www.postman.com/)

Set the postman variables you want and run the create , delete and update scripts 


----
License: [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/),
Copyright Contributors to the ODPi Egeria project.
