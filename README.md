# 🔐 Keycloak Custom Claim Mappers

## Description

**Keycloak Custom Claim Mappers** is an open-source collection of protocol mappers that extend [Keycloak](https://www.keycloak.org)’s token customization capabilities.  
Each mapper provides logic (mostly conditional) for transforming or enriching user, group, or custom data into OIDC tokens, beyond what’s available in the standard Keycloak distribution.

The project is designed to be modular — each mapper lives in its own Maven module and can be built and deployed independently or as part of the full suite.

---

## Purpose / Motivation

Keycloak’s built-in mappers cover common use cases, especially if an environment is build from
scratch to integrate with Keycloak, but migrated environments often require more flexibility:
- Adding custom claims from non-standard user attributes for compatibility with legacy apps.
- Issuing claims based on conditional logic related with user attributes or group membership. 
- Enforcing advanced claim logic during token creation.

This project provides a structured and reusable approach to building, maintaining, and sharing such custom mappers, ensuring clean integration with any Keycloak deployment.

---

## Features / Modules

| Module | Description                                                               |
|--------|---------------------------------------------------------------------------|
| **user-attribute-claim-mapper** | Issues custom claims based on filtered user attributes. |
| *(more modules coming soon)* |                                                                           |

Each module has its own `README.md` with setup details and configuration examples.

---

## Build

Requirements:
- Java 17+
- Maven 3.8+

To build all modules:

```bash
mvn clean package
```

The compiled JARs will be located under each module’s `target` directory, for example: `user-attribute-claim-mapper/target/user-attribute-claim-mapper-1.0.0.jar`

---

## Deploy

1. Copy the desired mapper JAR(s) into your Keycloak installation:
```
/opt/keycloak/providers/
```

2. Rebuild and restart Keycloak: 
```bash
/opt/keycloak/bin/kc.sh build
/opt/keycloak/bin/kc.sh start
```


Once deployed, the mapper will appear under Client → Mappers → Create → By Configuration → Mapper Type in the Keycloak Admin Console.

## Compatibility

- **Keycloak version**: Tested with 26.x (should work with 22+)
- **Java version**: 17 or higher
- **Build system**: Maven (multi-module)

Each module specifies its tested Keycloak version in its own pom.xml.

## Contributing

**Contributions are welcome!** 
You can propose new mappers, submit bugs or bug fixes, or improve documentation.

1. Fork the repository.

2. Make your changes

3. Open a pull request describing what did you change and why.

## License

Licensed under the Apache License 2.0