# Makefile for common commands

.PHONY: help build start stop logs clean shell

help:
	@echo "Available commands:"
	@echo "  make build   - Build Docker images"
	@echo "  make start   - Start containers"
	@echo "  make stop    - Stop containers"
	@echo "  make logs    - Show logs"
	@echo "  make clean   - Delete containers + volumes"
	@echo "  make shell   - Open shell inside API container"

build:
	docker-compose build

start:
	docker-compose up -d

stop:
	docker-compose down

logs:
	docker-compose logs -f

clean:
	docker-compose down -v
	rm -rf target/

shell:
	docker-compose exec java-api sh