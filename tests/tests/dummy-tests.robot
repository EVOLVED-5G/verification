*** Settings ***
Documentation   This test file contains the basic register requests from netApp to Capif API.
Library         RequestsLibrary
Library         /opt/robot-tests/pythonnetapp/netapp_to_capif.py
Variables       /opt/robot-tests/tests/libraries/ConfigVariables.py  CONFIG  /opt/robot-tests/pythonnetapp/credentials.properties

*** Variables ***
${CAPIF_HOSTNAME}           ${CONFIG.credentials.capif_ip}:${CONFIG.credentials.capif_port}
${status}                   201

*** keywords ***

*** Test Cases ***
Register DummyNetApp

    [Tags]      Dummy_NetApp_Register_Test

    ${resp}=  register_netapp_to_capif    ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${CONFIG.credentials.invoker_username}    ${CONFIG.credentials.invoker_password}    ${CONFIG.credentials.invoker_role}    ${CONFIG.credentials.invoker_description}

	Run keyword if   "${resp}" > "600"   ${status}

    # Should be equal as str  ${resp}  201
