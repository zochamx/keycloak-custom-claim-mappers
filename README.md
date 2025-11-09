
# 🔐 Keycloak Custom Claim Mappers

**Keycloak Custom Claim Mappers** is an open-source collection of protocol mappers that extend [Keycloak](https://www.keycloak.org)’s token customization capabilities.  
Each mapper provides logic (mostly conditional) for transforming or enriching user, group, or custom data into OIDC tokens, beyond what’s available in the standard Keycloak distribution.

The project is designed to be modular, each mapper lives in its own Maven module and can be built and deployed independently or as part of the full suite.


## Features


| Module                          | Description                                             |
|---------------------------------|---------------------------------------------------------|
| **user-attribute-claim-mapper** | Issues custom claims based on filtered user attributes. |
| *(more modules coming soon)*    |                                                         |

Each module has its own `README.md` with setup details and configuration examples.



## Deployment


You can install any of the modules by:

- Go to the [Releases](https://github.com/zochamx/keycloak-custom-claim-mappers/releases) page and download the latest `<module>-<version>.jar`.

- Place the JAR file into your Keycloak providers directory.

- Rebuild and restart Keycloak.

After restarting, the mapper will be available to included in any `Client` or `Client Scope` configuration.



## Contributing

Contributions are always welcome!

See `contributing.md` for ways to get started.

Please adhere to this project's `code of conduct`.


## License

Licensed under the Apache License 2.0