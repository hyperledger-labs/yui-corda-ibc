# Minimal makefile for Sphinx documentation
#

# You can set these variables from the command line, and also
# from the environment for the first two.
SPHINXOPTS    ?=
SPHINXBUILD   ?= sphinx-build
SOURCEDIR     = .
BUILDDIR      = _build

# Options used by dockerXxx targets
DOCKER_TAG    ?= corda-ibc-docs
DOCKER_PORT   ?= 8080

# Put it first so that "make" without argument is like "make help".
help:
	@$(SPHINXBUILD) -M help "$(SOURCEDIR)" "$(BUILDDIR)" $(SPHINXOPTS) $(O)

open: html
	xdg-open _build/html/index.html

dockerBuild:
	docker build -t $(DOCKER_TAG) --build-arg PORT=$(DOCKER_PORT) .

dockerRun:
	docker run -d --rm --name $(DOCKER_TAG) -p $(DOCKER_PORT):$(DOCKER_PORT) $(DOCKER_TAG)

dockerOpen:
	until nc -z localhost 8080; do sleep 1; done
	xdg-open http://localhost:8080

dockerStop:
	docker stop $(DOCKER_TAG)

.PHONY: help Makefile
.PHONY: dockerBuild dockerRun dockerStop

# Catch-all target: route all unknown targets to Sphinx using the new
# "make mode" option.  $(O) is meant as a shortcut for $(SPHINXOPTS).
%: Makefile
	@$(SPHINXBUILD) -M $@ "$(SOURCEDIR)" "$(BUILDDIR)" $(SPHINXOPTS) $(O)
