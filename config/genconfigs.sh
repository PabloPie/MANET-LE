#!/bin/bash
for scope in {50..250..10}
do
	sed -e 's/$ALGO/VKT04Dynamique/g' -e 's/$SCOPE/'$scope'/g' config.tpl > config-vkt04-$scope.txt
	sed -e 's/$ALGO/GlobalViewElection/g' -e 's/$SCOPE/'$scope'/g' config.tpl > config-globalview-$scope.txt
done
