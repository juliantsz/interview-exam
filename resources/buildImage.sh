## !/bin/sh
imageName=$1
imageVersion=$2

docker image build --tag ${imageName}:${imageVersion} .
docker image push ${imageName}:${imageVersion}
docker rmi ${imageName}:${imageVersion}