/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */
package tech.smartboot.feat.core.common.codec.h2.hpack;

import java.nio.ByteBuffer;

/**
 * Delivers results of the {@link Decoder#decode(ByteBuffer, boolean,
 * DecodingCallback) decoding operation}.
 *
 * <p> Methods of the callback are never called by a decoder with any of the
 * arguments being {@code null}.
 *
 * @apiNote <p> The callback provides methods for all possible
 * <a href="https://tools.ietf.org/html/rfc7541#section-6">binary representations</a>.
 * This could be useful for implementing an intermediary, logging, debugging,
 * etc.
 *
 * <p> The callback is an interface in order to interoperate with lambdas (in
 * the most common use case):
 * <pre>{@code
 *     DecodingCallback callback = (name, value) -> System.out.println(name + ", " + value);
 * }</pre>
 *
 * <p> Names and values are {@link CharSequence}s rather than {@link String}s in
 * order to allow users to decide whether or not they need to create objects. A
 * {@code CharSequence} might be used in-place, for example, to be appended to
 * an {@link Appendable} (e.g. {@link StringBuilder}) and then discarded.
 *
 * <p> That said, if a passed {@code CharSequence} needs to outlast the method
 * call, it needs to be copied.
 * @since 9
 */
@FunctionalInterface
public interface DecodingCallback {

    /**
     * A method the more specific methods of the callback forward their calls
     * to.
     *
     * @param name  header name
     * @param value header value
     */
    void onDecoded(CharSequence name, CharSequence value);

    /**
     * A more finer-grained version of {@link #onDecoded(CharSequence,
     * CharSequence)} that also reports on value sensitivity.
     *
     * <p> Value sensitivity must be considered, for example, when implementing
     * an intermediary. A {@code value} is sensitive if it was represented as <a
     * href="https://tools.ietf.org/html/rfc7541#section-6.2.3">Literal Header
     * Field Never Indexed</a>.
     *
     * <p> It is required that intermediaries MUST use the {@linkplain
     * Encoder#header(CharSequence, CharSequence, boolean) same representation}
     * for encoding this header field in order to protect its value which is not
     * to be put at risk by compressing it.
     *
     * @param name      header name
     * @param value     header value
     * @param sensitive whether or not the value is sensitive
     * @implSpec <p> The default implementation invokes {@code onDecoded(name, value)}.
     * @see #onLiteralNeverIndexed(int, CharSequence, CharSequence, boolean)
     * @see #onLiteralNeverIndexed(CharSequence, boolean, CharSequence, boolean)
     */
    default void onDecoded(CharSequence name,
                           CharSequence value,
                           boolean sensitive) {
        onDecoded(name, value);
    }

    /**
     * An <a href="https://tools.ietf.org/html/rfc7541#section-6.1">Indexed
     * Header Field</a> decoded.
     *
     * @param index index of an entry in the table
     * @param name  header name
     * @param value header value
     * @implSpec <p> The default implementation invokes
     * {@code onDecoded(name, value, false)}.
     */
    default void onIndexed(int index, CharSequence name, CharSequence value) {
        onDecoded(name, value, false);
    }

    /**
     * A <a href="https://tools.ietf.org/html/rfc7541#section-6.2.2">Literal
     * Header Field without Indexing</a> decoded, where a {@code name} was
     * referred by an {@code index}.
     *
     * @param index        index of an entry in the table
     * @param name         header name
     * @param value        header value
     * @param valueHuffman if the {@code value} was Huffman encoded
     * @implSpec <p> The default implementation invokes
     * {@code onDecoded(name, value, false)}.
     */
    default void onLiteral(int index,
                           CharSequence name,
                           CharSequence value,
                           boolean valueHuffman) {
        onDecoded(name, value, false);
    }

    /**
     * A <a href="https://tools.ietf.org/html/rfc7541#section-6.2.2">Literal
     * Header Field without Indexing</a> decoded, where both a {@code name} and
     * a {@code value} were literal.
     *
     * @param name         header name
     * @param nameHuffman  if the {@code name} was Huffman encoded
     * @param value        header value
     * @param valueHuffman if the {@code value} was Huffman encoded
     * @implSpec <p> The default implementation invokes
     * {@code onDecoded(name, value, false)}.
     */
    default void onLiteral(CharSequence name,
                           boolean nameHuffman,
                           CharSequence value,
                           boolean valueHuffman) {
        onDecoded(name, value, false);
    }

    /**
     * A <a href="https://tools.ietf.org/html/rfc7541#section-6.2.3">Literal
     * Header Field Never Indexed</a> decoded, where a {@code name}
     * was referred by an {@code index}.
     *
     * @param index        index of an entry in the table
     * @param name         header name
     * @param value        header value
     * @param valueHuffman if the {@code value} was Huffman encoded
     * @implSpec <p> The default implementation invokes
     * {@code onDecoded(name, value, true)}.
     */
    default void onLiteralNeverIndexed(int index,
                                       CharSequence name,
                                       CharSequence value,
                                       boolean valueHuffman) {
        onDecoded(name, value, true);
    }

    /**
     * A <a href="https://tools.ietf.org/html/rfc7541#section-6.2.3">Literal
     * Header Field Never Indexed</a> decoded, where both a {@code
     * name} and a {@code value} were literal.
     *
     * @param name         header name
     * @param nameHuffman  if the {@code name} was Huffman encoded
     * @param value        header value
     * @param valueHuffman if the {@code value} was Huffman encoded
     * @implSpec <p> The default implementation invokes
     * {@code onDecoded(name, value, true)}.
     */
    default void onLiteralNeverIndexed(CharSequence name,
                                       boolean nameHuffman,
                                       CharSequence value,
                                       boolean valueHuffman) {
        onDecoded(name, value, true);
    }

    /**
     * A <a href="https://tools.ietf.org/html/rfc7541#section-6.2.1">Literal
     * Header Field with Incremental Indexing</a> decoded, where a {@code name}
     * was referred by an {@code index}.
     *
     * @param index        index of an entry in the table
     * @param name         header name
     * @param value        header value
     * @param valueHuffman if the {@code value} was Huffman encoded
     * @implSpec <p> The default implementation invokes
     * {@code onDecoded(name, value, false)}.
     */
    default void onLiteralWithIndexing(int index,
                                       CharSequence name,
                                       CharSequence value,
                                       boolean valueHuffman) {
        onDecoded(name, value, false);
    }

    /**
     * A <a href="https://tools.ietf.org/html/rfc7541#section-6.2.1">Literal
     * Header Field with Incremental Indexing</a> decoded, where both a {@code
     * name} and a {@code value} were literal.
     *
     * @param name         header name
     * @param nameHuffman  if the {@code name} was Huffman encoded
     * @param value        header value
     * @param valueHuffman if the {@code value} was Huffman encoded
     * @implSpec <p> The default implementation invokes
     * {@code onDecoded(name, value, false)}.
     */
    default void onLiteralWithIndexing(CharSequence name,
                                       boolean nameHuffman,
                                       CharSequence value,
                                       boolean valueHuffman) {
        onDecoded(name, value, false);
    }

    /**
     * A <a href="https://tools.ietf.org/html/rfc7541#section-6.3">Dynamic Table
     * Size Update</a> decoded.
     *
     * @param capacity new capacity of the header table
     * @implSpec <p> The default implementation does nothing.
     */
    default void onSizeUpdate(int capacity) {
    }
}
