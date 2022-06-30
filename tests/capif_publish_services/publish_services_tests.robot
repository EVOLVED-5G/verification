
*** Settings ***
Library         /opt/robot-tests/pythonnetapp/apf_to_capif.py
Resource        /opt/robot-tests/resources/common/basicFunctions.robot
Variables       /opt/robot-tests/libraries/ConfigVariables.py  CONFIG  /opt/robot-tests/credentials.properties


*** Variables ***
${APF_ID_NOT_VALID}         not-valid


*** Keywords ***

*** Test Cases ***

Publish API by Authorised API Publisher

	[Tags]    capif_api_publish_service_by_auth_api_pub

    ${resp} =    Run Keyword    Apf_register    ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${CONFIG.credentials.apf_username}    ${CONFIG.credentials.apf_password}    ${CONFIG.credentials.apf_role}    ${CONFIG.credentials.apf_description}

    Set Global Variable    ${apfId}    ${resp}

    ${access_token_apf}=      Run Keyword    Token_case_apf    

    ${resp}=    Run Keyword    Publish services    ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${api_apf_id}    ${access_token_apf}


Publish API by NON Authorised API Publisher

    [Tags]    capif_api_publish_service_by_non_auth_api_pub

    ${access_token_apf}=      Run Keyword    Token_case_apf    

    ${resp}=    Run keyword And Expect Error    *     Publish services    ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${APF_ID_NOT_VALID}    ${access_token_apf}

    Should contain         ${resp}    401


Retrieve all APIs Published by Authorised apfId

    [Tags]    retrieve_published_service_by_auth_apfId

    ${access_token_apf}=      Run Keyword    Token_case_apf    

    ${resp}=    Run keyword And Expect Error    *     Publish services    ${CONFIG.credentials.capif_ip}    ${CONFIG.credentials.capif_port}    ${apfId}    ${access_token_apf}

    Should contain         ${resp}    200

*** Comments ***

