package tech.smartboot.feat.core.common.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ParamDecodeUtils {

    /**
     * 十六进制值字符数组。
     */
    private static char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    /**
     * 127的十六进制表示。
     */
    private static byte MASK = 0x7f;

    /**
     * 128的十六进制表示。
     */
    private static int MASK_128 = 0x80;

    /**
     * 十六进制解码值。
     */
    private static byte[] HEX_DECODE = new byte[MASK_128];

    /**
     * 表示文本的标记是用BASE64算法编码的。
     */
    private static String BASE64_ENCODING_MARKER = "B";

    /**
     * 表示文本的标记是用QuotedPrintable算法编码。
     */
    private static String QUOTEDPRINTABLE_ENCODING_MARKER = "Q";

    /**
     * 如果文本包含任何编码标记，这些标记将被标记为"=?"。
     */
    private static String ENCODED_TOKEN_MARKER = "=?";

    /**
     * 如果文本包含任何编码标记，这些标记将以"=?"结束。
     */
    private static String ENCODED_TOKEN_FINISHER = "?=";

    /**
     * 线性空白字符序列。
     */
    private static String LINEAR_WHITESPACE = " \t\r\n";

    /**
     * MIME和Java字符集之间的映射。
     */
    private static Map<String, String> MIME2JAVA = new HashMap<>();

    static {
        MIME2JAVA.put("iso-2022-cn", "ISO2022CN");
        MIME2JAVA.put("iso-2022-kr", "ISO2022KR");
        MIME2JAVA.put("utf-8", "UTF8");
        MIME2JAVA.put("utf8", "UTF8");
        MIME2JAVA.put("ja_jp.iso2022-7", "ISO2022JP");
        MIME2JAVA.put("ja_jp.eucjp", "EUCJIS");
        MIME2JAVA.put("euc-kr", "KSC5601");
        MIME2JAVA.put("euckr", "KSC5601");
        MIME2JAVA.put("us-ascii", StandardCharsets.ISO_8859_1.name());
        MIME2JAVA.put("x-us-ascii", StandardCharsets.ISO_8859_1.name());

        // 创建一个ASCII解码的十六进制值数组
        for (int i = 0; i < HEX_DIGITS.length; i++) {
            HEX_DECODE[HEX_DIGITS[i]] = (byte) i;
            HEX_DECODE[Character.toLowerCase(HEX_DIGITS[i])] = (byte) i;
            HEX_DECODE[Character.toLowerCase(HEX_DIGITS[i])] = (byte) i;
        }
    }


    public static String decodeRFC2231Text(String encodedText) throws UnsupportedEncodingException {
        int firstQuoteIndex = encodedText.indexOf('\'');
        if (firstQuoteIndex == -1) {
            return encodedText;
        }
        int secondQuoteIndex = encodedText.indexOf('\'', firstQuoteIndex + 1);
        if (secondQuoteIndex == -1) {
            return encodedText;
        }

        String charset = encodedText.substring(0, firstQuoteIndex);
        String encodedBytesPart = encodedText.substring(secondQuoteIndex + 1);

        byte[] decodedBytes = fromHex(encodedBytesPart);
        return new String(decodedBytes, charset);
    }

    /**
     * 转为16进制的字节数组
     *
     * @param text   ASCII 文本
     * @return 16进制的字节数组
     */
    private static byte[] fromHex(String text) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(text.length());

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '%' && i + 2 < text.length()) {
                int hexValue = (HEX_DECODE[text.charAt(++i) & MASK] << 4) | HEX_DECODE[text.charAt(++i) & MASK];
                out.write(hexValue);
            } else {
                out.write((byte) c);
            }
        }

        return out.toByteArray();
    }


    /**
     * RFC 2047 Mime规范：如果文本包含任何编码标记，这些标记将被标记为"=?"。如果源字符串不包含该序列，则不需要解码。
     * @param text
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String decodeRFC2047Text(String text) throws UnsupportedEncodingException {
        if (!text.contains(ENCODED_TOKEN_MARKER)) {
            return text;
        }

        int offset = 0;
        int endOffset = text.length();

        int startWhiteSpace = -1;
        int endWhiteSpace = -1;

        StringBuilder decodedText = new StringBuilder(text.length());

        boolean wasPreviousTokenEncoded = false;

        while (offset < endOffset) {
            int currentChar = text.charAt(offset);

            // 是否是空白字符？
            if (LINEAR_WHITESPACE.indexOf(currentChar) != -1) {
                startWhiteSpace = offset;
                while (offset < endOffset && LINEAR_WHITESPACE.indexOf(text.charAt(offset)) != -1) {
                    offset++;
                }
                endWhiteSpace = offset;
            } else {
                int wordStart = offset;

                // 扫描非空白字符以提取单词
                while (offset < endOffset && LINEAR_WHITESPACE.indexOf(text.charAt(offset)) == -1) {
                    offset++;
                }

                // 提取单词
                String word = text.substring(wordStart, offset);

                // 判断单词是否是编码的，若是则解码
                if (word.startsWith(ENCODED_TOKEN_MARKER)) {
                    try {
                        String decodedWord = decodeRFC2047Word(word);

                        // 如果上一个单词未编码且存在空白字符，则添加空白字符
                        if (!wasPreviousTokenEncoded && startWhiteSpace != -1) {
                            decodedText.append(text, startWhiteSpace, endWhiteSpace);
                        }

                        wasPreviousTokenEncoded = true;
                        decodedText.append(decodedWord);
                        continue;

                    } catch (ParseException ignored) {
                        // 忽略解析错误，继续处理下一个单词
                    }
                }

                // 添加之前的空白字符（若有）
                if (startWhiteSpace != -1) {
                    decodedText.append(text, startWhiteSpace, endWhiteSpace);
                }

                wasPreviousTokenEncoded = false;
                decodedText.append(word);
            }
        }

        return decodedText.toString();
    }

    private static String decodeRFC2047Word(String word) throws ParseException, UnsupportedEncodingException {
        // 编码的词汇以 "=? "开头。如果不是编码词汇，抛出 ParseException。

        int startMarkerPos = word.indexOf(ENCODED_TOKEN_MARKER);
        if (startMarkerPos != 0) {
            throw new ParseException("invalid coding vocabulary: " + word, startMarkerPos);
        }

        int charsetEndPos = word.indexOf('?', 2);
        if (charsetEndPos == -1) {
            throw new ParseException("missing character sets in encoding vocabulary: " + word, charsetEndPos);
        }

        // 提取字符集信息
        String charset = word.substring(2, charsetEndPos).toLowerCase(Locale.ROOT);

        // 提取编码信息
        int encodingEndPos = word.indexOf('?', charsetEndPos + 1);
        if (encodingEndPos == -1) {
            throw new ParseException("the lack of coding in the coding vocabulary: " + word, encodingEndPos);
        }

        String encoding = word.substring(charsetEndPos + 1, encodingEndPos);

        // 提取编码文本
        int textEndPos = word.indexOf(ENCODED_TOKEN_FINISHER, encodingEndPos + 1);
        if (textEndPos == -1) {
            throw new ParseException("coded text is missing from the coded vocabulary: " + word, textEndPos);
        }

        String encodedText = word.substring(encodingEndPos + 1, textEndPos);

        // 如果编码文本为空，直接返回空字符串
        if (encodedText.isEmpty()) {
            return "";
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream(encodedText.length())) {
            byte[] encodedData = encodedText.getBytes(StandardCharsets.US_ASCII);

            // 判断是Base64编码还是Quoted-Printable编码
            if (BASE64_ENCODING_MARKER.equals(encoding)) {
                out.write(Base64.getMimeDecoder().decode(encodedData));
            } else if (QUOTEDPRINTABLE_ENCODING_MARKER.equals(encoding)) {
                decodeQuotedPrintable(encodedData, out);
            } else {
                throw new UnsupportedEncodingException("unknown rfc2047 code: " + encoding);
            }

            // 将解码后的字节数据转换为字符串
            return out.toString(javaCharset(charset));
        } catch (IOException e) {
            throw new UnsupportedEncodingException("invalid rfc2047 encoding");
        }
    }


    /**
     * 将二进制数据编码成文本格式的方法。大多数字符保持原样（如 ASCII 字符 33-60, 62-126），
     * 但特殊字符（如不可打印字符、具有特殊意义的字符如 = 或某些协议不允许的字符）会被编码成 = 后跟两个十六进制数字，这两个数字表示该字节的值。
     */
    public static void decodeQuotedPrintable(byte[] data, OutputStream out) throws IOException {

        for (int i = 0; i < data.length; i++) {
            int ch = data[i];

            if (ch == '_') {
                out.write(' ');
            } else if (ch == '=') {
                if (i + 2 >= data.length) {
                    throw new IOException("Incomplete escape sequence");
                }

                int b1 = data[++i];
                int b2 = data[++i];

                if (!(b1 == '\r' && b2 == '\n')) {
                    out.write((hexToBinary(b1) << 4) | hexToBinary(b2));
                }
            } else {
                out.write(ch);
            }
        }

    }

    private static int hexToBinary(int b) throws IOException {
        int value = Character.digit(b, 16);
        if (value == -1) {
            throw new IOException("Invalid quoted printable encoding: not a valid hex digit: " + b);
        }
        return value;
    }

    /**
     * 判断参数末尾是否有"*"，有则代表需要解码
     *
     * @param paramName paramName
     * @return {@code true} 按照RFC 2231编码, {@code false} 否则
     */
    public static boolean hasEncodedValue(String paramName) {
        if (paramName != null) {
            return paramName.lastIndexOf('*') == paramName.length() - 1;
        }
        return false;
    }

    /**
     * 判断参数末尾是否有"*"，有则去除，否则返回原来的参数
     */
    public static String stripDelimiter(String paramName) {
        if (hasEncodedValue(paramName)) {
            StringBuilder paramBuilder = new StringBuilder(paramName);
            paramBuilder.deleteCharAt(paramName.lastIndexOf('*'));
            return paramBuilder.toString();
        }
        return paramName;
    }

    private static String javaCharset(String charset) {
        if (charset == null) {
            return null;
        }
        String mappedCharset = MIME2JAVA.get(charset.toLowerCase(Locale.ROOT));
        return mappedCharset == null ? charset : mappedCharset;
    }

}
