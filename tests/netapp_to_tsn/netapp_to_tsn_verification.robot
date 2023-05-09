*** Settings ***
Documentation   This test file contains the test cases for the network application communication with TSN
Library         /opt/robot-tests/pythonnetapp/0_network_app_to_tsn.py
Library         String
Library         Collections

*** Variables ***

*** Keywords ***


*** Test Cases ***
Initialize TSN Manager
    [Tags]    initialize_tsn_manager
    ${resp}=    Run Keyword     0_network_app_to_tsn.initialize_tsn
    Log To Console      ${resp}
    ${text}=    Set Variable If     "${resp}" == "${None}"  ${EMPTY}    ${resp}
    Should Be Equal      ${resp}  ${text}

Valid retrieval of TSN profiles
    [Tags]    valid_retrieval_of_tsn_profiles
    ${resp}=    Run Keyword     0_network_app_to_tsn.get_profiles
    Log To Console      ${resp}
    ${count}=   Get Length      ${resp}
    Should Be True  ${count} > 0

Apply TSN profile
    [Tags]    apply_tsn_profile
    ${identifier}   ${clearance_token}=    Run Keyword     0_network_app_to_tsn.apply_tsn_profile
    Log To Console      ${identifier}
    Log To Console      ${clearance_token}
    ${text}=    Set Variable If     "${identifier}" == "${None}"  ${EMPTY}    ${identifier}
    Should Be Equal      ${identifier}  ${text}
    Should Not Be Empty      ${clearance_token}
    Set Suite Variable      ${clearance_token}
    Set Suite Variable      ${identifier}


Clear Profile Configuration
    [Tags]    apply_tsn_profile
    Log To Console      ${identifier}
    Log To Console      ${clearance_token}
    Run Keyword     0_network_app_to_tsn.clear_profile_configuration     ${identifier}   ${clearance_token}
