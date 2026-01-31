.PHONY: daemon-install

daemon-install:
	sudo systemctl daemon-reload
	sudo systemctl restart claude-watch
