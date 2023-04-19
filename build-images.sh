#!/bin/bash

project_prefix="filecloud"

if [ $1 -ne "" ]; then 
    project_prefix=$1
fi

# Building service
echo "$(date) [INFO]: Building service...  " >&1
cd ./service
sh -c "./gradlew clean build"

if [ $? -ne 0 ]; then
    echo "$(date) [ERROR]: Build failed... exiting  " >&2
    return 1
fi

# Building docker images
mv ./build/*.jar ./deployment/app.jar
docker_version=$(sh -c "docker -v")
if [ $? -ne 0 ]; then 
    echo "$(date) [ERROR]: Docker client not found." >&2
    return 1
fi

echo "$(date) [INFO]: Building service image -> Docker version: $docker_version" >&1
sh -c "docker build --pull --rm -f \"deployment/Dockerfile\" -t $project_prefix-service:latest \"deployment\""
rm ./deployment/app.jar


echo "$(date) [INFO]: Build complete." >&1
