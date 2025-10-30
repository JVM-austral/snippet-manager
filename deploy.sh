#!/bin/bash

set -e

echo "Starting deployment..."


set -a
source .env
set +a



docker network inspect microservices-network >/dev/null 2>&1 || docker network create microservices-network

echo "Logging in to GitHub Container Registry..."
echo "$GHCR_PAT" | docker login ghcr.io -u "$GHCR_USER" --password-stdin

if [ $? -ne 0 ]; then
  echo "Error: Could not login to GHCR"
  exit 1
fi


echo "Pulling latest images..."
docker-compose pull

echo "Restarting services..."
docker-compose up -d --remove-orphans

echo "Deployment complete!"
echo ""
echo "Service status:"
docker-compose ps
