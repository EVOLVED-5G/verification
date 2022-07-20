*** Settings ***
Documentation   This test file contains the basic register requests from netApp to Capif API.
# Library         RequestsLibrary
# Library         Collections
Library         /opt/robot-tests/pythonnetapp/apf_to_capif.py
Library         /opt/robot-tests/pythonnetapp/netapp_to_capif.py
Library         OperatingSystem
Variables       /opt/robot-tests/libraries/ConfigVariables.py  CONFIG  /opt/robot-tests/credentials.properties

*** Variables ***
# ${CAPIF_HOSTNAME}           ${CONFIG.credentials.capif_ip}:${CONFIG.credentials.capif_port}
${success}                  201
${already_exists}           409
${forbidden}                403

*** Keywords ***

DummyNetApp_register

    [Arguments]    ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${CONFIG.credentials.invoker_username}    ${CONFIG.credentials.invoker_password}    ${CONFIG.credentials.invoker_role}    ${CONFIG.credentials.invoker_description}

    ${resp}=       register_netapp_to_capif    ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${CONFIG.credentials.invoker_username}    ${CONFIG.credentials.invoker_password}    ${CONFIG.credentials.invoker_role}    ${CONFIG.credentials.invoker_description}

    Set Global Variable    ${api_invoker_id}    ${resp}
    
    [Return]       ${resp}
    
Apf_register 

    [Arguments]    ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${CONFIG.credentials.apf_username}    ${CONFIG.credentials.apf_password}    ${CONFIG.credentials.apf_role}    ${CONFIG.credentials.apf_description}

    Copy File      /opt/robot-tests/pythonnetapp/service_api_description.json    .

    ${resp}=       register_apf_to_capif    ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${CONFIG.credentials.apf_username}    ${CONFIG.credentials.apf_password}    ${CONFIG.credentials.apf_role}    ${CONFIG.credentials.apf_description}

    Set Global Variable    ${api_apf_id}    ${resp}
    
    [Return]       ${resp}

DummyNetApp_already_registered

    ${output}=         Run Keyword And Expect Error    *    DummyNetApp_register    ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${CONFIG.credentials.invoker_username}    ${CONFIG.credentials.invoker_password}    ${CONFIG.credentials.invoker_role}    ${CONFIG.credentials.invoker_description}

    Should contain     ${output}    ${already_exists}


Token_case_invoker 

    ${resp}=    netapp_to_capif.get_capif_token   ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${CONFIG.credentials.invoker_username}    ${CONFIG.credentials.invoker_password}    ${CONFIG.credentials.invoker_role}

    [Return]    ${resp}


Token_case_apf 

    ${resp}=    apf_to_capif.get_capif_token   ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${CONFIG.credentials.apf_username}    ${CONFIG.credentials.apf_password}    ${CONFIG.credentials.apf_role}

    [Return]    ${resp}


OnBoard_DummyNetApp_invoker
    
    Copy File         /opt/robot-tests/pythonnetapp/invoker_details.json    .

    ${access_token}=  netapp_to_capif.get_capif_token   ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${CONFIG.credentials.invoker_username}    ${CONFIG.credentials.invoker_password}    ${CONFIG.credentials.invoker_role}

    ${resp}=          netapp_to_capif.onboard_netapp_to_capif   ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${access_token}

    [Return]          ${resp}


OnBoard_DummyNetApp_apf

    # Run keyword       MoveFile
    
    ${access_token}=  apf_to_capif.get_capif_token   ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${CONFIG.credentials.invoker_username}    ${CONFIG.credentials.invoker_password}    ${CONFIG.credentials.invoker_role}

    ${resp}=          netapp_to_capif.onboard_netapp_to_apf   ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${access_token}

    [Return]          ${resp}


Invoker_already_registered

    ${output}=         Run Keyword And Expect Error    *    OnBoard_DummyNetApp_invoker

    Should contain     ${output}    ${forbidden}


Publish services

    [Arguments]    ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${api_invoker_id}    ${access_token}
     
    ${resp}=       apf_to_capif.publish_service_api_to_capif    ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${api_invoker_id}    ${access_token}

    [Return]       ${resp}


Clean Test Information By HTTP Requests

    [Arguments]    ${NGINX_HOSTNAME}

    # ${resp}=       DELETE On Session      jwtsession    /testdata

    # Should Be Equal As Strings    ${resp.status_code}    200

