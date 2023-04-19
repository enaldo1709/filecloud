#!/bin/bash

for image in ./**/**images/*; 
do 
    echo "$(date --rfc-3339=seconds) [INFO]: Importing Docker image: $image"
    docker load --input $image
done

cd ./artifact

hostname_ips="$(hostname -I)"
local_ips=(${hostname_ips//' '/ })

export PROJECT_NAME=$1
export HOST_IP="${local_ips[0]}"

rm -f tmp-deploy.yaml tmp-tokens.yaml
cp --force deploy.yaml tmp-deploy.yaml
cp --force $2 tmp-tokens.yaml

./replacetokens "{{" "}}" tmp-tokens.yaml tmp-deploy.yaml $1-deploy.yaml

rm -r ./**tmp-*

sh -c "docker compose -f \"$1-deploy.yaml\" up -d --build"