
etting Started

### Quick Start: Profiles and Secrets

- Production: set environment variable `JWT_SECRET` to a strong value (>= 32 characters, 256-bit). The app will fail fast if missing/weak.
- Local Development: run with Spring profile `dev` to use safe local fallbacks from `application-dev.properties`.
- Tests: tests activate the `test` profile and use an in-memory H2 DB and a test JWT secret.

### IntelliJ Run/Debug Configuration

1. Open Run/Debug Configurations > Spring Boot > ManagioApplication (or create one).
2. Set Active profiles:
   - Local dev: `dev`
   - Production-like: `prod` (requires `JWT_SECRET` set as env var)
3. Environment variables (for production-like runs):
   - `JWT_SECRET=your-very-long-256-bit-secret-here`
   - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` as needed
4. Alternative: Create a `.env` file (not committed) based on `.env.example` for Docker/compose or local shells, then run via terminal that loads it.

Note: Never commit real secrets. `.env` is ignored by Git.

### Reference Documentation

For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/4.0.1/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/4.0.1/maven-plugin/build-image.html)
* [Spring Data JPA](https://docs.spring.io/spring-boot/4.0.1/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [Spring Security](https://docs.spring.io/spring-boot/4.0.1/reference/web/spring-security.html)
* [Validation](https://docs.spring.io/spring-boot/4.0.1/reference/io/validation.html)
* [Spring Web](https://docs.spring.io/spring-boot/4.0.1/reference/web/servlet.html)

### Guides

The following guides illustrate how to use some features concretely:

* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
* [Accessing data with MySQL](https://spring.io/guides/gs/accessing-data-mysql/)
* [Securing a Web Application](https://spring.io/guides/gs/securing-web/)
* [Spring Boot and OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)
* [Authenticating a User with LDAP](https://spring.io/guides/gs/authenticating-ldap/)
* [Validation](https://spring.io/guides/gs/validating-form-input/)
* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the
parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.

