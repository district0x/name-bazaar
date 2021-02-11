#!/bin/sh

docker stop namebazaar-ganache || true
docker rm namebazaar-ganache || true

docker run --name=namebazaar-ganache \
    -p 8549:8549 \
    trufflesuite/ganache-cli:v6.12.1 -p 8549
