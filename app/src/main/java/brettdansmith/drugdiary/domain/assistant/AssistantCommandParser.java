package brettdansmith.drugdiary.domain.assistant;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Single source of truth for local slash-command recognition and highlighting.
 */
public final class AssistantCommandParser {
    private AssistantCommandParser() {
    }

    public static boolean isKnownCommand(String command) {
        String normalized = normalize(command);
        if (normalized.isEmpty()) return false;

        String lower = normalized.toLowerCase(Locale.US);
        String body = lower.substring(1);
        String name = body.split("\\s+", 2)[0];
        String payload = body.length() > name.length() ? body.substring(name.length()).trim() : "";

        AssistantCommandRegistry.CommandSpec spec = AssistantCommandRegistry.specFor(name);
        if (spec != null) {
            if (spec.payloadMode == AssistantCommandRegistry.PayloadMode.NONE) return payload.isEmpty();
            if (spec.payloadMode == AssistantCommandRegistry.PayloadMode.OPTIONAL) return true;
            return !payload.isEmpty();
        }
        if ("drugcache".equals(name)) return payload.isEmpty()
                || "status".equals(payload)
                || "clear".equals(payload)
                || payload.startsWith("warm ");
        return false;
    }

    public static String commandName(String command) {
        String normalized = normalize(command);
        if (normalized.isEmpty()) return "";
        String body = normalized.substring(1);
        return body.split("\\s+", 2)[0].toLowerCase(Locale.US);
    }

    public static String normalize(String command) {
        if (command == null) return "";
        String trimmed = command.trim();
        if (!trimmed.startsWith("/")) return "";
        if (trimmed.startsWith("//")) return "";
        return stripTrailingPunctuation(trimmed);
    }

    public static List<Span> findCommands(String text) {
        List<Span> spans = new ArrayList<>();
        if (text == null || text.isEmpty()) return spans;
        
        collectExplicitCommands(text, spans);
        collectSuggestions(text, spans);
        collectQuoted(text, spans);
        collectAnywhere(text, spans);
        return spans;
    }

    private static void collectExplicitCommands(String text, List<Span> spans) {
        int start = text.indexOf("[[command:");
        while (start >= 0) {
            int end = text.indexOf("]]", start);
            if (end < 0) break;
            String cmd = text.substring(start + 10, end);
            spans.add(new Span(start, end + 2, cmd, false));
            start = text.indexOf("[[command:", end);
        }
    }

    private static void collectSuggestions(String text, List<Span> spans) {
        // AI can suggest prompts using [[suggest:Ask about aspirin]]
        int start = text.indexOf("[[suggest:");
        while (start >= 0) {
            int end = text.indexOf("]]", start);
            if (end < 0) break;
            String prompt = text.substring(start + 10, end);
            spans.add(new Span(start, end + 2, prompt, true));
            start = text.indexOf("[[suggest:", end);
        }
    }

    private static void collectQuoted(String text, List<Span> spans) {
        int startQuote = text.indexOf('"');
        while (startQuote >= 0) {
            int endQuote = text.indexOf('"', startQuote + 1);
            if (endQuote < 0) break;
            String inner = text.substring(startQuote + 1, endQuote);
            int slash = inner.indexOf('/');
            if (slash >= 0) {
                String candidate = inner.substring(slash);
                String normalized = normalize(candidate);
                if (isHighlightable(normalized)) {
                    int start = startQuote + 1 + slash;
                    int end = startQuote + 1 + slash + normalized.length();
                    if (!overlaps(spans, start, end)) {
                        spans.add(new Span(start, end, normalized));
                    }
                }
            }
            startQuote = text.indexOf('"', endQuote + 1);
        }
    }

    private static void collectAnywhere(String text, List<Span> spans) {
        int i = 0;
        while (i < text.length()) {
            if (overlaps(spans, i, i + 1)) {
                i++;
                continue;
            }
            if (text.charAt(i) == '/') {
                boolean isDouble = i + 1 < text.length() && text.charAt(i + 1) == '/';
                boolean isBoundary = i == 0 || isCommandStartBoundary(text.charAt(i - 1));
                if (isBoundary) {
                    int nameEnd = i + (isDouble ? 2 : 1);
                    while (nameEnd < text.length() && Character.isLetterOrDigit(text.charAt(nameEnd))) {
                        nameEnd++;
                    }
                    int nameStart = i + (isDouble ? 2 : 1);
                    if (nameEnd == nameStart && nameEnd < text.length() && text.charAt(nameEnd) == '?') {
                        nameEnd++;
                    }

                    if (nameEnd > nameStart) {
                        int segmentEnd = text.length();
                        for (int p = nameEnd; p < text.length(); p++) {
                            char c = text.charAt(p);
                            if (c == '\n' || c == '\r' || c == '"' || c == '\'' || c == '<' || c == '>' || c == '[' || c == ']') {
                                segmentEnd = p;
                                break;
                            }
                        }
                        String rawSegment = text.substring(i, segmentEnd);
                        String matched = bestKnownCommand(rawSegment);
                        if (!matched.isEmpty() && isHighlightable(matched)) {
                            int actualEnd = i + matched.length();
                            if (!overlaps(spans, i, actualEnd)) {
                                spans.add(new Span(i, actualEnd, matched));
                                i = actualEnd - 1;
                            }
                        }
                    }
                }
            }
            i++;
        }
    }

