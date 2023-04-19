#!/bin/bash

mkdir -p $ROOT_FOLDER
sudo chown -R appuser:appuser $ROOT_FOLDER

sudo su appuser
sh -c "java ${JAVA_OPTS} -jar app.jar"
