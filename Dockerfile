# SPDX-License-Identifier: Apache-2.0
# Copyright Contributors to the Egeria project

# This is the EGERIA version - typically passed from the ci/cd pipeline
ARG EGERIA_BASE_IMAGE=quay.io/odpi/egeria:latest

FROM ${EGERIA_BASE_IMAGE}

# Labels from https://github.com/opencontainers/image-spec/blob/master/annotations.md#pre-defined-annotation-keys (with additions prefixed    ext)
# We should inherit all the base labels from the egeria image and only overwrite what is necessary.
LABEL org.opencontainers.image.description = "Egeria with Strimzi connector" \
      org.opencontainers.image.documentation = "https://github.com/odpi/egeria-connector-integration-topic-strimzi"

# This assumes we only have one uber jar (ensure old versions cleaned out beforehand). Avoids having to pass connector version
COPY build/libs/egeria-connector-integration-topic-strimzi-*-with-dependencies.jar /deployments/server/lib

