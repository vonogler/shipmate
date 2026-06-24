package com.shipmate.api.emailorder;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EmailMimeDecoder {
    private static final Pattern ENCODED_WORD_PATTERN = Pattern.compile("=\\?([^?]+)\\?([bBqQ])\\?([^?]+)\\?=");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    private EmailMimeDecoder() {
    }

    public static String decodeHeader(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        Matcher matcher = ENCODED_WORD_PATTERN.matcher(value);
        StringBuilder decoded = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(decoded, Matcher.quoteReplacement(decodeEncodedWord(matcher)));
        }
        matcher.appendTail(decoded);
        return decoded.toString().replaceAll("\\s+", " ").strip();
    }

    public static String decodeParameterValue(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = trimQuotes(value.strip());
        int charsetEnd = normalized.indexOf("''");
        if (charsetEnd > 0) {
            String charsetName = normalized.substring(0, charsetEnd);
            String encoded = normalized.substring(charsetEnd + 2);
            try {
                return URLDecoder.decode(encoded, charset(charsetName)).strip();
            } catch (IllegalArgumentException ignored) {
                return URLDecoder.decode(encoded, StandardCharsets.UTF_8).strip();
            }
        }
        return decodeHeader(normalized);
    }

    public static String normalizeBody(String body, String contentType) {
        if (body == null || body.isBlank()) {
            return "";
        }
        String text = body;
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("text/html")) {
            text = text.replaceAll("(?is)<br\\s*/?>", "\n");
            text = text.replaceAll("(?is)</p>", "\n");
            text = HTML_TAG_PATTERN.matcher(text).replaceAll(" ");
            text = text.replace("&nbsp;", " ")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&amp;", "&")
                    .replace("&quot;", "\"");
        }
        return text.lines()
                .map(String::strip)
                .filter(line -> !line.isBlank())
                .reduce("", (left, right) -> left + (left.isBlank() ? "" : "\n") + right)
                .strip();
    }

    public static String decodeTextPart(String value, String transferEncoding, Charset fallbackCharset) {
        byte[] bytes = decodeTransfer(value, transferEncoding);
        return new String(bytes, fallbackCharset == null ? StandardCharsets.UTF_8 : fallbackCharset);
    }

    public static byte[] decodeTransfer(String value, String transferEncoding) {
        if (value == null || value.isBlank()) {
            return new byte[0];
        }
        String normalizedEncoding = transferEncoding == null ? "" : transferEncoding.toLowerCase(Locale.ROOT).strip();
        String compact = value.replaceAll("\\s+", "");
        try {
            if ("base64".equals(normalizedEncoding)) {
                return Base64.getMimeDecoder().decode(compact);
            }
        } catch (IllegalArgumentException ignored) {
            return new byte[0];
        }
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static String decodeEncodedWord(Matcher matcher) {
        Charset charset = charset(matcher.group(1));
        String encoding = matcher.group(2);
        String payload = matcher.group(3);
        try {
            byte[] bytes = "B".equalsIgnoreCase(encoding)
                    ? Base64.getDecoder().decode(payload)
                    : decodeQuotedPrintableWord(payload);
            return new String(bytes, charset);
        } catch (IllegalArgumentException exception) {
            return matcher.group(0);
        }
    }

    private static byte[] decodeQuotedPrintableWord(String value) {
        StringBuilder decoded = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '_') {
                decoded.append(' ');
            } else if (current == '=' && index + 2 < value.length()) {
                String hex = value.substring(index + 1, index + 3);
                try {
                    decoded.append((char) Integer.parseInt(hex, 16));
                    index += 2;
                } catch (NumberFormatException exception) {
                    decoded.append(current);
                }
            } else {
                decoded.append(current);
            }
        }
        return decoded.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private static Charset charset(String value) {
        try {
            return Charset.forName(value);
        } catch (Exception exception) {
            return StandardCharsets.UTF_8;
        }
    }

    private static String trimQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
