*** Settings ***
Documentation   This test file contains the test cases of monitoring events API functions in dummy netapp, refering to Nef API.
Library         /opt/robot-tests/pythonnetapp/emulator_utils.py
Library         /opt/robot-tests/pythonnetapp/netapp_to_nef.py
Library         String
Library         Collections
Resource        /opt/robot-tests/resources/common/basicFunctions.robot
Variables       /opt/robot-tests/libraries/ConfigVariables.py  CONFIG  /opt/robot-tests/pythonnetapp/credentials.properties

*** Variables ***
${NEF_HOSTNAME}                   ${CONFIG.credentials.nef_ip}:${CONFIG.credentials.nef_port}
${non-auth}                       eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJmcmVzaCI6ZmFsc2UsImlhdCI6MTY0NzYxNDQxNSwianRpIjoiZTc3MDhjMmMtZjFiMi00MDc1LWFlNTctM2YxYmYyYmU4YWY1IiwidHlwZSI6ImFjY2VzcyIsInN1YiI6ImN1c3RvbTRuZXRhcHAgaW52b2tlciIsIm5iZiI6MTY0NzYxNDQxNSwiZXhwIjoxNjQ3NjE1MzE1fQ.8CWiqYTtje4AjDmNqA6OjmYMJF3M90NM4GnYIOyHNnI
${API_INVOKER_NOT_REGISTERED}     not-valid


*** Keywords ***


*** Test Cases ***

Create subscription by Authorized NetApp

    [Tags]    Create_subscription_by_Authorized_NetApp

    Copy File      /opt/robot-tests/pythonnetapp/credentials.properties    .

    ${access_token}=    Run Keyword    netapp_to_nef.register_netapp_to_nef      ${CONFIG.credentials.nef_ip}    ${CONFIG.credentials.nef_port} 
    ${access_token}=    Catenate       ${access_token.access_token}

    Log To Console      ${access_token}

    ${resp}=            Run Keyword   netapp_to_nef.monitor_subscription        2    ${access_token}
    
    Log To Console      ${resp}

    Should Not Be Empty      ${resp}


One-time request to the Monitoring Event API by Authorized NetApp

    [Tags]    One_time_request_subscription_by_Authorized_NetApp

    ${access_token}=    Run Keyword    netapp_to_nef.register_netapp_to_nef  ${CONFIG.credentials.nef_ip}    ${CONFIG.credentials.nef_port}  
    ${access_token}=    Catenate       ${access_token.access_token}

    ${resp}=            Run Keyword    netapp_to_nef.monitor_subscription     1    ${access_token}
    
    Log To Console      ${resp}

    Should Not Be Empty      ${resp}


Create subscription when there is already an active subscription for a registered UE

    [Tags]    Create_subscription_when_already_active_by_Authorized_NetApp

    ${access_token}=    Run Keyword    netapp_to_nef.register_netapp_to_nef  ${CONFIG.credentials.nef_ip}    ${CONFIG.credentials.nef_port}
    ${access_token}=    Catenate       ${access_token.access_token}

    ${resp}=            Run Keyword And Expect Error  *  netapp_to_nef.monitor_subscription        2    ${access_token}

    Log To Console      ${resp}

    Should Contain    ${resp}   409


Create subscription by unAuthorized NetApp

    [Tags]    Create_subscription_when_already_active_by_Authorized_NetApp

    ${access_token}=    Run Keyword    emulator_utils.get_token  
    ${access_token}=    Catenate       ${access_token.access_token}

    ${resp}=            Run Keyword And Expect Error  *  netapp_to_nef.monitor_subscription    2    ${non-auth}    testID
    
    Log To Console    ${resp}

    Should Contain    ${resp}   403


*** Comments ***
