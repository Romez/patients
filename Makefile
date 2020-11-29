bash:
	docker-compose run -p 127.0.0.1:7002:7002 app bash

build:
	docker-compose build

down:
	docker-compose down

run:
	docker-compose run -p 7002:7002 -p 3449:3449 app lein figwheel

prod:
	docker-compose run app lein ring uberjar

psql:
	docker-compose exec db psql -U roman

compose-migrate:
	docker-compose run app lein migrate

compose-rollback:
	docker-compose run app lein rollback

test:
	docker-compose run app lein test
