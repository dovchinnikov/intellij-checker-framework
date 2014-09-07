package com.jetbrains.plugins.checkerframework.util;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CheckerFrameworkOutputParser {

    private static final String  DELIMITER        = "\\$\\$";
    private static final Pattern CODE_PATTERN     = Pattern.compile("^\\(([\\w\\.]+)\\)$");
    private static final Pattern POSITION_PATTERN = Pattern.compile("^\\(\\s?(\\d+)\\s?,\\s?(\\d+)\\s?\\)$");

    private final @NotNull String   content;
    private @NotNull       String   code;
    private @NotNull       String[] stuff;
    private                int      startPosition;
    private                int      endPosition;
    private @NotNull       String   message;
    private                boolean  parsed;

    public CheckerFrameworkOutputParser(@NotNull final String content) {
        this.content = content;
    }

    public @NotNull String getCode() {
        if (!parsed) doParse();
        return code;
    }

    public @NotNull String[] getStuff() {
        if (!parsed) doParse();
        return stuff;
    }

    public int getStartPosition() {
        if (!parsed) doParse();
        return startPosition;
    }

    public int getEndPosition() {
        if (!parsed) doParse();
        return endPosition;
    }

    public @NotNull String getMessage() {
        if (!parsed) doParse();
        return message;
    }

    private void doParse() {
        final String[] chunks = content.split(DELIMITER);
        int nextChunk = 0;

        final Matcher codeMatcher = CODE_PATTERN.matcher(chunks[nextChunk++].trim());
        assert codeMatcher.matches();
        code = codeMatcher.group(1);

        final int stuffLength = Integer.parseInt(chunks[nextChunk++].trim());
        stuff = new String[stuffLength];
        for (int i = 0; i < stuffLength; i++) {
            stuff[i] = chunks[nextChunk++].trim();
        }

        final Matcher positionMatcher = POSITION_PATTERN.matcher(chunks[nextChunk++].trim());
        assert positionMatcher.matches();
        startPosition = Integer.parseInt(positionMatcher.group(1));
        endPosition = Integer.parseInt(positionMatcher.group(2));

        message = chunks[nextChunk].substring(0, chunks[nextChunk].indexOf('.')).trim();
        nextChunk++;
        parsed = nextChunk == chunks.length;

        assert parsed;
    }
}
