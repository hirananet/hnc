# hnc
Hirana network connection (Bouncer connected to Anope mysql)

## Local Env

In order to start local environment, you need to run a redis server, you can do that using docker with this command:

``
docker run --name redisHNC -p 6379:6379 -d redis redis-server --requirepass local
``