#!/bin/sh

docker stop namebazaar-ganache || true
docker rm namebazaar-ganache || true

docker run --name=namebazaar-ganache \
    -p 8549:8549 \
    trufflesuite/ganache-cli:v6.12.2 -p 8549 --gasLimit 0xffe4e1c0
