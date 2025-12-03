package tech.smartboot.feat.core.client.stream;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Inflater;
import java.util.zip.ZipException;

/**
 * GZIP输入流实现类，用于解压GZIP格式的压缩数据
 * <p>
 * GZIP是一种广泛使用的数据压缩格式，通常用于HTTP传输、文件存储等场景。
 * 该类实现了GZIP格式的解压缩，遵循RFC 1952规范。
 * </p>
 * <p>
 * GZIP文件格式结构：
 * <pre>
 * +---+---+---+---+---+---+---+---+---+---+
 * |ID1|ID2|CM |FLG|     MTIME     |XFL|OS | (more-->)
 * +---+---+---+---+---+---+---+---+---+---+
 * 
 * (if FLG.FEXTRA set)
 * +---+---+=================================+
 * | XLEN  |...XLEN bytes of "extra field"...| (more-->)
 * +---+---+=================================+
 * 
 * (if FLG.FNAME set)
 * +=========================================+
 * |...original file name, zero-terminated...| (more-->)
 * +=========================================+
 * 
 * (if FLG.FCOMMENT set)
 * +===================================+
 * |...file comment, zero-terminated...| (more-->)
 * +===================================+
 * 
 * (if FLG.FHCRC set)
 * +---+---+
 * | CRC16 |
 * +---+---+
 * 
 * +=======================+
 * |...compressed blocks...| (more-->)
 * +=======================+
 * 
 * +---+---+---+---+---+---+---+---+
 * |     CRC32     |     ISIZE     |
 * +---+---+---+---+---+---+---+---+
 * </pre>
 * 其中：
 * - ID1,ID2: 魔数标识，固定为0x1f, 0x8b
 * - CM: 压缩方法，目前只支持deflate(值为8)
 * - FLG: 标志位，指示是否存在额外字段、文件名、注释等
 * - MTIME: 修改时间
 * - XFL: 额外标志
 * - OS: 操作系统类型
 * - CRC32: 未压缩数据的CRC32校验值
 * - ISIZE: 未压缩数据大小的低32位
 * </p>
 *
 * @author 三刀
 * @see InflaterInputStream
 * @since 1.0
 */
public class GZIPInputStream extends InflaterInputStream {
    /**
     * 用于计算未压缩数据的CRC-32校验值。
     * 在解压过程中会持续更新该值，最后与GZIP尾部的CRC32值进行比较验证数据完整性。
     */
    protected CRC32 crc = new CRC32();
    
    /**
     * 包装原始输入流的校验输入流，用于计算GZIP头部部分的CRC值。
     * 主要用于处理FLG.FHCRC标志位设置时的头部校验。
     */
    private final CheckedInputStream headInput;
    
    /**
     * 标识是否已到达输入流的结束位置。
     * 当处理完GZIP尾部数据后，该标志会被设置为true。
     */
    protected boolean eos;

    /**
     * 标识当前流是否已被关闭。
     * 用于防止对已关闭流的重复操作。
     */
    private boolean closed = false;
    
    // GZIP解析过程中的各种状态定义
    
    /** 状态：检查GZIP魔数 */
    private static final int STATE_MAGIC = 0;
    /** 状态：检查压缩方法 */
    private static final int STATE_COMPRESSION_METHOD = 1;
    /** 状态：解析标志位 */
    private static final int STATE_FLAGS = 2;
    /** 状态：解析额外字段长度 */
    private static final int STATE_FEXTRA_LEN = 3;
    /** 状态：解析额外字段数据 */
    private static final int STATE_FEXTRA_DATA = 4;
    /** 状态：解析文件名 */
    private static final int STATE_FNAME = 5;
    /** 状态：解析文件注释 */
    private static final int STATE_FCOMMENT = 6;
    /** 状态：校验头部CRC */
    private static final int STATE_HCRC = 7;
    /** 状态：执行inflate解压缩 */
    private static final int STATE_INFLATE = 10;
    /** 状态：检查尾部CRC和大小 */
    private static final int STATE_CRC_CHECK = 20;

    /**
     * 当前解析状态，默认从检查魔数开始。
     */
    private int state = STATE_MAGIC;
    
    /**
     * 存储从GZIP头部解析出的标志位。
     * 用于确定是否存在额外字段、文件名、注释等内容。
     */
    private int flags;
    
    /**
     * 剩余待处理的额外字段数据长度。
     * 当FLG.FEXTRA被设置时使用。
     */
    private int extraReaming;
    
    /**
     * 临时缓冲区，用于跳过不需要的数据。
     */
    private final static byte[] tmpbuf = new byte[128];
    
