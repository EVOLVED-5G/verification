*** Settings ***
Documentation   This test file contains the test cases of loss of connection events API functions in dummy netapp, refering to Nef API.
Library         /opt/robot-tests/pythonnetapp/1_netapp_to_nef.py
Library         /opt/robot-tests/libraries/scenario/import_scenario.py
Library         String
Library         Collections
Resource        /opt/robot-tests/resources/common/basicFunctions.robot
Variables       /opt/robot-tests/libraries/ConfigVariables.py    CONFIG    /opt/robot-tests/credentials.properties

*** Variables ***
${NEF_HOSTNAME}                   http://${CONFIG.credentials.nef_ip}:${CONFIG.credentials.nef_port}
${NEF_USER}                       ${CONFIG.credentials.nef_user}
${NEF_PASSWORD}                   ${CONFIG.credentials.nef_pass}
${NEF_HOSTNAME}                   http://${CONFIG.credentials.nef_ip}:${CONFIG.credentials.nef_port}
${non-auth}                       eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJmcmVzaCI6ZmFsc2UsImlhdCI6MTY0NzYxNDQxNSwianRpIjoiZTc3MDhjMmMtZjFiMi00MDc1LWFlNTctM2YxYmYyYmU4YWY1IiwidHlwZSI6ImFjY2VzcyIsInN1YiI6ImN1c3RvbTRuZXRhcHAgaW52b2tlciIsIm5iZiI6MTY0NzYxNDQxNSwiZXhwIjoxNjQ3NjE1MzE1fQ.8CWiqYTtje4AjDmNqA6OjmYMJF3M90NM4GnYIOyHNnI
${API_INVOKER_NOT_REGISTERED}     not-valid
${CERTIFICATE_FOLDER}             ${CONFIG.credentials.certificate_folder}
${CAPIF_HOST}                     ${CONFIG.credentials.capif_ip}
${CAPIF_PORT}                     ${CONFIG.credentials.capif_port}
${NEF_CALLBACK_HOSTNAME}          http://${CONFIG.credentials.nef_callback_ip}:${CONFIG.credentials.nef_callback_port}/nefcallbacks

*** Keywords ***


*** Test Cases ***

Create subscription by Authorized NetApp

    [Tags]    Create_subscription_by_Authorized_NetApp

    Copy File      /opt/robot-tests/credentials.properties    .

    ${access_token}=    Run Keyword    1_netapp_to_nef.request_nef_token  ${NEF_HOSTNAME}  ${NEF_USER}  ${NEF_PASSWORD}
    ${access_token}=    Catenate       ${access_token.access_token}

    Log To Console      ${access_token}

    ${resp}=            Run Keyword   1_netapp_to_nef.connection_monitoring_loss_of_conn  ${NEF_HOSTNAME}  ${access_token}  ${CERTIFICATE_FOLDER}  ${CAPIF_HOST}  ${CAPIF_PORT}  ${NEF_CALLBACK_HOSTNAME}

    Log To Console      ${resp}

    Should Not Be Empty      ${resp}


One-time request to the Monitoring Event API by Authorized NetApp

    [Tags]    One_time_request_subscription_by_Authorized_NetApp

    Copy File      /opt/robot-tests/credentials.properties    .

    ${access_token}=    Run Keyword    1_netapp_to_nef.request_nef_token  ${NEF_HOSTNAME}  ${NEF_USER}  ${NEF_PASSWORD}
    ${access_token}=    Catenate       ${access_token.access_token}

    Log To Console      ${access_token}

    ${resp}=            Run Keyword   1_netapp_to_nef.connection_monitoring_loss_of_conn  ${NEF_HOSTNAME}  ${access_token}  ${CERTIFICATE_FOLDER}  ${CAPIF_HOST}  ${CAPIF_PORT}  ${NEF_CALLBACK_HOSTNAME}

    Log To Console      ${resp}

    Should Not Be Empty      ${resp}


Create subscription when there is already an active subscription for a registered UE

    [Tags]    Create_subscription_when_already_active_by_Authorized_NetApp

    Copy File      /opt/robot-tests/credentials.properties    .

    ${access_token}=    Run Keyword    1_netapp_to_nef.request_nef_token  ${NEF_HOSTNAME}  ${NEF_USER}  ${NEF_PASSWORD}
    ${access_token}=    Catenate       ${access_token.access_token}

    Log To Console      ${access_token}

    ${resp}=            Run Keyword And Expect Error  *    1_netapp_to_nef.connection_monitoring_loss_of_conn  ${NEF_HOSTNAME}  ${access_token}  ${CERTIFICATE_FOLDER}  ${CAPIF_HOST}  ${CAPIF_PORT}  ${NEF_CALLBACK_HOSTNAME}

    Log To Console      ${resp}

    Should Contain    ${resp}   409

Create subscription by unAuthorized NetApp

    [Tags]    Create_subscription_when_already_active_by_Authorized_NetApp

    Copy File      /opt/robot-tests/credentials.properties    .

    ${resp}=            Run Keyword And Expect Error  *   1_netapp_to_nef.connection_monitoring_loss_of_conn  ${NEF_HOSTNAME}  ${non-auth}   ${CERTIFICATE_FOLDER}  ${CAPIF_HOST}  ${CAPIF_PORT}  ${NEF_CALLBACK_HOSTNAME}

    Log To Console      ${resp}

    Should Contain    ${resp}   401