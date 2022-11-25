#!/bin/bash

set -e

CERTS_PATH="backend/app/app/core/certificates"
check_num_of_files=$(ls $CERTS_PATH | wc -l)
check_private_key=$(openssl rsa -in $CERTS_PATH/private.key -check 2>&1)

if [[ ${check_num_of_files} -eq 4 ]]; then
   echo "All files are here";
else
   echo "Missing files";
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
check_client_cert=$(openssl x509 -in $CERTS_PATH/$client_cert_name -noout -text 2>&1)

if [[ "$check_client_cert" == *"$client_cert_basename"* ]]; then
   echo "Client certificate OK";
   exit 0;
else
   echo "Client certificate not OK";
   exit 1;
fi