/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.alg.cnn;

/**
 * 卷积运算实现
 * <p>
 * 卷积是CNN的核心操作，通过卷积核在输入特征图上滑动，提取局部特征。
 * 支持多通道输入和多个卷积核。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class Convolution {

    /**
     * 执行2D卷积运算
     *
     * @param input       输入特征图 [channels][height][width]
     * @param kernels     卷积核 [numKernels][channels][kernelHeight][kernelWidth]
     * @param biases      偏置 [numKernels]
     * @param stride      步幅
     * @param padding     填充大小
     * @return 输出特征图 [numKernels][outputHeight][outputWidth]
     */
    public static double[][][] conv2d(double[][][] input, double[][][][] kernels,
                                       double[] biases, int stride, int padding) {
        int numKernels = kernels.length;
        int inputChannels = input.length;
        int inputHeight = input[0].length;
        int inputWidth = input[0][0].length;
        int kernelHeight = kernels[0][0].length;
        int kernelWidth = kernels[0][0][0].length;

        // 计算输出尺寸
        int outputHeight = (inputHeight + 2 * padding - kernelHeight) / stride + 1;
        int outputWidth = (inputWidth + 2 * padding - kernelWidth) / stride + 1;

        // 填充输入
        double[][][] paddedInput = padding > 0 ? pad(input, padding) : input;

        // 执行卷积
        double[][][] output = new double[numKernels][outputHeight][outputWidth];

        for (int k = 0; k < numKernels; k++) {
            for (int i = 0; i < outputHeight; i++) {
                for (int j = 0; j < outputWidth; j++) {
                    double sum = biases[k];
                    int startY = i * stride;
                    int startX = j * stride;

                    for (int c = 0; c < inputChannels; c++) {
                        for (int kh = 0; kh < kernelHeight; kh++) {
                            for (int kw = 0; kw < kernelWidth; kw++) {
                                sum += paddedInput[c][startY + kh][startX + kw] * kernels[k][c][kh][kw];
                            }
                        }
                    }
                    output[k][i][j] = sum;
                }
            }
        }

        return output;
    }

    /**
     * 对输入进行填充
     *
     * @param input   输入特征图
     * @param padding 填充大小
     * @return 填充后的特征图
     */
    public static double[][][] pad(double[][][] input, int padding) {
        int channels = input.length;
        int height = input[0].length;
        int width = input[0][0].length;

        double[][][] padded = new double[channels][height + 2 * padding][width + 2 * padding];

        for (int c = 0; c < channels; c++) {
            for (int i = 0; i < height; i++) {
                System.arraycopy(input[c][i], 0, padded[c][i + padding], padding, width);
            }
        }

        return padded;
    }

    /**
     * 计算卷积层输出尺寸
     *
     * @param inputSize   输入尺寸
     * @param kernelSize  卷积核尺寸
     * @param stride      步幅
     * @param padding     填充
     * @return 输出尺寸
     */
    public static int calculateOutputSize(int inputSize, int kernelSize, int stride, int padding) {
        return (inputSize + 2 * padding - kernelSize) / stride + 1;
    }

    /**
     * 转置卷积（用于上采样）
     *
     * @param input       输入特征图
     * @param kernels     转置卷积核
     * @param biases      偏置
     * @param stride      步幅
     * @param padding     填充
     * @return 输出特征图
     */
    public static double[][][] transposedConv2d(double[][][] input, double[][][][] kernels,
                                                 double[] biases, int stride, int padding) {
        int numKernels = kernels.length;
        int inputChannels = input.length;
        int inputHeight = input[0].length;
        int inputWidth = input[0][0].length;
        int kernelHeight = kernels[0][0].length;
        int kernelWidth = kernels[0][0][0].length;

        // 计算输出尺寸
        int outputHeight = (inputHeight - 1) * stride - 2 * padding + kernelHeight;
        int outputWidth = (inputWidth - 1) * stride - 2 * padding + kernelWidth;

        double[][][] output = new double[numKernels][outputHeight][outputWidth];

        // 初始化偏置
        for (int k = 0; k < numKernels; k++) {
            for (int i = 0; i < outputHeight; i++) {
                for (int j = 0; j < outputWidth; j++) {
                    output[k][i][j] = biases[k];
                }
            }
        }

        // 执行转置卷积
        for (int k = 0; k < numKernels; k++) {
            for (int c = 0; c < inputChannels; c++) {
                for (int i = 0; i < inputHeight; i++) {
                    for (int j = 0; j < inputWidth; j++) {
                        int startY = i * stride;
                        int startX = j * stride;

                        for (int kh = 0; kh < kernelHeight; kh++) {
                            for (int kw = 0; kw < kernelWidth; kw++) {
                                int outY = startY + kh - padding;
                                int outX = startX + kw - padding;
                                if (outY >= 0 && outY < outputHeight && outX >= 0 && outX < outputWidth) {
                                    output[k][outY][outX] += input[c][i][j] * kernels[k][c][kh][kw];
                                }
                            }
                        }
                    }
                }
            }
        }

        return output;
    }

    /**
     * 空洞卷积（Dilated Convolution）
     *
     * @param input       输入特征图
     * @param kernels     卷积核
     * @param biases      偏置
     * @param stride      步幅
     * @param padding     填充
     * @param dilation    空洞率
     * @return 输出特征图
     */
    public static double[][][] dilatedConv2d(double[][][] input, double[][][][] kernels,
                                              double[] biases, int stride, int padding, int dilation) {
        int numKernels = kernels.length;
        int inputChannels = input.length;
        int inputHeight = input[0].length;
        int inputWidth = input[0][0].length;
        int kernelHeight = kernels[0][0].length;
        int kernelWidth = kernels[0][0][0].length;

        // 计算有效卷积核尺寸
        int effectiveKernelHeight = (kernelHeight - 1) * dilation + 1;
        int effectiveKernelWidth = (kernelWidth - 1) * dilation + 1;

        // 计算输出尺寸
        int outputHeight = (inputHeight + 2 * padding - effectiveKernelHeight) / stride + 1;
        int outputWidth = (inputWidth + 2 * padding - effectiveKernelWidth) / stride + 1;

        // 填充输入
        double[][][] paddedInput = padding > 0 ? pad(input, padding) : input;

        double[][][] output = new double[numKernels][outputHeight][outputWidth];

        for (int k = 0; k < numKernels; k++) {
            for (int i = 0; i < outputHeight; i++) {
                for (int j = 0; j < outputWidth; j++) {
                    double sum = biases[k];
                    int startY = i * stride;
                    int startX = j * stride;

                    for (int c = 0; c < inputChannels; c++) {
                        for (int kh = 0; kh < kernelHeight; kh++) {
                            for (int kw = 0; kw < kernelWidth; kw++) {
                                int y = startY + kh * dilation;
                                int x = startX + kw * dilation;
                                sum += paddedInput[c][y][x] * kernels[k][c][kh][kw];
                            }
                        }
                    }
                    output[k][i][j] = sum;
                }
            }
        }

        return output;
    }

    /**
     * 可分离卷积（Separable Convolution）
     * 深度可分离卷积：先进行depthwise卷积，再进行pointwise卷积
     *
     * @param input           输入特征图
     * @param depthwiseKernels 深度卷积核 [channels][1][kernelH][kernelW]
     * @param pointwiseKernels 点卷积核 [numKernels][channels][1][1]
     * @param biases          偏置
     * @param stride          步幅
     * @param padding         填充
     * @return 输出特征图
     */
    public static double[][][] separableConv2d(double[][][] input, double[][][][] depthwiseKernels,
                                                double[][][][] pointwiseKernels, double[] biases,
                                                int stride, int padding) {
        // 第一步：Depthwise卷积
        int channels = input.length;
        int inputHeight = input[0].length;
        int inputWidth = input[0][0].length;
        int kernelHeight = depthwiseKernels[0][0].length;
        int kernelWidth = depthwiseKernels[0][0][0].length;

        int outputHeight = calculateOutputSize(inputHeight, kernelHeight, stride, padding);
        int outputWidth = calculateOutputSize(inputWidth, kernelWidth, stride, padding);

        double[][][] paddedInput = padding > 0 ? pad(input, padding) : input;

        // Depthwise卷积结果
        double[][][] depthwiseOutput = new double[channels][outputHeight][outputWidth];

        for (int c = 0; c < channels; c++) {
            for (int i = 0; i < outputHeight; i++) {
                for (int j = 0; j < outputWidth; j++) {
                    double sum = 0;
                    int startY = i * stride;
                    int startX = j * stride;

                    for (int kh = 0; kh < kernelHeight; kh++) {
                        for (int kw = 0; kw < kernelWidth; kw++) {
                            sum += paddedInput[c][startY + kh][startX + kw] * depthwiseKernels[c][0][kh][kw];
                        }
                    }
                    depthwiseOutput[c][i][j] = sum;
                }
            }
        }

        // 第二步：Pointwise卷积（1x1卷积）
        return conv2d(depthwiseOutput, pointwiseKernels, biases, 1, 0);
    }

    /**
     * 初始化卷积核（He初始化）
     *
     * @param numKernels    卷积核数量
     * @param channels      输入通道数
     * @param kernelHeight  卷积核高度
     * @param kernelWidth   卷积核宽度
     * @return 初始化的卷积核
     */
    public static double[][][][] initializeKernels(int numKernels, int channels,
                                                    int kernelHeight, int kernelWidth) {
        double[][][][] kernels = new double[numKernels][channels][kernelHeight][kernelWidth];
        double scale = Math.sqrt(2.0 / (channels * kernelHeight * kernelWidth));

        for (int k = 0; k < numKernels; k++) {
            for (int c = 0; c < channels; c++) {
                for (int i = 0; i < kernelHeight; i++) {
                    for (int j = 0; j < kernelWidth; j++) {
                        kernels[k][c][i][j] = randomNormal() * scale;
                    }
                }
            }
        }

        return kernels;
    }

    /**
     * 标准正态分布随机数
     */
    private static double randomNormal() {
        double u1 = Math.random();
        double u2 = Math.random();
        return Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
    }
}
