#!/bin/zsh

TAG="3.1.9"

docker buildx build \
  --platform linux/amd64,linux/arm64 \
  --load \
  -t nilsbrenkman/parkeerassistent:"$TAG" .

docker push nilsbrenkman/parkeerassistent:"$TAG"
