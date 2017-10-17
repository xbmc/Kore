package org.xbmc.kore.ui.sections.remote;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;

import org.xbmc.kore.R;

/**
 * Replaces some BBCode-ish tagged text with styled spans.
 * <p>
 * Only replaces the common tags, namely B, I, CR and UPPERCASE. This is
 * very strict/dumb, it only recognizes uppercase tags with no spaces around
 * the delimiters.
 */
public class KodiMarkupCode {
    private final TextAppearanceSpan bold;
    private final TextAppearanceSpan italic;

    public KodiMarkupCode(Context context) {
        bold = new TextAppearanceSpan(context, R.style.TextAppearance_Bold);
        italic = new TextAppearanceSpan(context, R.style.TextAppearance_Italic);
    }

    /**
     * Converts a string to a spanned object with styles applied.
     * <p>
     * Basically walks through the string and finds open tags. It remembers
     * the index, skips to the content, copies the text until the closing tag
     * is found. It applies the appropriate style then repeats this until the
     * end. Works with nested tags, although I'm not sure if it works with
     * improperly nested tags.
     *
     * @param src The text to style
     * @return a styled CharSequence that can be passed to a TextView
     * or derivatives.
     */
    public SpannableStringBuilder format(String src) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        int capsStart = -1;
        int boldStart = -1;
        int italicStart = -1;
        for (int i = 0, length = src.length(); i < length;) {
            String s = src.substring(i);
            int n = sb.length();
            if (s.startsWith("[CR]")) {
                sb.append('\n');
                i += 4;
            } else if (s.startsWith("[UPPERCASE]")) {
                if (capsStart == -1) {
                    capsStart = n;
                }
                i += 11;
            } else if (s.startsWith("[B]")) {
                if (boldStart == -1) {
                    boldStart = n;
                }
                i += 3;
            } else if (s.startsWith("[I]")) {
                if (italicStart == -1) {
                    italicStart = n;
                }
                i += 3;
            } else if (s.startsWith("[/UPPERCASE]")) {
                if (capsStart != -1) {
                    String sub = sb.subSequence(capsStart, n).toString();
                    sb.replace(capsStart, n, sub.toUpperCase());
                }
                capsStart = -1;
                i += 12;
            } else if (s.startsWith("[/B]")) {
                if (boldStart != -1) {
                    sb.setSpan(bold, boldStart, n, 0);
                }
                boldStart = -1;
                i += 4;
            } else if (s.startsWith("[/I]")) {
                if (italicStart != -1) {
                    sb.setSpan(italic, italicStart, n, 0);
                }
                italicStart = -1;
                i += 4;
            } else {
                int nextBracket = s.indexOf('[');
                if (nextBracket == -1) {
                    sb.append(s);
                    break;
                } else if (nextBracket == 0) {
                    sb.append('[');
                    i += 1;
                } else {
                    sb.append(s, 0, nextBracket);
                    i += nextBracket;
                }
            }
        }
        return sb;
    }
}
