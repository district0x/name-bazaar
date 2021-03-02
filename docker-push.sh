#!/bin/bash

#--- ARGS

BUILD_ENV=$1  # dev / qa / prod
GITHUB=$2  # your github ssh private key filepath to download git+ssh deps

#--- FUNCTIONS

function build {
  {
    NAME=$1
    BUILD_ENV=$2
    TAG=$(git log -1 --pretty=%h)
    IMG=$NAME:$TAG

    SERVICE=$(echo $NAME | cut -d "-" -f 2)

    echo "============================================="
    echo  "["$BUILD_ENV"] ["$SERVICE"] Buidling: "$IMG""
    echo "============================================="

    DOCKER_BUILDKIT=1 docker build -t $IMG -f docker-builds/$SERVICE/Dockerfile . --ssh github=$GITHUB

    case $BUILD_ENV in
      "dev")
        # dev images are tagged as `dev`
        docker tag $IMG $NAME:dev
        ;;
      "qa")
        # qa images are tagged as `latest`
        docker tag $IMG $NAME:latest
        ;;
      "prod")
        # prod images are tagged as `release`
        docker tag $IMG $NAME:release
        ;;
      *)
        echo "ERROR: don't know what to do with BUILD_ENV: "$BUILD_ENV""
        exit 1
        ;;
    esac

  } || {
    echo "EXCEPTION WHEN BUIDLING "$IMG""
    exit 1
  }
}

function push {
  NAME=$1
  echo "Pushing: " $NAME
  docker push $NAME
}

function login {
  echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
}

#--- EXECUTE

login

images=(
  district0x/namebazaar-server
  district0x/namebazaar-ui
)

for i in "${images[@]}"; do
  (
    build $i $BUILD_ENV
    push $i
  )

done # END: i loop

exit $?
