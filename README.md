# CoTemplate

CoTemplate (can be spoken as *contemplate*) is a webservice to collaboratively combine multiple small templates
to one big template image.
It was created to help coordination of artworks for the [*Canvas Events*](https://fediverse.events/canvas).

Everyone can create a CoTemplate and then add images which are composed into one big template.
The images must be PNGs.\
Teams can be added (eiter by the owner or everyone) so that multiple groups of people can
manage their own template-items.

## Deployment

Note: CoTemplate must be served under the `/cotemplate/` subpath. To change this, rebuild the application.

### Container image

Use the `Dockerfile` to build a container-image with the CoTemplate server.
The build needs the following build-arguments to be set:

| Name            | Description                                                                           |
|-----------------|---------------------------------------------------------------------------------------|
| `GL_HOST`       | Host of the GitLab instance with the binaries (`projects.chocolatecakecodes.goip.de`) |
| `GL_PROJECT_ID` | Project-ID of CoTemplate in GitLab (`31`)                                             |
| `PACKAGE_VER`   | Version of CoTemplate                                                                 |

### Database

CoTemplate needs a Postgresql database to run.

Also there must be a directory for storage of the images available.

### Env variables

To run it, configure the following environment-variables
(all without a default value need to be set):

| Name                                  | Description                                                                                                                                                                       | Default Value |
|---------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| COTEMPLATE_DB_HOST                    | host of the DB-server                                                                                                                                                             |               |
| COTEMPLATE_DB_NAME                    | name of the DB                                                                                                                                                                    |               |
| COTEMPLATE_DB_USER                    | username for the DB                                                                                                                                                               |               |
| COTEMPLATE_DB_PASS                    | password for the DB                                                                                                                                                               |               |
| COTEMPLATE_DB_PORT                    | port of the DB-server                                                                                                                                                             | 5432          |
| COTEMPLATE_IMG_STORAGE                | path to the directory where images will be stored                                                                                                                                 |               |
| COTEMPLATE_SESSION_KEY                | key to use to encrypt the session-cookie                                                                                                                                          |               |
| COTEMPLATE_MAX_AGE                    | time after which templates will be automatically deleted (see [here](https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html#parse-java.lang.CharSequence-) for syntax) | P62D          |
| COTEMPLATE_ADMIN_PASS                 | password for admin-access (username is `admin`); if empty or `_` then the admin account will be disabled                                                                          | _             |
| COTEMPLATE_CACHE_TEMPLATE_MAX_ENTRIES | max number of composed templates to cache                                                                                                                                         | 128           |
| COTEMPLATE_CACHE_TEMPLATE_MAX_AGE     | time to cache composed templates for                                                                                                                                              | 1h            |

### Caddyfile

Here is an example Caddyfile to server CoTemplate.

```caddyfile
localhost {
    redir /cotemplate /cotemplate/ui/ permanent
    redir /cotemplate/ /cotemplate/ui/ permanent
    redir /cotemplate/ui /cotemplate/ui/ permanent
    reverse_proxy /cotemplate/* http://cotemplate.local:8080
}
```

## Build

To build the project, you need JDK 21.
Clone the repo and then run the `assemble` or `createTar` Gradle tasks (`./gradlew <task>`).

To change the subpath CoTemplate must be served, edit the `urlSubPath` variable in `build.gradle.kts`.
It must start with a slash but not end with a slash.

### Dev run

To run a local dev instance, use the `quarkusDev` Gradle task.
The required environment-variables must be set + `COTEMPLATE_PATH=/cotemplate` (or whatever value you wnt to use in `urlSubPath`).

Here is an example Caddyfile to server the backend and frontend unter the same domain (*https://localhost:8443/cotemplate*).

```caddyfile
localhost {
{
    http_port 8081
    https_port 8443
}

localhost {
    redir /cotemplate /cotemplate/ui/ permanent
    redir /cotemplate/ /cotemplate/ui/ permanent
    redir /cotemplate/ui /cotemplate/ui/ permanent
    reverse_proxy /cotemplate/* http://localhost:8080

    handle_path /cotemplate/ui/* {
        reverse_proxy http://localhost:5173
    }
    reverse_proxy /* http://localhost:5173
}
```
