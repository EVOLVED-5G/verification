*** Settings ***
Documentation   This test file contains the basic register requests from netApp to Capif API.
# Library         RequestsLibrary
# Library         Collections
Library         /opt/robot-tests/pythonnetapp/netapp_to_capif.py
Resource        /opt/robot-tests/resources/common/basicFunctions.robot
Variables       /opt/robot-tests/libraries/ConfigVariables.py  CONFIG  /opt/robot-tests/pythonnetapp/credentials.properties

*** Variables ***
${CAPIF_HOSTNAME}           ${CONFIG.credentials.capif_ip}:${CONFIG.credentials.capif_port}

*** Keywords ***


*** Test Cases ***
Register DummyNetApp

    [Tags]         Dummy_NetApp_Register_Test

    Run Keyword    DummyNetApp_register    openshift.evolved-5g.eu    80    ${CONFIG.credentials.invoker_username}    ${CONFIG.credentials.invoker_password}    ${CONFIG.credentials.invoker_role}    ${CONFIG.credentials.invoker_description}

Register DummyNetApp Already registered

    [Tags]     Dummy_NetApp_Already_Registered_Test

    DummyNetApp_already_registered

Get Capif Token

    [Tags]        Get_Capif_Token_Test

  	Run keyword   Token_case_invoker

Onboard DummyNetApp to Capif

    [Tags]         Onboard_Netapp_to_Capif

    Run Keyword    OnBoard_DummyNetApp_invoker

Already registered invoker to Capif

    [Tags]         Onboard_Netapp_to_Capif

    Run Keyword    Invoker_already_registered


*** Comments ***

