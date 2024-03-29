*** Settings ***
Library         RequestsLibrary
Library         Collections
Library         /opt/robot-tests/pythonnetapp/emulator_utils.py
Variables       /opt/robot-tests/libraries/ConfigVariables.py  CONFIG  /opt/robot-tests/credentials.properties


*** Variables ***
${APF_ID_NOT_VALID}         not-valid
${CAPIF_HOSTNAME}           http://${CONFIG.credentials.capif_callback_ip}:${CONFIG.credentials.capif_callback_port}
# ${NEF_HOSTNAME}             http://dummy-netapp_nef_callback_server_1:8080

*** Keywords ***

capifcallback

    Create Session    mysession    ${CAPIF_HOSTNAME}     verify=True

    ${resp}=    POST On Session    mysession    /capifcallbacks    

    return ${resp}

nefcallback

    ${NEF_HOSTNAME}=    emulator_utils.get_callback_server_for_nef_responses

    Create Session    mysession     ${NEF_HOSTNAME}    verify=True

    ${resp}=    POST On Session    mysession    /    

    return ${resp}


*** Test Cases ***

Capif Callback Server Test Case

    [Tags]    capif_callback_server

    ${resp}=      Run Keyword    capifcallback    

    Should Contain    ${resp}    201

    Log To Console    ${resp.message}

Nef Callback Server Test Case

    [Tags]    nef_callback_server

    ${resp}=      Run Keyword    nefcallback    

    Should Contain    ${resp}    201

    Log To Console    ${resp.message}
