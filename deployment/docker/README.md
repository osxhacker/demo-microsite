Docker Compose
==============


This document details the steps needed in order to run `microsite` using [Docker Compose](https://docs.docker.com/compose/) and the [Colima](https://github.com/abiosoft/colima) container runtime.

Note that what is documented here has been tested on my development machine but may not be the only way to successfully run `microsite` and its dependencies.


## Install Required Components

### Docker

Install [Docker](https://github.com/docker/cli) either with your favourite package manager or as described in the docker site.  Using [MacPorts](https://www.macports.org/):

```
sudo port install docker
```


### Docker Compose

Install [Docker Compose](https://github.com/docker/compose) either with your favourite package manager or as described in the docker compose [documentation](https://docs.docker.com/compose/).  Using [MacPorts](https://www.macports.org/):

```
sudo port install docker-compose
```

### Colima

This step is only needed if you want to use [Colima](https://github.com/abiosoft/colima) instead of [Docker Desktop](https://docs.docker.com/desktop/).  A good article discussing other container options can be found [here](https://jacobtomlinson.dev/posts/2022/docker-desktop-for-mac-alternatives-for-developers/).

Install [Colima](https://github.com/abiosoft/colima) as per its instructions or using your favourite package manager.  With [MacPorts](https://www.macports.org/):

```
sudo port install colima
```

Next, provision a `colima` VM:

```
colima start --cpu 6 --memory 14 --disk 20
```

If you intend on having a large amount of data, increase the `disk` value from 10 Gb to what you think you will need.  While `cpu` and `memory` can be changed later, the amount of disk space cannot without reinitializing `colima`.


## Build Frontends and Services

From the git root, build the docker definitions with `sbt`:

```
cd $(git rev-parse --show-toplevel) && sbt recompile-all docker
```

Feel free to inspect the generated and predefined Docker files if desired:

```
cd $(git rev-parse --show-toplevel) && find . -name Dockerfile
```

To run the integration tests, the repositories must be running (see below) before executing:

```
cd $(git rev-parse --show-toplevel) && sbt run-all-it
```


## Start Repositories

Bring up the [repositories](./repositories) with [Docker Compose](https://github.com/docker/compose) by:

```
cd $(git rev-parse --show-toplevel)/deployment/docker/repositories &&
	docker-compose up -d
```

This will start PostgreSQL and MongoDB within the Docker environment previously installed.  Verifying they are running can be done via:

```
cd $(git rev-parse --show-toplevel)/deployment/docker/repositories &&
	docker-compose ps
```

To connect to PostgreSQL with `psql` (use the same `user` and `password` as found in `application.conf`):

```
docker run -it --rm --network repositories_lan postgres:14.5 \
	psql -h postgres -U storage_facility
```


## Start Services

Bring up the [services](./services) with [Docker Compose](https://github.com/docker/compose) by:

```
cd $(git rev-parse --show-toplevel)/deployment/docker/services &&
	docker-compose up -d
```

This will start the `demo` microservices.  Verifying they are running can be done via:

```
cd $(git rev-parse --show-toplevel)/deployment/docker/services &&
	docker-compose ps
```

If one or more services are updated, be sure to run this command so that the images are updated:

```
cd $(git rev-parse --show-toplevel)/deployment/docker/services &&
	docker-compose build
```


## Start Frontends

Bring up the [frontends](./frontends) with [Docker Compose](https://github.com/docker/compose) by:

```
cd $(git rev-parse --show-toplevel)/deployment/docker/frontends &&
	docker-compose up -d
```

This will start the `demo` microsites.  Verifying they are running can be done via:

```
cd $(git rev-parse --show-toplevel)/deployment/docker/frontends &&
	docker-compose ps
```

If one or more microsites are updated, be sure to run this command so that the images are updated:

```
cd $(git rev-parse --show-toplevel)/deployment/docker/frontends &&
	docker-compose build
```

Once all subsystems are successfully started, the microsite entrypoint should be available at [the site home page](http://localhost:9080/).


## Start Operation Services

Bring up the [operations](./operations) instances with [Docker Compose](https://github.com/docker/compose) by:

```
cd $(git rev-parse --show-toplevel)/deployment/docker/operations &&
	docker-compose up -d
```

This will start the `demo` operational (monitoring and others) services.  Verifying they are running can be done via:

```
cd $(git rev-parse --show-toplevel)/deployment/docker/operations &&
	docker-compose ps
```

To help configure [Grafana](https://grafana.com/docs/grafana/latest/setup-grafana/installation/docker/), dashboards capable of being imported are in the [dashboards](./operations/grafana/dashboards) directory.  They are not required for successful use and are provided only as examples.


