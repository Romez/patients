bash:
	docker-compose run app bash

build:
	docker-compose build

run:
	docker-compose run -p 7002:7002 -p 3449:3449 app lein figwheel

prod:
	docker-compose run lein ring uberjar
