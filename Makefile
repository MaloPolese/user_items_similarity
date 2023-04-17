.PHONY: up-neo4j
up-neo4j:
	docker compose --profile neo4j up -d

.PHONY: populate-neo4j
populate-neo4j:
	docker compose run neo4j-populate

.PHONY: debug-neo4j
debug-neo4j:
	docker compose run neo4j-debug-console

.PHONY: up-in-memory
up-in-memory:
	docker compose --profile in-memory up -d

.PHONY: populate-in-memory
populate-in-memory:
	docker compose run in-memory-populate

.PHONY: debug-in-memory
debug-in-memory:
	docker compose run in-memory-debug-console

.PHONY: clean
clean:
	docker compose --profile downable down