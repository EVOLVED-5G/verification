#!/bin/bash

set -e

CERTS_PATH="backend/app/app/core/certificates"
check_num_of_files=$(ls $CERTS_PATH | grep -v gitkeep | wc -l)

if [[ ${check_num_of_files} -eq 12 ]]; then
   echo "All files are here";
else
   echo "Missing files";
   exit 1;
fi