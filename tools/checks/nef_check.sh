#!/bin/bash

set -e

CERTS_PATH="backend/app/app/core/certificates"
check_num_of_files=$(ls $CERTS_PATH | wc -l)
check_private_key=$(openssl rsa -in $CERTS_PATH/private.key -check 2>&1)

if [[ ${check_num_of_files} -eq 12 ]]; then
   echo "All files are here";
else
   echo "Missing files";
   exit 1;
fi