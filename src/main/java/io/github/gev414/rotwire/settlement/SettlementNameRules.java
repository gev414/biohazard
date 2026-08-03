package io.github.gev414.rotwire.settlement;

import java.util.Optional;

/**
 * Keeps settlement names compact and safe to persist and send over the wire.
 */
public final class SettlementNameRules {

    public static final int MAX_LENGTH = 40;

    public static Optional<String> normalize(String proposedName) {
        if (proposedName == null) {
            return Optional.empty();
        }

        StringBuilder normalized = new StringBuilder();
        boolean previousWhitespace = true;
        for (int offset = 0; offset < proposedName.length(); ) {
            int codePoint = proposedName.codePointAt(offset);
            offset += Character.charCount(codePoint);

            if (Character.isWhitespace(codePoint)) {
                if (!previousWhitespace && normalized.length() < MAX_LENGTH) {
                    normalized.append(' ');
                    previousWhitespace = true;
                }
                continue;
            }
            if (Character.isISOControl(codePoint)) {
                continue;
            }
            if (normalized.length() + Character.charCount(codePoint)
                    > MAX_LENGTH) {
                break;
            }
            normalized.appendCodePoint(codePoint);
            previousWhitespace = false;
        }

        String result = normalized.toString().trim();
        return result.isEmpty() ? Optional.empty() : Optional.of(result);
    }

    private SettlementNameRules() {
    }
}
