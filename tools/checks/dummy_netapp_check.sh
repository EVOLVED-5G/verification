#!/bin/bash

set -e

CERTS_PATH="pythonnetapp/capif_onboarding"
check_num_of_files=$(ls $CERTS_PATH | wc -l)
check_private_key=$(openssl rsa -in $CERTS_PATH/private.key -check 2>&1)
check_signing_request=$(openssl req -verify -in $CERTS_PATH/cert_req.csr 2>&1)

if [[ ${check_num_of_files} -eq 5 ]]; then
   echo "All files are here";
else
   echo "Missing files";
   exit 1;
fi


if [[ "$check_signing_request" =~ .*"verify OK".* ]]; then
   echo "CSR OK";
else
   echo "CSR not OK";
   exit 1;
fi


if [[ "$check_private_key" == *"RSA key ok"* ]]; then
   echo "Private key OK";
else
   echo "Private key not OK";
   exit 1;
fi

client_cert=$(find $CERTS_PATH -name "test*.crt")
client_cert_name=$(basename -- "$client_cert")
client_cert_basename="${client_cert_name%.*}"

check_client_cert_based_on_ca=$(openssl verify -verbose -CAfile $CERTS_PATH/ca.crt $CERTS_PATH/$client_cert_name 2>&1)
check_client_cert=$(openssl x509 -in $CERTS_PATH/$client_cert_name -noout -text 2>&1)

if [[ "$check_client_cert_based_on_ca" == "$client_cert: OK" ]]; then
   echo "Check 1: Client certificate OK";
else
   echo "Check 1: Client certificate not OK";
   exit 1;
fi


if [[ "$check_client_cert" == *"$client_cert_basename"* ]]; then
   echo "Check 2: Client certificate OK";
   exit 0;
else
   echo "Check 2: Client certificate not OK";
   exit 1;
fi
