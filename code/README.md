# DevOps Platform

## Repository Structure

- images/ - Images for requirements.md
- docker-compose.yml - Docker Compose Config
- certs/ - TLS Certificates
- redmine/repos/ - Storage location for Git Repos to register in Redmine
- authelia/configuration.yml - Authelia Configuration
- authelia/users.yml - Authelia User Configuration
- traefik/dynamic/tls.yml - Certificate configuration for Traefik
- jenkins-demo-pipeline.txt - Basic CI + Redmine API Demo
- local-build-result-pipeline.txt - Maven CI + Redmine API Demo
- mc-plugin-build-pipeline-demo.txt - Maven CI + Build Tools Demo
- redmine/repos/test/ - Repo for Build Tools Demo

## Architecture Overview

![alt text](/images/image-19.png)

Each container has its own volume for storing data and is connected to either the internal and/or external network, which are specified in the compose configuration file. To maintain good security practices, the development system itself and its database are hosted on an internal network to reduce the attack surface. Users who want to access the development system must first be authenticated through Authelia, which is hosted on the external network, and then are routed to the system via Traefik. 

## A Server (Traefik)

### Location
Container defined in `docker-compose.yml`
Files stored in `traefik/`

### Rationale
Traefik serves as the bridge between the internal and external networks. This reduces the exposure of internal services by keeping them unreachable until authentication is obtained.

## A Database (PostgreSQL and Redis)

### Location
Container defined in `docker-compose.yml`

### Rationale

Two separate database applications were chosen with specific performance goals in mind. Redis is an in-memory database designed for rapid high-demand access, making it a good fit for use with Authelia and Traefik, where high availability is critical. PostgreSQL is designed for persistence, which makes it better suited for storing data from the Redmine container.

## Web Frameworks (Redmine and Jenkins UI)

### Location
Containers defined in `docker-compose.yml`
Accessible at `redmine.localtest.me`
Accessible at `jenkins.localtest.me`

### Visuals
![alt text](/images/image-4.png)
### Rationale
When implementing an environment for developers to create issues that track progress and create documentation, Redmine was chosen as its built-in features fit the scope requirements well. 

### Further Extension

Redmine could be further extended by enabling integration with a Git repository.

## Continuous Integration (Jenkins)

### Location
Container defined in `docker-compose.yml`
Accessible at `jenkins.localtest.me`

### Visuals
![alt text](/images/image-20.png)

### Rationale
Jenkins is an open-source CI pipeline tool used to validate code changes through automated verification tasks. It is versatile and can be used for validating application builds and system deployment from various languages and deployment solutions.

### Further Extension

The functionality of Jenkins could be expanded with pipeline tasks that further integrate the Redmine API.

## Deployment, Scalability and Maintainability (Docker / Docker Compose)

### Location
Docker is installed on the system itself, and uses docker-compose.yml and the command `docker compose` for deployment configuration.

### Visuals

![alt text](/images/image-5.png)
![alt text](/images/image-3.png)

### Rationale

Docker was chosen as the deployment platform over other options because it provides reliable reproducibility between systems. Creating Docker containers is streamlined through the use of Docker Compose, where defining and deploying containers can be done from a configuration file. 

### Further Extension

A platform that focuses more on Horizontal scaling could opt to use Kubernetes instead.

## Observability (Netdata)

### Location
Container defined in `docker-compose.yml`.

### Visuals
![alt text](/images/image-17.png)

### Rationale

Netdata was used for process monitoring and system observability. It monitors containers in the external and internal networks. Netdata was specifically chosen for its ease of deployment and customisability.
### Further Extension

Netdata could be extended with alert notifications that send an email or trigger a webhook in a messaging platform like Discord.

## Security

Authelia and Traefik

### Location
Container defined in `docker-compose.yml`, manual configuration files are stored in `authelia/` and `traefik/dynamic/`.

Authelia is accessible at `authelia.localtest.me`

### Visuals

![alt text](/images/image-2.png)

### Rationale

Authelia was implemented as the authentication gateway for centralised user access control, and Traefik was chosen as a reverse proxy to sit between the two networks and route traffic securely.

### Further Extension

A future development could add a VPN server as a method to securely tunnel to the system from the internet, rather than exposing Authelia / Traefik to the open internet.

## Setup Instructions

### Install Docker

