# Connect2id Server Login / Logout UI

A server-side rendered login and consent page for a [Connect2id server](https://connect2id.com/products/server).
The application uses Spring Boot 4+, Java 21+, JTE templates, Bootstrap 5, and minimal browser JavaScript.

It supports:

- OpenID Connect authentication and consent flows.
- RP-initiated logout through a server-rendered logout page.
- RP-only and OpenID provider-wide logout choices.
- Front-channel logout notifications and post-logout redirection.
- Integration with the Connect2id server via the [logout session web API](https://connect2id.com/products/server/docs/integration/logout-session).
- Supports LDAP authentication through the Connect2id LdapAuth JSON-RPC 2.0 service.
- Custom `UserAuthenticator` implementations through the existing SPI.
- Subject session verification and logout through the Connect2id APIs.
- Query, fragment, and `form_post` OpenID Connect response modes.
- User avatar rendering from the OIDC `picture` claim when available.
- Spring Boot Actuator health checks.

The project has no client-side build step and does not use NodeJS tooling.

## Quick start

1. Configure the Connect2id and LdapAuth endpoints and credentials in
   `src/main/resources/config/application.yml` or an active profile.
2. Run the development profile:

   ```bash
   ./mvnw spring-boot:run
   ```

3. Open `http://localhost:8000` or configure that URL as the Connect2id authorization endpoint.

The development profile loads templates from `src/main/jte` on demand and disables static resource
caching. Changes to JTE templates and CSS are visible after a page refresh without restarting the
application.

## Build and run

Build the executable WAR:

```bash
./mvnw -Pprod clean package
```

Run it with the embedded Tomcat:

```bash
java -jar target/c2id-login-*.war
```

The same WAR can be deployed to an external Tomcat server. JTE templates are precompiled during
the Maven build. The application targets Java 21.

Run tests with:

```bash
./mvnw clean test
```

## Configuration

Configuration files are in `src/main/resources/config`:

- `application.yml` — default settings.
- `application-dev.yml` — development server on port 8000 and JTE hot reload.
- `application-prod.yml` — production server on port 8080 and precompiled JTE templates.

Important settings include:

```yaml
c2id:
  endpoint: https://demo.c2id.com
  cookieName: sid
  authenticatorClass: LdapUserAuthenticator
  authz:
    resourcePath: /authz-sessions/rest/v3
    accessToken: <token>
  session:
    resourcePath: /session-store/rest/v2
    accessToken: <token>
  ldapAuth:
    endpoint: https://demo.c2id.com/ldapauth/
    apiKey: <api-key>
  logout:
    resourcePath: /logout-sessions/rest/v1
    accessToken: <token>
    defaultPostLogoutRedirectUri: https://example.com/
```

Do not commit production tokens or API keys. Prefer environment variables or an external configuration source.

### Application properties

These properties are bound by `ApplicationProperties` with the `c2id` prefix.

| Property | Description | Default |
| --- | --- | --- |
| `c2id.endpoint` | Base URL of the Connect2id server. | `http://127.0.0.1:8080/c2id` |
| `c2id.cookieName` | Name of the Connect2id subject-session cookie. | `sid` |
| `c2id.authenticatorClass` | Simple class name of a `UserAuthenticator` loaded through `ServiceLoader`. | Empty; uses `LdapUserAuthenticator` |
| `c2id.authz.resourcePath` | Authorisation-session API path. | `/authz-sessions/rest/v3` |
| `c2id.authz.accessToken` | Long-lived bearer token for the authorisation-session API. | Required secret |
| `c2id.authz.longLived` | Requests long-lived tokens during authentication. | `false` (`true` in DEV) |
| `c2id.session.resourcePath` | Subject-session store API path. | `/session-store/rest/v2` |
| `c2id.session.accessToken` | Bearer token for the subject-session store API. | Required secret |
| `c2id.ldapAuth.endpoint` | LdapAuth JSON-RPC endpoint. | `http://localhost:8080/ldapauth/` |
| `c2id.ldapAuth.apiKey` | LdapAuth API key. | Required secret |
| `c2id.logout.resourcePath` | Logout-session API path. | `/logout-sessions/rest/v1` |
| `c2id.logout.accessToken` | Long-lived bearer token for the logout-session API. | Required secret |
| `c2id.logout.defaultPostLogoutRedirectUri` | Fallback redirect when the RP does not provide `post_logout_redirect_uri`. | Empty; no redirect |

### Spring Boot, JTE, and Actuator properties

| Property | Description | Default / profile value |
| --- | --- | --- |
| `spring.application.name` | Spring application name. | `c2idlogin` |
| `spring.profiles.active` | Active Spring profile. | Maven DEV profile: `dev`; Maven PROD profile: `prod` |
| `server.port` | HTTP port for the embedded server. | Spring Boot: `8080`; DEV: `8000`; PROD: `8080` |
| `server.servlet.context-path` | Servlet context path used for all application URLs. | Empty; DEV example: `/c2id-login` |
| `server.servlet.application-display-name` | Display name shown by the servlet container. | `Connect2id Login ${project.version}` |
| `server.compression.enabled` | Enables HTTP response compression. | `false`; PROD: `true` |
| `server.compression.mime-types` | MIME types compressed when compression is enabled. | Spring Boot default; PROD config lists HTML, XML, text, CSS, JavaScript, and JSON |
| `server.compression.min-response-size` | Minimum response size for compression. | Spring Boot default; PROD config: `1024` bytes |
| `server.error.path` | Error dispatch path. | `/error` |
| `server.servlet.session.cookie.http-only` | Prevents client-side scripts from reading the session cookie. | `true` |
| `gg.jte.templateLocation` | JTE template source or compiled-template location. | `src/main/jte` |
| `gg.jte.developmentMode` | Compiles source templates on demand. | `false`; DEV: `true`; PROD: `false` |
| `gg.jte.usePrecompiledTemplates` | Uses templates generated during the Maven build. | `false`; DEV: `false`; PROD: `true` |
| `spring.web.resources.cache.period` | Static-resource cache period. | Spring Boot default; DEV: `0s` |
| `spring.web.resources.cache.cachecontrol.no-cache` | Adds `no-cache` to static-resource responses. | `false`; DEV: `true` |
| `spring.web.resources.cache.cachecontrol.no-store` | Prevents browser storage of static resources. | `false`; DEV: `true` |
| `spring.web.resources.cache.cachecontrol.must-revalidate` | Requires cache validation before reuse. | `false`; DEV: `true` |
| `management.endpoints.web.exposure.include` | Actuator endpoints exposed over HTTP. | `health` |
| `management.endpoint.health.show-details` | Health detail visibility. | `never` |

### Servlet context path

The application supports deployment below a servlet context path:

```yaml
server:
  servlet:
    context-path: /c2id-login
```

Templates generate context-aware asset, form, and logout URLs. The Connect2id logout endpoint must
include the context path, for example:

```text
http://localhost:8000/c2id-login/logout
```

### RP-initiated logout

Configure the Connect2id server logout settings:

- `op.logout.endpoint` — the URL of this application's `/logout` page.
- `op.logout.apiAccessToken` — the long-lived token used by the logout session API.
- `op.logout.sessionLifetime` — the lifetime of a logout session.

The application supports these routes:

- `GET /logout` and `POST /logout` — receive an RP-initiated logout request.
- `POST /logout/confirm` — submit `op_logout=true` or `op_logout=false`.
- `POST /logout/local` — start an OpenID provider-initiated logout from the local UI.
- `POST /consent/logout` — delete the subject session and start a new login flow while switching accounts.

The application uses a client-provided `post_logout_redirect_uri` when available. Otherwise it uses
`c2id.logout.defaultPostLogoutRedirectUri`, if configured.

### Health endpoint

Spring Boot Actuator exposes only the health endpoint. With the development context path, use:

```text
http://localhost:8000/c2id-login/actuator/health
```

The endpoint returns a minimal response such as:

```json
{"status":"UP"}
```

Health details are not exposed over HTTP.

## Docker

Build the application and image using the Dockerfile:

```bash
./mvnw -Pprod clean package
docker build -f Dockerfile -t c2id-login .
docker run --rm -p 8080:8080 c2id-login
```

The image uses the Eclipse Temurin 21 JRE.

## Implementing `UserAuthenticator`

`UserAuthenticator` has one method, `authenticate(User user)`. The included
`LdapUserAuthenticator` is an example implementation. A custom implementation can be loaded with
the Java `ServiceLoader` by setting `c2id.authenticatorClass` to its simple class name.

## OpenID Connect endpoints

The application root (`/`) is the authorization endpoint. Configure it as the Connect2id
authorization endpoint. `/logout` handles RP-initiated end-session requests.

Copyright (c) Connect2id Ltd., 2026
