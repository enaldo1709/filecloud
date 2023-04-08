#!/bin/bash

case $1 in
    "compose")
        sh -c "docker compose -f \"docker-compose.yaml\" up -d --build"
    ;;
    *)
        echo "$(date) [ERROR]: Invalid deployment tool" >&2
    ;;
esac