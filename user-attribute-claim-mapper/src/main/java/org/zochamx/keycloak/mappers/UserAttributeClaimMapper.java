package org.zochamx.keycloak.mappers;

import org.jboss.logging.Logger;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.mappers.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.*;
import java.util.regex.Pattern;

public class UserAttributeClaimMapper extends AbstractOIDCProtocolMapper
        implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    private static final Logger LOG = Logger.getLogger(UserAttributeClaimMapper.class);

    public static final String PROVIDER_ID = "user-attribute-claim-mapper";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String CFG_ATTR_NAME = "attribute_name";
    private static final String CFG_MATCH_MODE = "match_mode";
    private static final String CFG_MATCH_OPERATOR = "match_operator";
    private static final String CFG_MATCH_VALUES = "match_values";
    private static final String CFG_CLAIM_VALUE = "claim_value";
    private static final String CFG_NEGATE = "negate";

    private enum MatchMode {
        EXACT,
        CONTAINS,
        REGEX
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty attrName = new ProviderConfigProperty();
        attrName.setName(CFG_ATTR_NAME);
        attrName.setLabel("User Attribute");
        attrName.setType(ProviderConfigProperty.STRING_TYPE);
        attrName.setHelpText("Name of the user attribute to inspect (e.g., 'ldap.groupMembership').");
        configProperties.add(attrName);

        ProviderConfigProperty matchMode = new ProviderConfigProperty();
        matchMode.setName(CFG_MATCH_MODE);
        matchMode.setLabel("Match Mode");
        matchMode.setType(ProviderConfigProperty.LIST_TYPE);
        matchMode.setOptions(List.of("EXACT", "CONTAINS", "REGEX"));
        matchMode.setDefaultValue("CONTAINS");
        matchMode.setHelpText("How to match the users' attribute values against the match list.");
        configProperties.add(matchMode);

        ProviderConfigProperty matchOperator = new ProviderConfigProperty();
        matchOperator.setName(CFG_MATCH_OPERATOR);
        matchOperator.setLabel("Match Operator");
        matchOperator.setType(ProviderConfigProperty.LIST_TYPE);
        matchOperator.setOptions(List.of("ANY", "ALL"));
        matchOperator.setDefaultValue("ANY");
        matchOperator.setHelpText("ANY = at least one value must match; ALL = all values must match.");
        configProperties.add(matchOperator);


        ProviderConfigProperty matchValues = new ProviderConfigProperty();
        matchValues.setName(CFG_MATCH_VALUES);
        matchValues.setLabel("Match Values");
        matchValues.setType(ProviderConfigProperty.MULTIVALUED_STRING_TYPE);
        matchValues.setHelpText("One or more values to match against the attribute values. Each value on its own line.");
        configProperties.add(matchValues);

        ProviderConfigProperty claimValue = new ProviderConfigProperty();
        claimValue.setName(CFG_CLAIM_VALUE);
        claimValue.setLabel("Claim Value");
        claimValue.setType(ProviderConfigProperty.STRING_TYPE);
        claimValue.setDefaultValue("true");
        claimValue.setHelpText("Value of the claim to insert if a match occurs.");
        configProperties.add(claimValue);

        ProviderConfigProperty negate = new ProviderConfigProperty();
        negate.setName(CFG_NEGATE);
        negate.setLabel("Negate Logic");
        negate.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        negate.setDefaultValue("false");
        negate.setHelpText("If true, claim is inserted when no match is found.");
        configProperties.add(negate);

        // The builtin protocol mapper let the user define under which claim name (key)
        // the protocol mapper writes its value.
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);
        // The builtin protocol mapper let the user define for which tokens the protocol mapper
        // is executed (access token, id token, user info).
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, UserAttributeClaimMapper.class);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getDisplayCategory() {
        return "Token Mapper";
    }

    @Override
    public String getDisplayType() {
        return "User Attribute Claim Mapper";
    }

    @Override
    public String getHelpText() {
        return "Adds a custom claim to the token based on user attribute values (e.g., ldap.groupMembership).";
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel,
                            UserSessionModel userSession, KeycloakSession keycloakSession,
                            ClientSessionContext clientSessionCtx) {

        UserModel user = userSession.getUser();

        String attributeName = mappingModel.getConfig().get(CFG_ATTR_NAME);
        String claimName = mappingModel.getConfig().get(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME);
        String claimValueStr = mappingModel.getConfig().getOrDefault(CFG_CLAIM_VALUE, "true");
        boolean negate = Boolean.parseBoolean(mappingModel.getConfig().getOrDefault(CFG_NEGATE, "false"));
        String modeStr = mappingModel.getConfig().getOrDefault(CFG_MATCH_MODE, "EXACT");

        if (attributeName == null || claimName == null) {
            LOG.warnf("Mapper [%s] misconfigured: missing attribute_name or claim_name.", mappingModel.getName());
            return;
        }

        List<String> attrValues = user.getAttributes().get(attributeName);
        if (attrValues == null || attrValues.isEmpty()) {
            LOG.debugf("User [%s] has no attribute [%s]; skipping mapper [%s].",
                    user.getUsername(), attributeName, mappingModel.getName());
            return;
        }

        List<String> targets = getListConfigProperty(mappingModel, CFG_MATCH_VALUES);
        if (targets == null || targets.isEmpty()) {
            LOG.warnf("Mapper [%s] has no match_values configured.", mappingModel.getName());
            return;
        }

        MatchMode mode = MatchMode.valueOf(modeStr);
        boolean requireAll = "ALL".equalsIgnoreCase(mappingModel.getConfig()
                .getOrDefault(CFG_MATCH_OPERATOR, "ANY"));
        boolean matched = requireAll
                ? matchesAllTargets(attrValues, targets, mode)
                : matchesAnyTarget(attrValues, targets, mode);

        if (negate) {
            matched = !matched;
        }

        if (!matched) {
            LOG.debugf("User [%s] did not match mapper [%s] criteria.", user.getUsername(), mappingModel.getName());
            return;
        }

        Object claimValue = parseValue(claimValueStr);
        OIDCAttributeMapperHelper.mapClaim(token, mappingModel, claimValue);

        LOG.debugf("Mapper [%s] added claim [%s=%s] for user [%s].",
                mappingModel.getName(), claimName, claimValueStr, user.getUsername());
    }

    /**
     * Returns true if EVERY target matches at least one of the provided values.
     */
    private boolean matchesAllTargets(List<String> values, List<String> targets, MatchMode mode) {
        if (values == null || values.isEmpty() || targets == null || targets.isEmpty()) return false;
        return targets.stream().allMatch(target ->
                values.stream().anyMatch(value -> matches(value, target, mode))
        );
    }

    /**
     * Returns true if ANY target matches at least one of the provided values.
     */
    private boolean matchesAnyTarget(List<String> values, List<String> targets, MatchMode mode) {
        if (values == null || values.isEmpty() || targets == null || targets.isEmpty()) return false;
        return targets.stream().anyMatch(target ->
                values.stream().anyMatch(value -> matches(value, target, mode))
        );
    }

    private boolean matches(String value, String target, MatchMode mode) {
        if (value == null || target == null) return false;

        value = value.trim();
        target = target.trim();

        return switch (mode) {
            case EXACT -> value.equalsIgnoreCase(target);
            case CONTAINS -> value.toLowerCase().contains(target.toLowerCase());
            case REGEX -> Pattern.compile(target).matcher(value).find();
        };
    }

    private Object parseValue(String value) {
        if ("true".equalsIgnoreCase(value)) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(value)) return Boolean.FALSE;
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
        }
        return value;
    }

    private List<String> getListConfigProperty(ProtocolMapperModel model, String key) {
        String raw = model.getConfig().get(key);
        if (raw == null || raw.isBlank()) return List.of();

        try {
            if (raw.contains("##")) {
                // Keycloak export or legacy delimiter form
                return Arrays.stream(raw.split("##"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
            }
            if (raw.startsWith("[") && raw.endsWith("]")) {
                // JSON array form
                return MAPPER.readValue(raw, new TypeReference<List<String>>() {});
            }
            // Single value
            return List.of(raw);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to parse as an array for config key [%s]: %s", key, raw);
            return List.of();
        }
    }
}