Docker can be installed using the guide provided in the Docker documentation. (https://docs.docker.com/engine/install/)

### Edit .env

Make sure to change the environment variables to freshly generated passwords.

### Configuring health checks
Health checks can be created by defining a `test: ["CMD-SHELL", "(shell command) || exit 1"]` that executes a shell command successfully or exits with code 1 if it fails.

#### Using wget
Some containers only have `wget`, which can be used to poll an endpoint to check if the service is accessible.

`test: ["CMD-SHELL", "wget -qO- http://localhost:3000/login || exit 1"]`

`-q` reduces the output of the command by enabling quiet mode.

`-O-` writes the response body of the command to standard output instead of a file.

`|| exit 1` fails the health check if the request fails.

#### Using curl

Some containers don't contain wget, so curl must be used to poll the service as the health check.

`test: ["CMD-SHELL", "curl -fsS http://localhost:3000/login >/dev/null || exit 1"]`

`-f` fails on HTTP errors (400, 404, 500)

`-s` enables silent mode and disables terminal output.

`-S` enables displaying errors in silent mode.

(these are also used when making HTTP requests)

curl outputs a file, this can be immediately deleted by outputting it to `/dev/null` with `>`.

Outputting the failure code is the same as a wget healthcheck and uses `|| exit 1`.

### Generate HTTPS Certificates 

#### Local Generation (without a domain)

Localhost certificates can be created using OpenSSL.

If it isn't already there, create the certs directory in the root directory. 
`mkdir certs`

Then run this command : 
```
openssl req -x509 -newkey rsa:2048 -sha256 -days 365 \
  -nodes \
  -keyout certs/local.key \
  -out certs/local.crt \
  -subj "/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"
```

Make sure that Traefik is using the certificates in `traefik/dynamic/tls.yml`

![alt text](/images/image-21.png)

#### Using Let's Encrypt (with a domain)

To generate a certificate for a registered domain, configure the required additional flags in the Traefik container.

Set this email to match your domain.
`- --certificatesresolvers.le.acme.email=you@example.com`

Specify the location for the ACME file and enable HTTP challenge.
`- --certificatesresolvers.le.acme.storage=/letsencrypt/acme.json`
`- --certificatesresolvers.le.acme.httpchallenge=true`
`- --certificatesresolvers.le.acme.httpchallenge.entrypoint=web`

Mount Let's Encrypt's storage to a folder by adding it to the volumes in the Traefik container.
`- ./letsencrypt:/letsencrypt`

Specify this label in containers that you want to resolve with the generated certificate.

`- traefik.http.routers.(container name).tls.certresolver=le`

### Deploy with Docker Compose
The system can be deployed by using the command `docker compose up` (`-d` lets you run the system in the background and `--force-recreate` lets you force the recreation of containers)

## Authelia Configuration Setup

Set the server address and port.
![alt text](/images/image-26.png)

Configure the authentication backend to use the admin credentials in `users.yml`. (the default password is "123")
![alt text](/images/image-25.png)

Configure Authelia's local storage.
![alt text](/images/image-27.png)

![](/images/image.png)
Change `domain` to the domain you're using, or keep it as localtest.me for testing purposes. Adding the redis option stores these sessions in Redis.

![alt text](/images/image-28.png)

users.yml must also be configured to include an admin account, by creating a user and adding them to the `admins` group The configuration uses Argon2 hashes for password input, hashing a password with Argon2 can be done with this command 
```
docker run --rm -it authelia/authelia:latest authelia crypto hash generate argon2
```

![alt text](/images/image-1.png)

## Redmine Setup

Log into Redmine by using the default username and password `admin`, you'll be prompted to change the password.

After setup, a new project can be created, though you won't be able to create issues yet.

![alt text](/images/image-15.png)

To be able to create issues, trackers, issue statuses and priorities must be set. This can be configured in the administrator menus.

![](/images/image-9.png)
![alt text](/images/image-11.png)
![alt text](/images/image-12.png)
![alt text](/images/image-13.png)

You can configure a locally mounted Git repository by going to the repository tab of the project settings menu.

![alt text](/images/image-30.png)
Click on New Repository
![alt text](/images/image-29.png)

Open the SCM selector and select Git.
![alt text](/images/image-31.png)

Mount a repository folder with `/repos/(repository folder name)`
![alt text](/images/image-32.png)

Your repository should successfully mount, if not, check your file/folder permissions as Redmine may not have the correct permissions.

## Jenkins Setup

Jenkins requires the automatically generated password to proceed with setup, it appears in the terminal (if you didn't use `-d`) 

If you used `-d`, you can get the password by running this command, which just gets the log output of the container : 

`docker logs jenkins --tail 20`

![alt text](/images/image-18.png)

If you can't find the password in the log output, you can use this command instead.

`docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword`

Input this password, change it and proceed with the regular setup, which only involves installing the base plugins and restarting Jenkins.

![alt text](/images/image-16.png)

After restarting, Jenkins is now set up and can run pipeline tasks. In the main menu, a pipeline task can be created by clicking "New Item"
![alt text](/images/image-22.png)

The pipeline script editor can be opened by first clicking "Configure"
![alt text](/images/image-24.png)

Then scrolling down to "Pipeline" to add code.
![alt text](/images/image-23.png)

### API Setup

Enable the Redmine REST API by toggling it in the Manage Jenkins menu.

![alt text](/images/image-14.png)
![alt text](/images/image-6.png)

Store the Redmine API Key in the credentials section of the Jenkins settings menu by creating a secret text.

![alt text](/images/image-8.png)
![alt text](/images/image-7.png)

This credential can then be accessed in pipeline tasks to authenticate API requests sent from Jenkins to Redmine, examples are included in `jenkins-demo-pipeline.txt`, `mc-plugin-build-pipeline-demo.txt` and `maven-demo-pipeline.txt`.

### Using curl for requests

Curl can send HTTP POST/PUT requests to send data to APIs.

```
sh """
    curl -v -X PUT \
    -H "X-Redmine-API-Key: ${REDMINE_KEY}" \
    -H "Content-Type: application/json" \
    -d '${payload}' \
    "http://redmine:3000/issues/3.json"
"""
```

This example uses `-H` to add the Redmine API key to the HTTP header, along with the Content-Type header, and then adds the data using `-d`, with the last parameter specifying the destination of the request. `-v` is used to enable verbose mode for better debugging.

Curl can also send GET requests to receive data from APIs. 
```
sh 'curl -sS -H "X-Redmine-API-Key: $REDMINE_KEY" "http://redmine:3000/issues.json"'
```
For example, this GET request receives a list of every issue stored in Redmine.


### Configuring Maven
Maven can be configured (and then reproducibly defined as a tool in pipelines) by going into the Tools menu in the Manage Jenkins menu.

![alt text](/images/image-34.png)

Configure a maven installation and give it a name, this name is used to call the tool in a pipeline script. (The name must match the name used in the pipeline.)

![alt text](/images/image-35.png)

### Creating a Pipeline 

Pipelines are first defined with the `pipeline{}` field which includes the pipeline tools, variables, parameters and stages. 

#### Specifying tools

Pipeline tools can be defined at the start of a script by using the `tools{}` field.
![alt text](/images/image-36.png)

#### Setting environment variables

Environment variables can be defined using the `environment{}` field.
![alt text](/images/image-37.png)

#### Build Stages

Individual stages can be defined by creating the `stages{}` field, and then defining a stage with `stage("Stage Name"){}`. These stages contain a `steps{}` field that execute commands.
![alt text](/images/image-38.png)

You can also use a `post{}` stage to specify code which runs afterwards. The `always{}` field can be used in any `stage{}`.
![alt text](/images/image-39.png)

Stages can also be conditional with the `when{}` field.
![alt text](/images/image-40.png)

After executing commands and determining the build status, the `stages{}` field can contain its own `post{}` field with handlers for each status.
![alt text](/images/image-41.png)

#### Workspace Cleanup
Pipelines need their workspaces to be cleaned before and after use or old files can potentially affect builds. This can be done with `cleanWs(deleteDirs : true/false, disableDeferredWipeout: true/false)`.

#### Using Credentials / Secret Text

Secret text and credentials can be used in execution using `withCredentials([string(credentialsId: 'credential_id', variable: 'variable name')])`, credentialsID must be the id you set in the Manage Jenkins menu. `variable` defines the environment variable name of the secret.
![alt text](/images/image-42.png)

#### Using Shell Commands

Shell commands can be used by using `sh ''' (shell commands) '''` ![alt text](/images/image-43.png)

#### Cloning a Repository
Repositories can be mounted from the locally mounted repos folder and shell commands can be used to clone and checkout the branch.

`git clone "$REPO_PATH" .` clones the repo into the workspace directory.

The branch can then be checked out.
 `git checkout "$REPO_BRANCH"`

#### Moving a build artifact for deployment
After a build is successful, the jar can be copied to a directory for deployment.

The artifact can be copied as an environment variable by listing the build directory and getting the first artifact.
`JAR="$(ls -1 target/*.jar | head -n 1)"`

This can then be copied to a deployment directory.
`cp "$JAR" "(DIRECTORY)"`

#### Building additional dependencies
Some pipelines require extra tools, this can be done by installing and building them in the pipeline using shell commands. 

This example supports containers that run on Alpine and Debian distros, it may vary if a container uses a different package manager.

```
if command -v apt-get >/dev/null 2>&1; then
  apt-get update
  DEBIAN_FRONTEND=noninteractive apt-get install -y git gcc make libc6-dev
elif command -v apk >/dev/null 2>&1; then
  apk add --no-cache git build-base
fi
```
This installs the build tools required for compiling most programs, it can be adapted to install more than `gcc`, `make` and `libc6-dev`.

The dependency can then be cloned from GitHub and built in the workspace, for example the demo clones and builds mcrcon to be able to restart the Minecraft server using RCON.

Make sure to clean up any previous pipeline usage.
`rm -rf .mcrcon-src`

Then clone the code from GitHub.
`git clone https://github.com/Tiiffi/mcrcon.git .mcrcon-src`

The build tools installed can then be used to compile the code into an executable.

`gcc -O2 -o .mcrcon-src/mcrcon .mcrcon-src/mcrcon.c`

This executable must be marked as an executable, and then can be added to PATH so it can be called in the terminal.

`chmod +x .mcrcon-src/mcrcon`
This makes the program executable.

`export PATH="$PWD/.mcrcon-src:$PATH"`
It can then be exported to PATH and executed in a terminal as a command. `$PWD` defines the current work directory and `$PATH` defines the current PATH in the pipeline.

This can then be used in pipelines that require additional commands.
