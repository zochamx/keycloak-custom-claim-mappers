# 🧩 User Attribute Claim Mapper

This module adds custom claims to issued tokens when a user's attribute meets configurable criteria. The source attribute, the matching value (exact match or regular expression) are configurable. Claims can be emitted conditionally and included in the Access Token, ID Token, and/or UserInfo response.

This mapper is part of the [Keycloak Custom Claim Mappers](../..) project.


## Why

By default, Keycloak supports mapping user attributes to token claims, but its built-in mapper may not always allow:
- Using custom claims to present user attributes.
- Customizing inclusion based on complex matching.
- Customizing inclusion based on multiple values i.e. issue a claim if user is part of two groups.

This module provides a dedicated and reusable implementation for exposing **user attributes as token claims** with improved configurability.


## Configuration

Once the module is deployed to your Keycloak instance, you can add the mapper via the Admin Console:

1. Go to **Clients → (your client) → Client Scopes → Add Mapper by Configuration**.
2. Select **User Attribute Claim Mapper**.
3. Configure the following fields:


| Field | Description |
|--------|-------------|
| **Name** | A descriptive name for this mapper. |
| **User Attribute** | Name of the user attribute to inspect (e.g. `department`, `memberOf`). |
| **Match Mode** |How to match the users' attribute values against the match list. (e.g. `EXACT`, `CONTAINS`, `REGEX`). |
| **Match Operator** |ANY = at least one value must match; ALL = all values must match. |
| **Match Values** |One or more values to match against the attribute values. Each value on its own line. |
| **Claim Value** |Value of the claim to insert if a match occurs. |
| **Token Claim Name** | Name of the claim to insert into the token. |
| **Negate Logic** | If true, claim is inserted when no match is found. |
| **Add to ID token** | Whether to include this claim in the ID Token. |
| **Add to access token** | Whether to include this claim in the Access Token. |
| **Add to lightweight access token** | Whether to include this claim in the lightweight Access Token. |
| **Add to userinfo** | Whether to include this claim in the UserInfo response. |



## Example

**User attributes:**
- memberOf: cn=Developers;ou=IT;o=myOrg,cn=Reviewers;ou=IT;o=myOrg 

**Mapper configuration:**
| Setting | Value |
|----------|-------|
| Name | memberOf-to-codereview-mapper |
| User Attribute | memberOf |
| Match Mode | REGEX |
| Match Operator | ALL |
| Match Values | [Developers, Reviewers] |
| Token Claim Name | codereview |
| Claim Value | true |
| Negate Logic | false |
| Add to ID Token | ✅ |
| Add to Access Token | ✅ |
| Add to UserInfo | ✅ |

**Resulting token claim:**
```json
{
...
  "codereview": "true"
...
}

