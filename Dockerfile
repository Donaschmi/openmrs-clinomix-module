FROM openmrs/openmrs-core:2.7.4

# Essential modules for O3 to work
COPY --chown=1001:1001 modules/webservices.rest-2.49.0.omod        /openmrs/data/modules/
COPY --chown=1001:1001 modules/fhir2-2.5.0.omod                    /openmrs/data/modules/
COPY --chown=1001:1001 modules/legacyui-1.23.0.omod                /openmrs/data/modules/

# Copy the clinomix OMOD into the modules directory
COPY --chown=1001:1001 omod/target/clinomix-omod-1.0.0-SNAPSHOT.omod /openmrs/data/modules/
