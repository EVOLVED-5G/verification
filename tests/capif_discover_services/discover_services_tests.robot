*** Settings ***
Documentation   This test file contains the basic register requests from netApp to Capif API.
Library         /opt/robot-tests/pythonnetapp/netapp_to_capif.py
# Library         /opt/robot-tests/pythonnetapp/invoker_details.json
Resource        /opt/robot-tests/resources/common/basicFunctions.robot
Variables       /opt/robot-tests/libraries/ConfigVariables.py  CONFIG  /opt/robot-tests/tools/credentials.properties

*** Variables ***
${CAPIF_HOSTNAME}           ${CONFIG.credentials.capif_ip}:${CONFIG.credentials.capif_port}
${non-auth}                 eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJmcmVzaCI6ZmFsc2UsImlhdCI6MTY0NzYxNDQxNSwianRpIjoiZTc3MDhjMmMtZjFiMi00MDc1LWFlNTctM2YxYmYyYmU4YWY1IiwidHlwZSI6ImFjY2VzcyIsInN1YiI6ImN1c3RvbTRuZXRhcHAgaW52b2tlciIsIm5iZiI6MTY0NzYxNDQxNSwiZXhwIjoxNjQ3NjE1MzE1fQ.8CWiqYTtje4AjDmNqA6OjmYMJF3M90NM4GnYIOyHNnI
${API_INVOKER_NOT_REGISTERED}    not-valid
${NGINX_HOSTNAME}           openshift.evolved-5g.eu


*** Keywords ***

OnBoard_DummyNetApp
    
    [Arguments]       ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${access_token}

    ${resp}=          OnBoard_DummyNetApp_invoker   ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${access_token}

    Set Global Variable    ${api_invoker_id}    ${resp}

    [Return]          ${resp}


Discover services

    [Arguments]    ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${id}    ${access_token}
     
    ${resp}=       netapp_to_capif.discover_service_apis    ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${id}    ${access_token}

    [Return]       ${resp}


*** Test Cases ***
Discover Published service APIs by Authorised API Invoker

    [Tags]    Discover_Services_by_Authorised_Invoker

    Run Keyword    Apf_register    ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${CONFIG.credentials.apf_username}    ${CONFIG.credentials.apf_password}    ${CONFIG.credentials.apf_role}    ${CONFIG.credentials.apf_description}

    ${access_token_apf}=      Run Keyword    Token_case_apf    
    
    Run Keyword    Publish services    ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${api_apf_id}    ${access_token_apf}

    Run Keyword    DummyNetApp_register    ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${CONFIG.credentials.invoker_username}    ${CONFIG.credentials.invoker_password}    ${CONFIG.credentials.invoker_role}    ${CONFIG.credentials.invoker_description}

    ${access_token_invoker}=      Run Keyword    Token_case_invoker     

    ${temp}=                      Run Keyword    OnBoard_DummyNetApp_invoker

    Set Global Variable    ${invId}    ${temp}

    ${resp}=    Run Keyword   Discover services    ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${invId}    ${access_token_invoker}

    Log To Console    ${resp}
    # Should contain         ${resp}    200

    # Should Not Be Empty    ${resp}
	# Length Should Be       ${resp}    1

Discover Published service APIs by Non Authorised API Invoker

    [Tags]    Discover_Services_by_Authorised_Invoker

    ${access_token}=      Run Keyword    Token_case_invoker

    # ${invId}=             Run Keyword    OnBoard_DummyNetApp_invoker

    ${resp}=    Run keyword And Expect Error    *    Discover services    ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${invId}    ${non-auth}

    Should contain         ${resp}    401

Discover Published service APIs by not registered API Invoker

    [Tags]    Discover_Services_by_Authorised_Invoker

    ${access_token}=      Run Keyword    Token_case_invoker

    ${resp}=    Run keyword And Expect Error    *    Discover services    ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${API_INVOKER_NOT_REGISTERED}    ${access_token}

    Should contain         ${resp}    403

Discover Published service APIs by registered API Invoker with 1 result filtered

    ${access_token_invoker}=      Run Keyword    Token_case_invoker     

    # ${invId}=                     Run Keyword    OnBoard_DummyNetApp_invoker

    ${resp}=    Run Keyword   Discover services    ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${invId}    ${access_token_invoker}

    # Should contain         ${resp}    200

    Should Not Be Empty    ${resp}
	Length Should Be       ${resp}    1


Discover Published service APIs by registered API Invoker filtered with no match

    [Tags]    Discover_Services_by_Registered_Invoker_no_match

    ${access_token_invoker}=      Run Keyword    Token_case_invoker     

    # ${invId}=                     Run Keyword    OnBoard_DummyNetApp_invoker

    ${resp}=    Run Keyword   Discover services    ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${invId}    ${access_token_invoker}

    # Should contain         ${resp}    200

    Should Not Be Empty    ${resp}
	Length Should Be       ${resp}    0

Discover Published service APIs by registered API Invoker not filtered

    [Tags]    Discover_Services_by_Registered_Invoker_no_match

    ${access_token_invoker}=      Run Keyword    Token_case_invoker     

    # ${invId}=                     Run Keyword    OnBoard_DummyNetApp_invoker

    ${resp}=    Run Keyword   Discover services    ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${invId}    ${access_token_invoker}

    # Should contain         ${resp}    200

    Should Not Be Empty    ${resp}
	Length Should Be       ${resp}    2


*** Comments ***
