# NOTE: this Dockerfile is not used by the CI pipeline
# on openshift because we want to add multiple projects
# to the same image later.

FROM quay.io/ibmgaragecloud/gradle:jdk11 as build

# Copy files
COPY gradle gradle
COPY settings.gradle .
COPY gradlew .
COPY build.gradle .
COPY src src

# Build application and test it
RUN ./gradlew assemble --no-daemon && \
    ./gradlew testClasses --no-daemon

# Get Egeria base image and add build artifacts
FROM quay.io/odpi/egeria:latest
COPY --from=build /home/gradle/build /strimzi/build