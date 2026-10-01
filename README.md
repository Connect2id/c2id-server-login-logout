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

## Customization

There is no client-side build step. You change the appearance and the wording by editing the source
files. The application must be rebuilt for each change in production. In the development profile a
page refresh is sufficient.

| Change | File to edit |
| --- | --- |
| Logo and favicon | `src/main/resources/static/img/connect2id-logo.svg`, `src/main/resources/static/favicon.ico` |
| Colours, fonts, and other styles | `src/main/resources/static/css/main.css` |
| Page titles and all page texts | `src/main/resources/i18n/messages.properties` and the locale files next to it |
| Application name in the header | `server.servlet.application-display-name` in `src/main/resources/config` |
| Page structure | `src/main/jte/*.jte` |
| Authentication method | See [Implementing `UserAuthenticator`](#implementing-userauthenticator) |

### Logo and favicon

The header always requests this file:

```jte
<img src="${imagesPath}/connect2id-logo.svg" width="158" height="19" alt="...">
```

Two ways exist to use a different logo.

- Keep the file name. Overwrite `src/main/resources/static/img/connect2id-logo.svg` with your own SVG
  file. You must also change the size, because the width and the height are in `layout.jte` and the
  width is set in `.auth-brand img` in `main.css`.
- Use a new file. Add the file, for example `my-company-logo.svg`, to `src/main/resources/static/img`
  and change the `src` value in `layout.jte`.

Any file below `src/main/resources/static` is served from the servlet context root. The template uses
`${imagesPath}`, which is the context path plus `/img`, so the URL is correct for a WAR deployment
under any context path.

The browser icon is a different file. Overwrite `src/main/resources/static/favicon.ico`. The
alternative texts `global.connect2idLogo` and `global.connect2idHome` are in the message files. Change
them there if the logo is not a Connect2id logo.

### Styles

`layout.jte` loads two style sheets in this order.

```html
<link rel="stylesheet" href="${contextPath}/webjars/bootstrap/5.3.8/css/bootstrap.min.css">
<link rel="stylesheet" href="${stylesPath}/main.css">
```

Bootstrap 5 is loaded first. Because `main.css` is loaded second, its rules win when both style
sheets use the same selector. Use `!important` only if Bootstrap uses a more specific selector.

The theme is driven by CSS custom properties. `:root` holds the dark values and the media query
`@media (prefers-color-scheme: light)` holds the light values. The browser decides the theme. There
is no switch in the page.

| Variable | Use |
| --- | --- |
| `--c2id-background` | Page background and header background |
| `--c2id-surface` | Panel, list item, and avatar backgrounds |
| `--c2id-border` | Border lines |
| `--c2id-text` | Main text colour |
| `--c2id-muted` | Secondary text colour |
| `--c2id-link` | Links and the primary button |
| `--c2id-lime` | Accent colour |
| `--c2id-input` | Input field background |
| `--c2id-input-border` | Input field border |
| `--c2id-placeholder` | Placeholder and hint text |
| `--c2id-shadow` | Panel shadow |

Two ways exist to change the styles.

- Change the values in the `:root` block and in the light media query. This keeps both colour
  schemes.
- Overwrite `src/main/resources/static/css/main.css` with your own style sheet. Keep the custom
  properties and the font face rules if you want to keep both colour schemes and the current fonts.

The font is PolySans. The three font files are in `src/main/resources/static/fonts` and are declared
in three `@font-face` rules at the top of `main.css`. The `font-family` on `body` lists fallbacks.
Change the `font-family` value to use a system font only, and then delete the `@font-face` rules.

The main class names are listed below. They are the interface between `main.css` and the templates.

- Layout: `auth-header`, `auth-header-inner`, `auth-brand`, `auth-header-label`, `auth-main`.
- Login page: `login-page`, `login-intro`, `login-eyebrow`, `login-panel`, `login-form`, `form-field`,
  `login-hint`, `login-alert`, `login-actions`, `login-submit`, `login-cancel`, `login-subtitle`.
- Account picker: `account-list`, `account-option`, `account-session-option`, `account-select`.
- Consent page: `consent-items`, `consent-item`.
- Logged-in header: `user-card`, `user-avatar`, `user-details`, `user-name`, `user-email`,
  `user-logout`.
- Logout page: `logout-iframe`.

Bootstrap utility classes are also used. Do not delete the Bootstrap link tag unless you replace all
of those classes.

### Page titles and texts

All page texts are in the message files in `src/main/resources/i18n`. Spring Boot reads them with
`spring.messages.basename: i18n/messages`.

| File | Use |
| --- | --- |
| `messages.properties` | Default texts and English |
| `messages_en.properties` | English |
| `messages_de.properties` | German |
| `messages_fr.properties` | French |

The login page title is one key. `login.title` sets the browser tab title and the heading in the
page. For example:

```properties
login.title=Sign in to Acme
```

The `ui_locales` parameter of the OpenID Connect request selects the file. The application stores
the selected locale in a cookie named `locale`. Add a new file, for example
`messages_nl.properties`, to add a new language.

The other page titles are `global.title`, `error.title`, `logout.title`, and `consent.title`. Every
other text in the interface, such as `login.form.username` or `consent.claims`, is also a key in the
same files. Copy the default file before you change it, so that no key is missing in a new language.

The name in the header on the right is not a message. It comes from
`server.servlet.application-display-name`, which is set in `src/main/resources/config`. For example:

```yaml
server:
    servlet:
        application-display-name: Acme Identity
```

### Templates

The JTE templates are in `src/main/jte`. `layout.jte` is the common page layout and holds the header,
the logo, and the style sheet links. The other files render one page each: `login.jte`,
`consent.jte`, `logout.jte`, `user.jte`, `error.jte`, `home.jte`, and `formPost.jte`.

The development profile compiles the templates when a page is requested. The production profile uses
the templates that Maven compiled during the build. You must run `./mvnw -Pprod clean package` after
each template change.

## Docker

Build the application and image using the Dockerfile:

```bash
./mvnw -Pprod clean package
docker build -f Dockerfile -t c2id-login .
docker run --rm -p 8080:8080 c2id-login
```

The image uses the Eclipse Temurin 21 JRE.

## Implementing `UserAuthenticator`

`UserAuthenticator` has one method.

```java
AuthenticationResult authenticate(User user) throws AuthenticationException;
```

The interface is in `src/main/java/com/nimbusds/login/spi/UserAuthenticator.java`. Implement it if
the subject must be authenticated by another service, for example a local database, a SAML identity
provider, or a remote API.

### The method contract

The `User` object holds the data that the user typed in the login form. The fields are set before
the method is called.

| Field | Requirement | Meaning |
| --- | --- | --- |
| `username` | Required | The subject name. The setter converts the value to lower case. |
| `password` | Required | The plain password. It is never stored. |
| `id` | Optional | A stable internal identifier of the subject. |
| `name` | Optional | The display name shown in the pages. |
| `email` | Optional | The e-mail address shown in the pages. |

The method must return an `AuthenticationResult`. Two fields matter.

- `subject`: the same `User` object, with `id`, `name`, and `email` now set.
- `details`: the raw response of the authentication service, as a `Map`. The application reads
  `details.result.attributes`. Every entry in that map, except `userID`, is sent to the Connect2id
  server as a subject-session claim. A `jpegPhoto` or `picture` entry becomes the OIDC `picture`
  claim and is shown as the user avatar. `details` is optional; return an empty map if the service
  returns nothing else.

Use the two exceptions to report failures. `InvalidCredentialsException` is a runtime exception and
means that the password is wrong or the subject is unknown. The application shows the login page
again with an error message. `AuthenticationException` is a checked exception and means that the
authentication service failed or returned an invalid answer. The application shows the same login
error. Do not throw either exception for a valid user.

Never log a password or a claim value. Log the subject name and the failure reason only.

`LdapUserAuthenticator` in `src/main/java/com/nimbusds/login/spi/impl` is the reference
implementation. It calls the `user.authGet` method of the Connect2id LdapAuth JSON-RPC 2.0 service,
fills the `User` from `result.attributes`, and returns the parsed JSON-RPC response in `details`.

### Loading a custom implementation

`WebConfigurator` selects the implementation at startup.

```java
@Bean
public UserAuthenticator userAuthenticator(ApplicationProperties config) {
    String authenticatorClass = config.getAuthenticatorClass();
    if (!StringUtils.isBlank(authenticatorClass)) {
        for (UserAuthenticator authImpl : ServiceLoader.load(UserAuthenticator.class)) {
            if (authImpl != null && authenticatorClass.equalsIgnoreCase(authImpl.getClass().getSimpleName())) {
                logger.info("Using UserAuthenticator implementation: {}", authImpl.getClass().getName());
                return authImpl;
            }
        }
        logger.warn("No UserAuthenticator implementation found for '{}', using {}", authenticatorClass,
                LdapUserAuthenticator.class.getName());
    }
    logger.info("Using UserAuthenticator implementation: {}", LdapUserAuthenticator.class.getName());
    return new LdapUserAuthenticator(config);
}
```

Four rules follow from this code.

1. Put the new class in `src/main/java`, for example in the package `com.nimbusds.login.spi.impl`, and
   implement `UserAuthenticator`.
2. The class must have a public constructor with no arguments. `ServiceLoader` creates the object,
   so the class cannot need constructor parameters. Read the configuration with
   `@Value` or with the Spring `Environment` instead of with a constructor argument.
3. Register the class in `src/main/resources/META-INF/services/com.nimbusds.login.spi.UserAuthenticator`.
   Put the full class name on one line.

   ```text
   com.nimbusds.login.spi.impl.MyUserAuthenticator
   ```

4. Set the simple class name of the implementation. The comparison ignores case. A missing property,
   an empty value, or a name that no registered class matches means that the application uses
   `LdapUserAuthenticator`.

```yaml
c2id:
    authenticatorClass: MyUserAuthenticator
```

The application logs the loaded implementation class name at startup. Check the log if you are not
sure which implementation is active. The application logs a warning if the value of
`c2id.authenticatorClass` matches no registered class.

## OpenID Connect endpoints

The application root (`/`) is the authorization endpoint. Configure it as the Connect2id
authorization endpoint. `/logout` handles RP-initiated end-session requests.

Copyright (c) Connect2id Ltd., 2026
