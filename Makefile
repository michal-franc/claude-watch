.PHONY: check lint test test-python test-watch test-phone lint-python lint-watch lint-phone coverage coverage-python coverage-watch coverage-phone daemon-install daemon-branch daemon-reset

# Run all lints and tests (mirrors CI)
check: lint test

# All lints
lint: lint-python lint-watch lint-phone

# All tests
test: test-python test-watch test-phone

# All coverage reports
coverage: coverage-python coverage-watch coverage-phone

# Python
lint-python:
	ruff check *.py
	ruff format --check *.py

test-python:
	DEEPGRAM_API_KEY=dummy pytest

coverage-python:
	DEEPGRAM_API_KEY=dummy pytest --cov=. --cov-report=term-missing --cov-report=html:htmlcov --ignore=worktrees

# Watch app
lint-watch:
	cd watch-app && ./gradlew lint

test-watch:
	cd watch-app && ./gradlew test

coverage-watch:
	cd watch-app && ./gradlew jacocoTestReport
	@echo "Watch coverage report: watch-app/app/build/reports/jacoco/index.html"

# Phone app
lint-phone:
	cd phone-app && ./gradlew lint

test-phone:
	cd phone-app && ./gradlew test

coverage-phone:
	cd phone-app && ./gradlew jacocoTestReport
	@echo "Phone coverage report: phone-app/app/build/reports/jacoco/index.html"

daemon-install:
	sudo systemctl daemon-reload
	sudo systemctl restart claude-watch

# Switch service to current working directory
daemon-branch:
	sudo mkdir -p /etc/systemd/system/claude-watch.service.d
	printf '[Service]\nExecStart=\nExecStart=/home/mfranc/.local/share/mise/installs/python/3.14.0/bin/python3 %s/server.py /home/mfranc/Work/notes/bots/toadie\nWorkingDirectory=%s\n' \
		"$(CURDIR)" "$(CURDIR)" \
		| sudo tee /etc/systemd/system/claude-watch.service.d/branch.conf > /dev/null
	sudo systemctl daemon-reload
	sudo systemctl restart claude-watch
	@echo "Service now running from $(CURDIR) (branch: $$(git rev-parse --abbrev-ref HEAD))"

# Reset service back to master
daemon-reset:
	sudo rm -f /etc/systemd/system/claude-watch.service.d/branch.conf
	sudo systemctl daemon-reload
	sudo systemctl restart claude-watch
	@echo "Service reset to master"