    /**
     * GZIP头部魔数，固定值为0x8b1f。
     * 用于识别GZIP格式文件。
     */
    public final static int GZIP_MAGIC = 0x8b1f;

    /*
     * GZIP头部标志位常量定义
     */
    /** FTEXT标志：表示文件是文本文件 */
    private final static int FTEXT = 1;    // Extra text
    /** FHCRC标志：表示存在头部CRC16校验 */
    private final static int FHCRC = 2;    // Header CRC
    /** FEXTRA标志：表示存在额外字段 */
    private final static int FEXTRA = 4;    // Extra field
    /** FNAME标志：表示存在原始文件名 */
    private final static int FNAME = 8;    // File name
    /** FCOMMENT标志：表示存在文件注释 */
    private final static int FCOMMENT = 16;   // File comment

    /**
     * 检查确保此流尚未关闭
     */
    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }
    }

    /**
     * 创建具有指定缓冲区大小的新输入流。
     * <p>
     * 该构造函数会初始化Inflater对象用于解压缩，并创建CheckedInputStream用于计算头部CRC。
     * 同时会设置usesDefaultInflater标志为true，表明使用的是默认的Inflater实例。
     * </p>
     *
     * @param in   输入流，应包含有效的GZIP格式数据
     * @param size 输入缓冲区大小，必须大于0
     * @throws ZipException             如果发生GZIP格式错误或使用的压缩方法不受支持
     * @throws IOException              如果发生了I/O错误
     * @throws IllegalArgumentException 如果{@code size <= 0}
     */
    public GZIPInputStream(InputStream in, int size) throws IOException {
        super(in, new Inflater(true), size);
        usesDefaultInflater = true;
        headInput = new CheckedInputStream(in, crc);
    }

    /**
     * 创建具有默认缓冲区大小的新输入流。
     * <p>
     * 使用默认缓冲区大小512字节调用带size参数的构造函数。
     * </p>
     *
     * @param in 输入流，应包含有效的GZIP格式数据
     * @throws ZipException 如果发生GZIP格式错误或使用的压缩方法不受支持
     * @throws IOException  如果发生了I/O错误
     */
    public GZIPInputStream(InputStream in) throws IOException {
        this(in, 512);
    }

    /**
     * 将未压缩的数据读入字节数组。如果<code>len</code>不为零，
     * 该方法将阻塞直到某些输入可以解压缩；否则，
     * 不读取任何字节并返回<code>0</code>。
     * <p>
     * 该方法按照GZIP格式规范逐步解析输入流：
     * 1. 检查GZIP魔数标识
     * 2. 验证压缩方法（仅支持deflate）
     * 3. 解析标志位和相关字段
     * 4. 处理额外字段、文件名、注释等可选内容
     * 5. 执行头部CRC校验（如需要）
     * 6. 执行实际的数据解压缩
     * 7. 验证尾部CRC和数据大小
     * </p>
     *
     * @param buf 数据读入的缓冲区
     * @param off 目标数组<code>b</code>中的起始偏移量
     * @param len 要读取的最大字节数
     * @return 实际读取的字节数，如果到达压缩输入流的末尾则返回-1
     * @throws NullPointerException      如果<code>buf</code>为<code>null</code>。
     * @throws IndexOutOfBoundsException 如果<code>off</code>为负数，
     *                                   <code>len</code>为负数，或者<code>len</code>大于
     *                                   <code>buf.length - off</code>
     * @throws ZipException              如果压缩的输入数据已损坏。
     * @throws IOException               如果发生了I/O错误。
     */
    public int read(byte[] buf, int off, int len) throws IOException {
        ensureOpen();
        if (eos) {
            return -1;
        }
        switch (state) {
            case STATE_MAGIC: {
                // 检查是否有足够的数据读取魔数（2字节）
                if (headInput.available() < 2) {
                    return 0;
                }
                // 检查GZIP魔数标识，必须为0x1f8b
                if (readUShort(headInput) != GZIP_MAGIC) {
                    throw new ZipException("Not in GZIP format");
                }
                // 魔数检查通过，进入下一个状态：检查压缩方法
                state = STATE_COMPRESSION_METHOD;
            }
            case STATE_COMPRESSION_METHOD: {
                // 检查是否有足够的数据读取压缩方法（1字节）
                if (headInput.available() < 1) {
                    return 0;
                }
                // 检查压缩方法，GZIP只支持deflate算法（值为8）
                if (readUByte(headInput) != 8) {
                    throw new ZipException("Unsupported compression method");
                }
                // 压缩方法检查通过，进入下一个状态：解析标志位
                state = STATE_FLAGS;
            }
            case STATE_FLAGS: {
                // 检查是否有足够的数据读取标志位及其他头部字段（共7字节）
                // 包括：FLG(1字节) + MTIME(4字节) + XFL(1字节) + OS(1字节)
                if (headInput.available() < 7) {
                    return 0;
                }
                // 读取标志位
                flags = readUByte(headInput);
                // 跳过MTIME(4字节)、XFL(1字节)、OS(1字节)，共6字节
                // 这些字段在解压过程中不是必需的
                skipBytes(headInput, 6);
                // 根据标志位判断下一步处理
                if ((flags & FEXTRA) == FEXTRA) {
                    // 存在额外字段，进入处理额外字段长度的状态
                    state = STATE_FEXTRA_LEN;
                } else {
                    // 不存在额外字段，直接进入处理文件名状态
                    state = STATE_FNAME;
                    // 递归调用继续处理下一阶段
                    return this.read(buf, off, len);
                }
            }
            case STATE_FEXTRA_LEN: {
                // 检查是否有足够的数据读取额外字段长度（2字节）
                if (headInput.available() < 2) {
                    return 0;
                }
                // 读取额外字段长度
                extraReaming = readUShort(headInput);
                // 进入处理额外字段数据状态
                state = STATE_FEXTRA_DATA;
            }
            case STATE_FEXTRA_DATA: {
                // 处理额外字段数据
                if (extraReaming > 0) {
                    // 还有额外字段数据需要处理
                    if (headInput.available() == 0) {
                        // 暂时没有可用数据，返回0让上层再次尝试读取
                        return 0;
                    }
                    // 计算本次可以处理的字节数
                    int n = Math.min(extraReaming, headInput.available());
                    // 跳过这些额外字段数据
                    skipBytes(headInput, n);
                    // 更新剩余待处理的额外字段长度
                    extraReaming -= n;
                    // 递归调用继续处理
                    return this.read(buf, off, len);
                } else if (extraReaming == 0) {
                    // 额外字段处理完毕，进入处理文件名状态
                    state = STATE_FNAME;
                } else {
                    // 额外字段长度出现异常
                    throw new ZipException("Invalid extra data size");
                }
            }
            case STATE_FNAME: {
                // 处理原始文件名（如果存在）
                if ((flags & FNAME) == FNAME) {
                    // 存在文件名字段，需要读取到第一个0字节为止
                    while (headInput.available() > 0) {
                        int c = headInput.read();
                        if (c == -1) {
                            // 流意外结束
                            throw new ZipException("Unexpected end of stream");
                        } else if (c == 0) {
                            // 遇到终止符0，文件名处理完毕
                            state = STATE_FCOMMENT;
                            // 递归调用继续处理下一阶段
                            return this.read(buf, off, len);
                        }
                        // 继续读取文件名的下一个字符
                    }
                    // 当前没有足够数据，返回0让上层再次尝试读取
                    return 0;
                } else {
                    // 不存在文件名字段，直接进入处理注释状态
                    state = STATE_FCOMMENT;
                }
            }
            case STATE_FCOMMENT: {
                // 处理文件注释（如果存在）
                if ((flags & FCOMMENT) == FCOMMENT) {
                    // 存在注释字段，需要读取到第一个0字节为止
                    while (headInput.available() > 0) {
                        int c = headInput.read();
                        if (c == -1) {
                            // 流意外结束
                            throw new ZipException("Unexpected end of stream");
                        } else if (c == 0) {
                            // 遇到终止符0，注释处理完毕
                            state = STATE_HCRC;
                            // 递归调用继续处理下一阶段
                            return this.read(buf, off, len);
                        }
                        // 继续读取注释的下一个字符
                    }
                    // 当前没有足够数据，返回0让上层再次尝试读取
                    return 0;
                } else {
                    // 不存在注释字段，直接进入头部CRC校验状态
                    state = STATE_HCRC;
                }
            }
            case STATE_HCRC: {
                // 处理头部CRC校验（如果需要）
                if ((flags & FHCRC) == FHCRC) {
                    // 需要校验头部CRC
                    if (headInput.available() < 2) {
                        // 数据不足，返回0让上层再次尝试读取
                        return 0;
                    }
                    // 获取当前已读取头部数据的CRC值（低16位）
                    int v = (int) crc.getValue() & 0xffff;
                    // 读取存储在GZIP中的头部CRC值并与计算值比较
                    if (readUShort(headInput) != v) {
                        throw new ZipException("Corrupt GZIP header");
                    }
                }
                // 头部处理完毕，进入数据解压阶段
                state = STATE_INFLATE;
                // 重置CRC计算器，准备计算未压缩数据的CRC
                crc.reset();
            }
            case STATE_INFLATE: {
                // 执行实际的数据解压缩
                int n = super.read(buf, off, len);
                if (n == -1) {
                    // 解压完成，进入尾部校验阶段
                    state = STATE_CRC_CHECK;
                } else {
                    // 成功读取数据，更新CRC值
                    crc.update(buf, off, n);
                    // 返回读取到的字节数
                    return n;
                }
            }
            case STATE_CRC_CHECK: {
                // 检查是否有足够的数据读取尾部（至少8字节）
                if ((inf.getRemaining() + in.available()) < 8) {
                    return 0;
                } else if (readTrailer()) {
                    // 尾部校验通过，标记流结束
                    eos = true;
                    return -1;
                } else {
                    // 同JDK源码不一致，此处中断阻塞
                    return this.read(buf, off, len);
                }
            }
        }
        throw new IllegalStateException();
    }

    /**
     * 关闭此输入流并释放与该流关联的任何系统资源。
     * <p>
     * 关闭操作会设置eos标志为true，并将closed标志设置为true，
     * 防止后续对已关闭流的操作。
     * </p>
     *
     * @throws IOException 如果发生了I/O错误
     */
    public void close() throws IOException {
        if (!closed) {
            super.close();
            eos = true;
            closed = true;
        }
    }


    /*
     * 读取GZIP成员尾部，如果到达eos则返回true，
     * 如果还有更多（连接的gzip数据集）则返回false
     * 
     * GZIP尾部包含两个重要字段：
     * 1. CRC32：未压缩数据的CRC32校验值（4字节）
     * 2. ISIZE：未压缩数据大小的低32位（4字节）
     */
    private boolean readTrailer() throws IOException {
        InputStream in = this.in;
        int n = inf.getRemaining();
        if (n > 0) {
            // 如果还有未处理的数据，构建一个新的序列输入流来处理
            in = new SequenceInputStream(
                new ByteArrayInputStream(buf, len - n, n), 
                new FilterInputStream(in) {
                    public void close() throws IOException {
                        // 重写close方法避免关闭原始输入流
                    }
                }
            );
        }
        // 使用从左到右的求值顺序进行校验
        // 1. 比较存储的CRC32值与实际计算的CRC32值
        // 2. 比较存储的ISIZE值与实际解压数据大小的低32位
        if ((readUInt(in) != crc.getValue()) ||
                // rfc1952; ISIZE is the input size modulo 2^32
                (readUInt(in) != (inf.getBytesWritten() & 0xffffffffL))) 
            throw new ZipException("Corrupt GZIP trailer");

        // If there are more bytes available in "in" or
        // the leftover in the "inf" is > 26 bytes:
        // this.trailer(8) + next.header.min(10) + next.trailer(8)
        // try concatenated case
        if (this.in.available() > 0 || n > 26) {
            // 当前实现不支持连接的gzip流，抛出异常
            throw new UnsupportedEncodingException("不支持连接的gzip流");
//            int m = 8;                  // this.trailer
//            try {
//                m += readHeader(in);    // next.header
//            } catch (IOException ze) {
//                return true;  // ignore any malformed, do nothing
//            }
//            inf.reset();
//            if (n > m) inf.setInput(buf, len - n + m, n - m);
//            return false;
        }
        return true;
    }

    /*
     * 以Intel字节序读取无符号整数（4字节）
     * GZIP格式使用小端序（Little Endian）存储多字节数值
     */
    private long readUInt(InputStream in) throws IOException {
        long s = readUShort(in);
        return ((long) readUShort(in) << 16) | s;
    }

    /*
     * 以Intel字节序读取无符号短整数（2字节）
     * GZIP格式使用小端序（Little Endian）存储多字节数值
     */
    private int readUShort(InputStream in) throws IOException {
        int b = readUByte(in);
        return (readUByte(in) << 8) | b;
    }

    /*
     * 读取无符号字节
     * 处理Java中byte是有符号类型的问题，将其转换为0-255范围的int值
     */
    private int readUByte(InputStream in) throws IOException {
        int b = in.read();
        if (b == -1) {
            // 读取到流末尾，抛出EOFException
            throw new EOFException();
        }
        return b;
    }


    /*
     * 跳过指定数量的输入数据字节，阻塞直到所有字节都被跳过
     * 不假定输入流具备随机访问（seek）能力
     * 
     * @param in 输入流
     * @param n  需要跳过的字节数
     */
    private void skipBytes(InputStream in, int n) throws IOException {
        while (n > 0) {
            // 使用临时缓冲区分批读取并丢弃数据
            int len = in.read(tmpbuf, 0, Math.min(n, tmpbuf.length));
            if (len == -1) {
                // 意外遇到流末尾，抛出EOFException
                throw new EOFException();
            }
            // 更新还需跳过的字节数
            n -= len;
        }
    }
}
