*** Settings ***
Resource    /opt/robot-tests/tests/resources/common.resource
Resource    /opt/robot-tests/tests/resources/api_invoker_management_requests/apiInvokerManagementRequests.robot
Library     /opt/robot-tests/tests/libraries/api_invoker_management/bodyRequests.py

Test Setup    Initialize Test And Register    role=invoker

*** Variables ***
${API_INVOKER_NOT_REGISTERED}    not-valid

*** Keywords ***


*** Test Cases ***
Register DummyNetApp
	[Tags]    capif_api_invoker_management-1
	
	${request_body}=    Create Onboarding Notification Body
	${resp}=            Post Request Capif                     /api-invoker-management/v1/onboardedInvokers    ${request_body}

	Should Be Equal As Strings    ${resp.status_code}    201
