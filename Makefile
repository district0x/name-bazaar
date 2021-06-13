# VARS
COMMIT_ID = $(shell git rev-parse --short HEAD)
PROJECT_NAME = namebazaar

DEV_IMAGE = namebazaar_base:local
SHELL=bash
DOCKER_VOL_PARAMS = -v ${PWD}:/build/ -v ${PROJECT_NAME}_vol_m2_cache:/root/.m2 -v ${PROJECT_NAME}_vol_target:/build/target -v ${PROJECT_NAME}_vol_server_tests:/build/server-tests -v ${PROJECT_NAME}_vol_node_modules:/build/node_modules --workdir /build
DOCKER_NET_PARAMS = --network=${PROJECT_NAME}_dev_network
.PHONY: help

# HELP
help: ## Print help
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

.DEFAULT_GOAL := help

# DOCKER BUILDS

# DEV image
dev-image: ## Builds dev image
	docker build -t ${DEV_IMAGE} -f docker-builds/base/Dockerfile .

dev-image-no-cache: ## Builds dev image (no cache)
	docker build --no-cache -t ${DEV_IMAGE} -f docker-builds/base/Dockerfile .

# All images
build-images: dev-image ## Build all containers in docker-compose file
	COMPOSE_DOCKER_CLI_BUILD=1 docker-compose -p ${PROJECT_NAME} build --parallel

build-images-no-cache: # Build base docker image with node11.14, yarn, clojure, lein, truffle
	COMPOSE_DOCKER_CLI_BUILD=1 docker-compose -p ${PROJECT_NAME} build --parallel --pull --no-cache

# RUN CONTAINERS
init:  ## Initiate volumes, networks build containers
	docker-compose -p ${PROJECT_NAME} up --no-start

start-containers: ## (Re)Build and start containers
	docker-compose -p ${PROJECT_NAME} up -d

run-dev-shell: dev-image ## Start dev container in interactive mode
	docker run -ti --rm --entrypoint="" ${DOCKER_NET_PARAMS} ${DOCKER_VOL_PARAMS} ${DEV_IMAGE} bash

check-containers: ## Show docker-compose ps for given project
	docker-compose -p ${PROJECT_NAME} ps

clear-all: ## Remove containers, networks and volumes for this project
	docker-compose -p ${PROJECT_NAME} down

# TEST CODE SERVER
deps-npm: dev-image ## Install/update npm deps
	docker run -t --rm ${DOCKER_NET_PARAMS} ${DOCKER_VOL_PARAMS} ${DEV_IMAGE} bash -c "lein npm install"

truffle-compile: deps-npm ## Compile contracts
	docker run -t --rm ${DOCKER_NET_PARAMS} ${DOCKER_VOL_PARAMS} ${DEV_IMAGE} bash -c "truffle compile"

test-server: deps-npm  ## Run Backend (server) tests
	docker run -t --rm ${DOCKER_NET_PARAMS} ${DOCKER_VOL_PARAMS} ${DEV_IMAGE}  bash -c "lein cljsbuild once server-tests"

# TEST CODE UI
tests-frontend: deps-npm ## Run Frontend (browser) tests
	docker run -t --rm ${DOCKER_NET_PARAMS} ${DOCKER_VOL_PARAMS} ${DEV_IMAGE}  bash -c "lein npm run cypress-open"

# SHORTCUTS
build: build-images ## Build all containers (alias for docker-build)
up: start-containers ## Start dev environment (alias for start-containers)
rm: clear-all ## Remove containers, networks and volumes (alias for clear-all)
ps: check-containers ## Show docker-compose ps for given project (alias for check-containers)
exec: run-dev-shell ## Show docker-compose ps for given project (alias for check-containers)
