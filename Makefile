.PHONY: up
up:
	docker compose up -d

.PHONY: down
down:
	docker compose down

.PHONY: populate
populate:
	docker compose run populate-console

.PHONY: debug
debug:
	docker compose run debug-console
