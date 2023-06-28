*** Settings ***
Documentation   This test file contains the test cases of monitoring events API functions in network application, refering to Nef API.
Library         /opt/robot-tests/network-application/%{FILE_TO_IMPORT}
Library         String
Library         Collections
Resource        /opt/robot-tests/resources/common/basicFunctions.robot
Variables       /opt/robot-tests/libraries/ConfigVariables.py    CONFIG    /opt/robot-tests/credentials.properties

*** Variables ***
${NEF_HOSTNAME}                   https://${CONFIG.credentials.nef_ip}:${CONFIG.credentials.nef_port}
${CERTIFICATE_FOLDER}             %{CERTIFICATES_FOLDER_PATH}
${CAPIF_HOST}                     ${CONFIG.credentials.capif_ip}
${CAPIF_PORT}                     ${CONFIG.credentials.capif_port}
${NEF_CALLBACK_HOSTNAME}          http://${CONFIG.credentials.nef_callback_ip}:${CONFIG.credentials.nef_callback_port}/nefcallbacks

*** Keywords ***


*** Test Cases ***

Create subscription by Authorized Network Application

    [Tags]    Create_subscription_by_Authorized_Network_Application

    Copy File      /opt/robot-tests/credentials.properties    .

    ${resp}=            Run Keyword   Monitor Subscription  2  ${NEF_HOSTNAME}  ${CERTIFICATE_FOLDER}  ${CAPIF_HOST}  ${CAPIF_PORT}  ${NEF_CALLBACK_HOSTNAME}

    Log To Console      ${resp}

    Should Not Be Empty      ${resp}


One-time request to the Monitoring Event API by Authorized Network Application

    [Tags]    One_time_request_subscription_by_Authorized_Network_Application

    Copy File      /opt/robot-tests/credentials.properties    .

    ${resp}=            Run Keyword   Monitor Subscription  1  ${NEF_HOSTNAME}  ${CERTIFICATE_FOLDER}  ${CAPIF_HOST}  ${CAPIF_PORT}  ${NEF_CALLBACK_HOSTNAME}

    Log To Console      ${resp}

    Should Not Be Empty      ${resp}


Create subscription when there is already an active subscription for a registered UE

    [Tags]    Create_subscription_when_already_active_by_Authorized_Network_Application

    Copy File      /opt/robot-tests/credentials.properties    .

    ${resp}=            Run Keyword And Expect Error  *    Monitor Subscription   2  ${NEF_HOSTNAME}  ${CERTIFICATE_FOLDER}  ${CAPIF_HOST}  ${CAPIF_PORT}  ${NEF_CALLBACK_HOSTNAME}

    Log To Console      ${resp}

    Should Contain    ${resp}   409
