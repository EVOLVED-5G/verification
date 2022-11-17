*** Settings ***
Documentation   This test file contains the basic register requests from netApp to Capif API.
# Library         RequestsLibrary
# Library         Collections
Library         OperatingSystem
Variables       /opt/robot-tests/libraries/ConfigVariables.py  CONFIG  /opt/robot-tests/credentials.properties

*** Variables ***
# ${CAPIF_HOSTNAME}           ${CONFIG.credentials.capif_ip}:${CONFIG.credentials.capif_port}
${success}                  201
${already_exists}           409
${forbidden}                403

*** Keywords ***
