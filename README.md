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
