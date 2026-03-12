package com.dochiri.nplusguard.core.query;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DefaultQueryFingerprintStrategy {

    private static final Pattern FROM_ALIAS =
            Pattern.compile("\\bfrom\\s+([\\w.\"`]+)\\s+(?:as\\s+)?([\\w$]+)\\b");
    private static final Pattern JOIN_ALIAS =
            Pattern.compile("\\bjoin\\s+(?:fetch\\s+)?([\\w.\"`]+)\\s+(?:as\\s+)?([\\w$]+)\\b");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private DefaultQueryFingerprintStrategy() {
    }

    public static String fingerprint(String normalizedSql) {
        if (normalizedSql == null || normalizedSql.isBlank()) {
            return "";
        }

        String fingerprint = normalizedSql.strip();
        fingerprint = stripAliases(fingerprint);
        fingerprint = WHITESPACE.matcher(fingerprint).replaceAll(" ").trim();
        return fingerprint;
    }

    private static String stripAliases(String sql) {
        Set<String> aliases = new LinkedHashSet<>();
        String withoutFromAliases = stripAliasDeclarations(sql, FROM_ALIAS, "from", aliases);
        String withoutJoinAliases = stripAliasDeclarations(withoutFromAliases, JOIN_ALIAS, "join", aliases);

        String fingerprint = withoutJoinAliases;
        for (String alias : aliases) {
            fingerprint = fingerprint.replaceAll("\\b" + Pattern.quote(alias) + "\\.", "");
        }
        return fingerprint;
    }

    private static String stripAliasDeclarations(String sql, Pattern pattern, String keyword, Set<String> aliases) {
        Matcher matcher = pattern.matcher(sql);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            aliases.add(matcher.group(2));
            matcher.appendReplacement(buffer, keyword + " " + matcher.group(1));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
