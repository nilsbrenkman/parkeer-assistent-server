#!/bin/zsh

TAG="2.0.7"

docker buildx build \
  --builder desktop-remote \
  --platform linux/amd64,linux/arm64 \
  --push \
  -t nilsbrenkman/parkeerassistent:"$TAG" .

docker pull nilsbrenkman/parkeerassistent:"$TAG"
