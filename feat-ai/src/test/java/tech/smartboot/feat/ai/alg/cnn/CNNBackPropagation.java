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
 * CNN反向传播实现
 * <p>
 * 实现卷积神经网络的反向传播算法，包括：
 * - 卷积层梯度计算
 * - 池化层梯度传播
 * - 参数更新
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class CNNBackPropagation {

    /**
     * 卷积层反向传播
     *
     * @param gradOutput    输出梯度 [numKernels][outH][outW]
     * @param input         输入特征图 [channels][inH][inW]
     * @param kernels       卷积核 [numKernels][channels][kH][kW]
     * @param stride        步幅
     * @param padding       填充
     * @return 包含输入梯度和卷积核梯度的结果
     */
    public static ConvGradResult convBackward(double[][][] gradOutput, double[][][] input,
                                               double[][][][] kernels, int stride, int padding) {
        int numKernels = kernels.length;
        int channels = input.length;
        int inputHeight = input[0].length;
        int inputWidth = input[0][0].length;
        int kernelHeight = kernels[0][0].length;
        int kernelWidth = kernels[0][0][0].length;
        int outputHeight = gradOutput[0].length;
        int outputWidth = gradOutput[0][0].length;

        // 填充输入
        double[][][] paddedInput = padding > 0 ? Convolution.pad(input, padding) : input;
        int paddedHeight = paddedInput[0].length;
        int paddedWidth = paddedInput[0][0].length;

        // 初始化梯度
        double[][][] gradInput = new double[channels][inputHeight][inputWidth];
        double[][][][] gradKernels = new double[numKernels][channels][kernelHeight][kernelWidth];
        double[] gradBiases = new double[numKernels];

        // 计算偏置梯度
        for (int k = 0; k < numKernels; k++) {
            for (int i = 0; i < outputHeight; i++) {
                for (int j = 0; j < outputWidth; j++) {
                    gradBiases[k] += gradOutput[k][i][j];
                }
            }
        }

        // 计算卷积核梯度和输入梯度
        for (int k = 0; k < numKernels; k++) {
            for (int c = 0; c < channels; c++) {
                for (int i = 0; i < outputHeight; i++) {
                    for (int j = 0; j < outputWidth; j++) {
                        int startY = i * stride;
                        int startX = j * stride;

                        double grad = gradOutput[k][i][j];

                        // 计算卷积核梯度
                        for (int kh = 0; kh < kernelHeight; kh++) {
                            for (int kw = 0; kw < kernelWidth; kw++) {
                                gradKernels[k][c][kh][kw] += grad * paddedInput[c][startY + kh][startX + kw];
                            }
                        }

                        // 计算输入梯度
                        for (int kh = 0; kh < kernelHeight; kh++) {
                            for (int kw = 0; kw < kernelWidth; kw++) {
                                int inY = startY + kh - padding;
                                int inX = startX + kw - padding;
                                if (inY >= 0 && inY < inputHeight && inX >= 0 && inX < inputWidth) {
                                    gradInput[c][inY][inX] += grad * kernels[k][c][kh][kw];
                                }
                            }
                        }
                    }
                }
            }
        }

        return new ConvGradResult(gradInput, gradKernels, gradBiases);
    }

    /**
     * 转置卷积反向传播
     *
     * @param gradOutput    输出梯度
     * @param input         输入
     * @param kernels       转置卷积核
     * @param stride        步幅
     * @param padding       填充
     * @return 梯度结果
     */
    public static ConvGradResult transposedConvBackward(double[][][] gradOutput, double[][][] input,
                                                        double[][][][] kernels, int stride, int padding) {
        int numKernels = kernels.length;
        int channels = input.length;
        int inputHeight = input[0].length;
        int inputWidth = input[0][0].length;
        int kernelHeight = kernels[0][0].length;
        int kernelWidth = kernels[0][0][0].length;

        // 计算输出尺寸
        int outputHeight = (inputHeight - 1) * stride - 2 * padding + kernelHeight;
        int outputWidth = (inputWidth - 1) * stride - 2 * padding + kernelWidth;

        // 初始化梯度
        double[][][] gradInput = new double[channels][inputHeight][inputWidth];
        double[][][][] gradKernels = new double[numKernels][channels][kernelHeight][kernelWidth];
        double[] gradBiases = new double[numKernels];

        // 计算偏置梯度
        for (int k = 0; k < numKernels; k++) {
            for (int i = 0; i < outputHeight; i++) {
                for (int j = 0; j < outputWidth; j++) {
                    gradBiases[k] += gradOutput[k][i][j];
                }
            }
        }

        // 计算梯度
        for (int k = 0; k < numKernels; k++) {
            for (int c = 0; c < channels; c++) {
                for (int i = 0; i < inputHeight; i++) {
                    for (int j = 0; j < inputWidth; j++) {
                        int startY = i * stride;
                        int startX = j * stride;

                        for (int kh = 0; kh < kernelHeight; kh++) {
                            for (int kw = 0; kw < kernelWidth; kw++) {
                                int outY = startY + kh - padding;
                                int outX = startX + kw - padding;

                                if (outY >= 0 && outY < outputHeight && outX >= 0 && outX < outputWidth) {
                                    double grad = gradOutput[k][outY][outX];
                                    gradKernels[k][c][kh][kw] += grad * input[c][i][j];
                                    gradInput[c][i][j] += grad * kernels[k][c][kh][kw];
                                }
                            }
                        }
                    }
                }
            }
        }

        return new ConvGradResult(gradInput, gradKernels, gradBiases);
    }

    /**
     * 深度可分离卷积反向传播
     *
     * @param gradOutput        输出梯度
     * @param input             输入
     * @param depthwiseKernels  深度卷积核
     * @param pointwiseKernels  点卷积核
     * @param stride            步幅
     * @param padding           填充
     * @return 梯度结果
     */
    public static SeparableConvGradResult separableConvBackward(
            double[][][] gradOutput, double[][][] input,
            double[][][][] depthwiseKernels, double[][][][] pointwiseKernels,
            int stride, int padding) {

        int channels = input.length;
        int inputHeight = input[0].length;
        int inputWidth = input[0][0].length;
        int kernelHeight = depthwiseKernels[0][0].length;
        int kernelWidth = depthwiseKernels[0][0][0].length;

        // 填充输入
        double[][][] paddedInput = padding > 0 ? Convolution.pad(input, padding) : input;

        // 第一步：计算pointwise卷积的梯度
        // 需要先进行depthwise卷积得到中间结果
        int outputHeight = (inputHeight - kernelHeight) / stride + 1;
        int outputWidth = (inputWidth - kernelWidth) / stride + 1;

        double[][][] depthwiseOutput = new double[channels][outputHeight][outputWidth];
        for (int c = 0; c < channels; c++) {
            for (int i = 0; i < outputHeight; i++) {
                for (int j = 0; j < outputWidth; j++) {
                    int startY = i * stride;
                    int startX = j * stride;
                    double sum = 0;
                    for (int kh = 0; kh < kernelHeight; kh++) {
                        for (int kw = 0; kw < kernelWidth; kw++) {
                            sum += paddedInput[c][startY + kh][startX + kw] * depthwiseKernels[c][0][kh][kw];
                        }
                    }
                    depthwiseOutput[c][i][j] = sum;
                }
            }
        }

        // Pointwise卷积反向传播
        ConvGradResult pointwiseGrad = convBackward(gradOutput, depthwiseOutput, pointwiseKernels, 1, 0);

        // 计算depthwise卷积的梯度
        double[][][][] gradDepthwiseKernels = new double[channels][1][kernelHeight][kernelWidth];
        double[][][] gradDepthwiseInput = new double[channels][outputHeight][outputWidth];

        int numKernels = pointwiseKernels.length;

        for (int c = 0; c < channels; c++) {
            for (int i = 0; i < outputHeight; i++) {
                for (int j = 0; j < outputWidth; j++) {
                    int startY = i * stride;
                    int startX = j * stride;

                    // 计算该位置的梯度
                    double grad = 0;
                    for (int k = 0; k < numKernels; k++) {
                        grad += pointwiseGrad.gradInput[c][i][j] * pointwiseKernels[k][c][0][0];
                    }

                    // 计算depthwise卷积核梯度
                    for (int kh = 0; kh < kernelHeight; kh++) {
                        for (int kw = 0; kw < kernelWidth; kw++) {
                            gradDepthwiseKernels[c][0][kh][kw] += grad * paddedInput[c][startY + kh][startX + kw];
                        }
                    }

                    // 传播梯度到depthwise输入
                    gradDepthwiseInput[c][i][j] = grad;
                }
            }
        }

        // 计算原始输入梯度
        double[][][] gradInput = new double[channels][inputHeight][inputWidth];
        for (int c = 0; c < channels; c++) {
            for (int i = 0; i < outputHeight; i++) {
                for (int j = 0; j < outputWidth; j++) {
                    int startY = i * stride;
                    int startX = j * stride;

                    for (int kh = 0; kh < kernelHeight; kh++) {
                        for (int kw = 0; kw < kernelWidth; kw++) {
                            int inY = startY + kh - padding;
                            int inX = startX + kw - padding;
                            if (inY >= 0 && inY < inputHeight && inX >= 0 && inX < inputWidth) {
                                gradInput[c][inY][inX] += gradDepthwiseInput[c][i][j] * depthwiseKernels[c][0][kh][kw];
                            }
                        }
                    }
                }
            }
        }

        return new SeparableConvGradResult(gradInput, gradDepthwiseKernels, pointwiseGrad.gradKernels,
                pointwiseGrad.gradBiases);
    }

    /**
     * 激活函数反向传播
     *
     * @param gradOutput  输出梯度
     * @param z           激活前的值
     * @param activation  激活函数类型
     * @return 输入梯度
     */
    public static double[][][] activationBackward(double[][][] gradOutput, double[][][] z,
                                                  ActivationType activation) {
        int channels = gradOutput.length;
        int height = gradOutput[0].length;
        int width = gradOutput[0][0].length;

        double[][][] gradInput = new double[channels][height][width];

        for (int c = 0; c < channels; c++) {
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    double grad = gradOutput[c][i][j];
                    double val = z[c][i][j];

                    switch (activation) {
                        case RELU:
                            gradInput[c][i][j] = val > 0 ? grad : 0;
                            break;
                        case LEAKY_RELU:
                            gradInput[c][i][j] = val > 0 ? grad : 0.01 * grad;
                            break;
                        case SIGMOID:
                            double s = sigmoid(val);
                            gradInput[c][i][j] = grad * s * (1 - s);
                            break;
                        case TANH:
                            double t = Math.tanh(val);
                            gradInput[c][i][j] = grad * (1 - t * t);
                            break;
                        case LINEAR:
                            gradInput[c][i][j] = grad;
                            break;
                    }
                }
            }
        }

        return gradInput;
    }

    /**
     * 展平操作（用于连接卷积层和全连接层）
     *
     * @param input  输入特征图 [channels][height][width]
     * @return 展平后的向量
     */
    public static double[] flatten(double[][][] input) {
        int channels = input.length;
        int height = input[0].length;
        int width = input[0][0].length;

        double[] output = new double[channels * height * width];
        int idx = 0;

        for (int c = 0; c < channels; c++) {
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    output[idx++] = input[c][i][j];
                }
            }
        }

        return output;
    }

    /**
     * 展平反向传播
     *
     * @param gradOutput  输出梯度（向量）
     * @param channels    通道数
     * @param height      高度
     * @param width       宽度
     * @return 输入梯度（特征图）
     */
    public static double[][][] flattenBackward(double[] gradOutput, int channels, int height, int width) {
        double[][][] gradInput = new double[channels][height][width];
        int idx = 0;

        for (int c = 0; c < channels; c++) {
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    gradInput[c][i][j] = gradOutput[idx++];
                }
            }
        }

        return gradInput;
    }

    /**
     * Sigmoid函数
     */
    private static double sigmoid(double x) {
        if (x < -500) return 0.0;
        if (x > 500) return 1.0;
        return 1.0 / (1.0 + Math.exp(-x));
    }

    /**
     * 激活函数类型枚举
     */
    public enum ActivationType {
        RELU, LEAKY_RELU, SIGMOID, TANH, LINEAR
    }

    /**
     * 卷积层梯度结果
     */
    public static class ConvGradResult {
        public final double[][][] gradInput;
        public final double[][][][] gradKernels;
        public final double[] gradBiases;

        public ConvGradResult(double[][][] gradInput, double[][][][] gradKernels, double[] gradBiases) {
            this.gradInput = gradInput;
            this.gradKernels = gradKernels;
            this.gradBiases = gradBiases;
        }
    }

    /**
     * 可分离卷积梯度结果
     */
    public static class SeparableConvGradResult {
        public final double[][][] gradInput;
        public final double[][][][] gradDepthwiseKernels;
        public final double[][][][] gradPointwiseKernels;
        public final double[] gradBiases;

        public SeparableConvGradResult(double[][][] gradInput, double[][][][] gradDepthwiseKernels,
                                       double[][][][] gradPointwiseKernels, double[] gradBiases) {
            this.gradInput = gradInput;
            this.gradDepthwiseKernels = gradDepthwiseKernels;
            this.gradPointwiseKernels = gradPointwiseKernels;
            this.gradBiases = gradBiases;
        }
    }
}