    private static String bestKnownCommand(String rawSegment) {
        String candidate = normalize(rawSegment);
        if (candidate.isEmpty()) return "";
        String name = commandName(candidate);
        if (name.isEmpty()) return "";
        String prefix = "/";

        AssistantCommandRegistry.CommandSpec spec = AssistantCommandRegistry.specFor(name);

        if (spec != null && spec.payloadMode == AssistantCommandRegistry.PayloadMode.OPTIONAL && !"drugcache".equals(name)) {
            if ("help".equals(name)) {
                String payload = payload(candidate, name);
                String token = firstToken(payload);
                if (!token.isEmpty() && !isSentenceWord(token)) {
                    String maybeHelpTopic = prefix + name + " " + token;
                    if (isKnownCommand(maybeHelpTopic)) return maybeHelpTopic;
                }
            }
            return prefix + name;
        }

        if (spec != null && spec.payloadMode == AssistantCommandRegistry.PayloadMode.REQUIRED) {
            if (candidate.contains("|")) {
                return isKnownCommand(candidate) ? candidate : prefix + name;
            }
            String payload = payload(candidate, name);
            if (payload.isEmpty()) return "";
            String[] tokens = payload.split("\\s+");
            StringBuilder trimmed = new StringBuilder(prefix).append(name);
            int kept = 0;
            for (String token : tokens) {
                if (token == null || token.trim().isEmpty()) continue;
                if (kept > 0 && isSentenceWord(token)) break;
                if (token.startsWith("/")) break;
                trimmed.append(" ").append(token);
                kept++;
            }
            String narrowed = normalize(trimmed.toString());
            if (isKnownCommand(narrowed)) return narrowed;
        }

        if (isKnownCommand(candidate)) return candidate;

        int split;
        while ((split = candidate.lastIndexOf(' ')) > 0) {
            candidate = candidate.substring(0, split).trim();
            if (isKnownCommand(candidate)) return candidate;
        }
        String nameOnly = "/" + commandName(rawSegment);
        return isKnownCommand(nameOnly) ? nameOnly : "";
    }

    private static boolean isCommandStartBoundary(char previous) {
        return Character.isWhitespace(previous)
                || previous == '('
                || previous == '['
                || previous == '{'
                || previous == '<'
                || previous == '"'
                || previous == '\''
                || previous == ','
                || previous == ';'
                || previous == ':';
    }

    private static String payload(String normalizedCommand, String name) {
        String body = normalizedCommand.substring(1);
        return body.length() > name.length() ? body.substring(name.length()).trim() : "";
    }

    private static String firstToken(String payload) {
        if (payload == null || payload.trim().isEmpty()) return "";
        String[] parts = payload.trim().split("\\s+", 2);
        return parts.length == 0 ? "" : parts[0].trim();
    }

    private static boolean isSentenceWord(String token) {
        String clean = token == null ? "" : token.trim().toLowerCase(Locale.US);
        return clean.equals("to")
                || clean.equals("for")
                || clean.equals("because")
                || clean.equals("then")
                || clean.equals("please")
                || clean.equals("after")
                || clean.equals("before")
                || clean.equals("while")
                || clean.equals("when")
                || clean.equals("if")
                || clean.equals("but")
                || clean.equals("thanks")
                || clean.equals("thank");
    }

    private static boolean isHighlightable(String command) {
        String name = commandName(command);
        return isHighlightableName(name);
    }

    private static boolean isHighlightableName(String name) {
        return AssistantCommandRegistry.specFor(name) != null || "drugcache".equals(name);
    }

    private static boolean overlaps(List<Span> spans, int start, int end) {
        for (Span span : spans) {
            if (start < span.end && end > span.start) return true;
        }
        return false;
    }

    private static String stripTrailingPunctuation(String command) {
        int end = command.length();
        while (end > 0) {
            char c = command.charAt(end - 1);
            if (c == '?' && end > 1 && command.charAt(end - 2) == '/') break;
            if (Character.isWhitespace(c) || ".,!*'\"`?\":;".indexOf(c) != -1) {
                end--;
            } else if (c == ')' || c == ']' || c == '}') {
                char opener = c == ')' ? '(' : (c == ']' ? '[' : '{');
                if (countChars(command, opener) < countChars(command, c)) {
                    end--;
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        return command.substring(0, end);
    }

    private static int countChars(String s, char c) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) count++;
        }
        return count;
    }

    public static final class Span {
        public final int start;
        public final int end;
        public final String command;
        public final boolean isSuggestion;

        Span(int start, int end, String command) {
            this(start, end, command, false);
        }

        Span(int start, int end, String command, boolean isSuggestion) {
            this.start = start;
            this.end = end;
            this.command = command;
            this.isSuggestion = isSuggestion;
        }
    }
}
