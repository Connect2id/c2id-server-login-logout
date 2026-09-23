# Connect2id Server Login / Logout UI

A server-side rendered login and consent page for a [Connect2id server](https://connect2id.com/products/server).
The application uses Spring Boot 4+, Java 21+, JTE templates, Bootstrap 5, and minimal browser JavaScript.

It supports:

- OpenID Connect authentication and consent flows.
- LDAP authentication through the Connect2id LdapAuth JSON-RPC 2.0 service.
- Custom `UserAuthenticator` implementations through the existing SPI.
- Subject session verification and logout through the Connect2id APIs.
- Query, fragment, and `form_post` OpenID Connect response modes.

The project has no client-side build step and does not use NodeJS tooling.

## Quick start

1. Configure the Connect2id and LdapAuth endpoints and credentials in
   `src/main/resources/config/application.yml` or an active profile.
2. Run the development profile:

   ```bash
   ./mvnw spring-boot:run
   ```

3. Open `http://localhost:8000` or configure that URL as the Connect2id authorization endpoint.

The development profile watches `src/main/jte` and reloads changed templates.

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
```

Do not commit production tokens or API keys. Prefer environment variables or an external configuration source.

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
authorization endpoint. `/logout` handles an optional end-session request.

Copyright (c) Connect2id Ltd., 2026
