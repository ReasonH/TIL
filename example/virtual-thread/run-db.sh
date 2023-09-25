#!/bin/bash

docker run --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=1234 --platform linux/amd64 \
 -d --rm mysql:5 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
sleep 20
docker exec -i -t mysql mysql -p1234 -e 'create database exercise'