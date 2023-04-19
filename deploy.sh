#!/bin/bash

log() {
    case $1 in 
        1) 
            echo "$(date --rfc-3339=seconds) [ERROR]: $2" >&2
        ;;
        2)
            echo "$(date --rfc-3339=seconds) [WARNING]: $2" >&2
        ;;
        *)
            echo "$(date --rfc-3339=seconds) [INFO]: $1" >&1
        ;;
    esac
}

replacetokens -v &>/dev/null
if [[ $? -ne 0 ]]; then
    log  1 "Rquired replace tokens tool not installed -> https://github.com/enaldo1709/replace-tokens" 
    return 1
fi

project_name=$1

deploy_docker_compose() {
    case $2 in
        "up")
            if [ $3 -eq "build" ]; then
                /bin/bash build-images.sh $1
            fi
            rm -f tmp-deploy.yaml tmp-tokens.yaml
            cp --force docker-compose.yaml tmp-deploy.yaml
            cp --force tokenfile.yaml tmp-tokens.yaml

            replacetokens "{{" "}}" tmp-tokens.yaml tmp-deploy.yaml $1-tmp-deploy.yaml

            sh -c "docker compose -f \"$1-tmp-deploy.yaml\" up -d --build"
        ;;
        "start")
            sh -c "docker compose --file 'tmp-deploy.yaml' start"
        ;;
        "restart")
            sh -c "docker compose --file 'tmp-deploy.yaml' restart"
        ;;
        "stop")
            sh -c "docker compose --file 'tmp-deploy.yaml' stop"
        ;;
        "down")
            sh -c "docker compose --file '$1-tmp-deploy.yaml' down"
            rm -r ./**tmp-*
        ;;
        *)
            echo "$(date) [ERROR]: Invalid operation" >&2
        ;;
    esac
}

install_remote() {
    sh ./build-images.sh $1
    mkdir -p ./artifact/images

    operation=$2
    host=$3
    username=$4
    password=$(echo $5 | base64 -d)

    ftp_url="ftp://$username:$password@$host/"
    
    images=$(docker images --format="{{.Repository}}:{{.Tag}}" | grep $project_name)
    
    image_array=(${images//' '/ })

    for i in "${!image_array[@]}"
    do
        splitted_name=(${image_array[i]//':'/ })
        image_name="${splitted_name[0]}"
        tag="${splitted_name[1]}"
        
        sh -c "docker save -o artifact/images/$image_name-$tag.tar ${image_array[i]}" 
    done

    replace_tokens_path="$(whereis replacetokens)"
    replace_tokens_path=(${replace_tokens_path//': '/ })
    replace_tokens_path="${replace_tokens_path[1]}"

    uuidv4="$(cat /proc/sys/kernel/random/uuid)"
    suff=(${uuidv4//'-'/ })
    suff="${suff[0]}"

    cp -f ./init-remote.sh ./artifact/init.sh
    cp -f $replace_tokens_path ./artifact/
    
    cp --force docker-compose.yaml ./artifact/deploy.yaml
    cp --force $6 ./artifact/

    zip -r artifact-$suff.zip artifact

    ssh $username@$host sh -c "docker compose --file 'artifact/$1-deploy.yaml' down"
    ssh $username@$host rm -r ./artifact ./artifact-*.zip

    ftp -u $ftp_url artifact-$suff.zip

    rm -r ./artifact ./artifact-*.zip

    ssh $username@$host unzip artifact-$suff.zip
    ssh $username@$host chmod a+x ./artifact/init.sh
    ssh $username@$host ./artifact/init.sh $1 $6
}

case $2 in
    "compose")
        deploy_docker_compose $1 $3
    ;;
    "install-remote")
        install_remote $1 $3 $4 $5 $6 $7
    ;;
    *)
        echo "$(date) [ERROR]: Invalid deployment tool" >&2
    ;;
esac