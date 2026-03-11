FROM openmrs/openmrs-core:2.7.4

# Essential modules for O3 to work
COPY modules/webservices.rest-2.49.0.omod        /openmrs/data/modules/
COPY modules/fhir2-2.5.0.omod                    /openmrs/data/modules/
COPY modules/legacyui-1.23.0.omod                /openmrs/data/modules/

# Copy the clinomix OMOD into the modules directory
COPY omod/target/clinomix-omod-1.0.0-SNAPSHOT.omod /openmrs/data/modules/
