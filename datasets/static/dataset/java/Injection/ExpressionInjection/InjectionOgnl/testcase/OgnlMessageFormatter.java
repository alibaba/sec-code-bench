<filename>biobank-development/eclipse_ws/biobank.common/src/edu/ualberta/med/biobank/validator/messageinterpolator/OgnlMessageFormatter.java<fim_prefix>

package edu.ualberta.med.biobank.validator.messageinterpolator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ognl.Ognl;
import ognl.OgnlException;

/**
 * Like {@link java.text.MessageFormat}, but replaces OGNL expressions with
 * their evaluation using some context.
 * 
 * @author Jonathan Ferland
 */
public class OgnlMessageFormatter {
    @SuppressWarnings("nls")
    private static final Pattern VARIABLE_START_PATTERN = Pattern
        .compile("\\$\\{");

    /**
     * Enables the OGNL evaluation of any substring of the format
     * <code>$&#123;ognl_expression&#125;</code>, within a given message
     * {@link String}. Format the message by replacing OGNL expressions with
     * their evaluated result.
     * 
     * @param message may contain OGNL expressions for evaluation
     * @param root object providing the context of the OGNL evaluation
     * 
     * @return the formatted message
     */
    public static String format(String message, Object root) {
        String interpolatedMessage = message;
        Matcher matcher = VARIABLE_START_PATTERN.matcher(message);

        while (matcher.find()) {
            int curlyBraceOpenings = 1;
            boolean inDoubleQuotes = false;
            boolean inSingleQuotes = false;
            int lastIndex = matcher.end();

            do {
                char current = message.charAt(lastIndex);

                if (current == '\'') {
                    if (!inDoubleQuotes && !isEscaped(message, lastIndex)) {
                        inSingleQuotes = !inSingleQuotes;
                    }
                } else if (current == '"') {
                    if (!inSingleQuotes && !isEscaped(message, lastIndex)) {
                        inDoubleQuotes = !inDoubleQuotes;
                    }
                } else if (!inDoubleQuotes && !inSingleQuotes) {
                    if (current == '{') {
                        curlyBraceOpenings++;
                    } else if (current == '}') {
                        curlyBraceOpenings--;
                    }
                }

                lastIndex++;

            } while (curlyBraceOpenings > 0 && lastIndex < message.length());

            // The validated value expression seems correct
            if (curlyBraceOpenings == 0) {
                String variable = message.substring(matcher.start(), lastIndex);
                String ognlExpression = extractContents(variable);

                String evaluation = evaluateOgnl(ognlExpression, root);

                String escapedVariable = Pattern.quote(variable);
                String escapedEvaluation = Matcher.quoteReplacement(evaluation);

                interpolatedMessage = interpolatedMessage.replaceFirst(
                    escapedVariable,
                    escapedEvaluation);
            }
        }

        return interpolatedMessage;
    }

    /**
     * 
     * @param variable a string matching <code>$&#123;contents&#125;</code>
     * @return contents
     */
    @SuppressWarnings("nls")
    private static String extractContents(String variable) {
        int start = 2;
        int end = variable.length() - 1;
        if (end < start) {
            throw new IndexOutOfBoundsException("");
        }
        return variable.substring(start, end);
    }

    /**
     * Return true if the char at the given index is preceded by a backslash in
     * the containing String.
     * 
     * @param string the containing string
     * @param charIndex the index of the character
     * 
     * @return true if the given character is escaped, otherwise false
     */
    @SuppressWarnings("nls")
    private static boolean isEscaped(String string, int charIndex) {
        if (charIndex < 0 || charIndex > string.length()) {
            throw new IndexOutOfBoundsException(
                "Index must be between 0 and string.length() - 1.");
        }
        return charIndex > 0 && string.charAt(charIndex - 1) == '\\';
    }

    private static String evaluateOgnl(String expr, Object root) {
        String result = null;
        try {
            <fim_suffix>
            result = String.valueOf(value);
        } catch (OgnlException e) {
            // TODO: something better?
            throw new RuntimeException(e);
        }
        return result;
    }
}
<fim_middle>