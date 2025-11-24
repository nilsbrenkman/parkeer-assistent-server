#!/bin/zsh

TAG="3.0.1"

docker build \
  --platform linux/amd64,linux/arm64 \
  --push \
  -t nilsbrenkman/parkeerassistent:"$TAG" .

docker push nilsbrenkman/parkeerassistent:"$TAG"
