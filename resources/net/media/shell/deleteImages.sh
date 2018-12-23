#!/bin/bash

if [ $# -ne 2 ]
then
  echo "2 arguments required."
  exit 1
fi

BASEURL="http://r.reports.mn"
REGISTRY=$1
shift
TAG=$1


DIGEST=$(curl -v -s  -H "Accept:application/vnd.docker.distribution.manifest.v2+json" $BASEURL/v2/$REGISTRY/manifests/$TAG 2>&1 |grep "< Docker-Content-Digest:"|awk '{print $3}')
digest=$(echo -n $DIGEST | grep -o "[0-9a-z:]*")

if [ -z $digest ]; then
  echo "Image:tag tuple not found in registry."
  exit 1
fi

echo "DIGEST: $digest"

# k - insecure , -v verbose prints headers, -s silent, -o output
echo "Connecting to registry..."
result=$(curl -H "Accept: application/vnd.docker.distribution.manifest.v2+json" -k -w "%{http_code}" -v -s -o /dev/null -X DELETE "$BASEURL/v2/$REGISTRY/manifests/$digest")

if [ $result -eq 202 ]; then
  echo "Deletion successful."
  exit 0
else
  echo "Deletion failed."
  exit 1
fi
