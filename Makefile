# Makefile für einfache Befehle

.PHONY: help build start stop logs clean shell

help:
	@echo "Verfügbare Befehle:"
	@echo "  make build   - Build Docker-Images"
	@echo "  make start   - Start Container"
	@echo "  make stop    - Stop Container"
	@echo "  make logs    - Show Logs"
	@echo "  make clean   - Delete Container + Volumes"
	@echo "  make shell   - Open Shell inside API-Container"

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