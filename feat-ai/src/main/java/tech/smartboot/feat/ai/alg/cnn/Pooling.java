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
 * 池化层实现
 * <p>
 * 池化操作用于降低特征图的空间维度，减少计算量，同时提供平移不变性。
 * 支持最大池化和平均池化。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class Pooling {

    /**
     * 最大池化
     *
     * @param input       输入特征图 [channels][height][width]
     * @param poolHeight  池化窗口高度
     * @param poolWidth   池化窗口宽度
     * @param stride      步幅
     * @return 池化后的特征图和最大值位置索引
     */
    public static PoolingResult maxPool(double[][][] input, int poolHeight, int poolWidth, int stride) {
        int channels = input.length;
        int inputHeight = input[0].length;
        int inputWidth = input[0][0].length;

        int outputHeight = (inputHeight - poolHeight) / stride + 1;
        int outputWidth = (inputWidth - poolWidth) / stride + 1;

        double[][][] output = new double[channels][outputHeight][outputWidth];
        // 存储最大值位置，用于反向传播
        int[][][][] maxIndices = new int[channels][outputHeight][outputWidth][2];

        for (int c = 0; c < channels; c++) {
            for (int i = 0; i < outputHeight; i++) {
                for (int j = 0; j < outputWidth; j++) {
                    int startY = i * stride;
                    int startX = j * stride;

                    double maxVal = Double.NEGATIVE_INFINITY;
                    int maxY = 0, maxX = 0;

                    for (int ph = 0; ph < poolHeight; ph++) {
                        for (int pw = 0; pw < poolWidth; pw++) {
                            int y = startY + ph;
                            int x = startX + pw;
                            if (y < inputHeight && x < inputWidth) {
                                double val = input[c][y][x];
                                if (val > maxVal) {
                                    maxVal = val;
                                    maxY = ph;
                                    maxX = pw;
                                }
                            }
                        }
                    }

                    output[c][i][j] = maxVal;
                    maxIndices[c][i][j][0] = startY + maxY;
                    maxIndices[c][i][j][1] = startX + maxX;
                }
            }
        }

        return new PoolingResult(output, maxIndices);
    }

    /**
     * 平均池化
     *
     * @param input       输入特征图
     * @param poolHeight  池化窗口高度
     * @param poolWidth   池化窗口宽度
     * @param stride      步幅
     * @return 池化后的特征图
     */
    public static double[][][] avgPool(double[][][] input, int poolHeight, int poolWidth, int stride) {
        int channels = input.length;
        int inputHeight = input[0].length;
        int inputWidth = input[0][0].length;

        int outputHeight = (inputHeight - poolHeight) / stride + 1;
        int outputWidth = (inputWidth - poolWidth) / stride + 1;

        double[][][] output = new double[channels][outputHeight][outputWidth];

        for (int c = 0; c < channels; c++) {
            for (int i = 0; i < outputHeight; i++) {
                for (int j = 0; j < outputWidth; j++) {
                    int startY = i * stride;
                    int startX = j * stride;

                    double sum = 0;
                    int count = 0;

                    for (int ph = 0; ph < poolHeight; ph++) {
                        for (int pw = 0; pw < poolWidth; pw++) {
                            int y = startY + ph;
                            int x = startX + pw;
                            if (y < inputHeight && x < inputWidth) {
                                sum += input[c][y][x];
                                count++;
                            }
                        }
                    }

                    output[c][i][j] = sum / count;
                }
            }
        }

        return output;
    }

    /**
     * 全局平均池化
     *
     * @param input 输入特征图 [channels][height][width]
     * @return 输出向量 [channels]
     */
    public static double[] globalAvgPool(double[][][] input) {
        int channels = input.length;
        int height = input[0].length;
        int width = input[0][0].length;

        double[] output = new double[channels];

        for (int c = 0; c < channels; c++) {
            double sum = 0;
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    sum += input[c][i][j];
                }
            }
            output[c] = sum / (height * width);
        }

        return output;
    }

    /**
     * 全局最大池化
     *
     * @param input 输入特征图
     * @return 输出向量 [channels]
     */
    public static double[] globalMaxPool(double[][][] input) {
        int channels = input.length;
        double[] output = new double[channels];

        for (int c = 0; c < channels; c++) {
            double max = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < input[c].length; i++) {
                for (int j = 0; j < input[c][i].length; j++) {
                    if (input[c][i][j] > max) {
                        max = input[c][i][j];
                    }
                }
            }
            output[c] = max;
        }

        return output;
    }

    /**
     * 最大池化反向传播
     *
     * @param gradOutput  输出梯度
     * @param maxIndices  最大值位置索引
     * @param inputHeight 输入高度
     * @param inputWidth  输入宽度
     * @return 输入梯度
     */
    public static double[][][] maxPoolBackward(double[][][] gradOutput, int[][][][] maxIndices,
                                                int inputHeight, int inputWidth) {
        int channels = gradOutput.length;
        int outputHeight = gradOutput[0].length;
        int outputWidth = gradOutput[0][0].length;

        double[][][] gradInput = new double[channels][inputHeight][inputWidth];

        for (int c = 0; c < channels; c++) {
            for (int i = 0; i < outputHeight; i++) {
                for (int j = 0; j < outputWidth; j++) {
                    int maxY = maxIndices[c][i][j][0];
                    int maxX = maxIndices[c][i][j][1];
                    gradInput[c][maxY][maxX] = gradOutput[c][i][j];
                }
            }
        }

        return gradInput;
    }

    /**
     * 平均池化反向传播
     *
     * @param gradOutput  输出梯度
     * @param poolHeight  池化窗口高度
     * @param poolWidth   池化窗口宽度
     * @param stride      步幅
     * @param inputHeight 输入高度
     * @param inputWidth  输入宽度
     * @return 输入梯度
     */
    public static double[][][] avgPoolBackward(double[][][] gradOutput, int poolHeight, int poolWidth,
                                                int stride, int inputHeight, int inputWidth) {
        int channels = gradOutput.length;
        int outputHeight = gradOutput[0].length;
        int outputWidth = gradOutput[0][0].length;

        double[][][] gradInput = new double[channels][inputHeight][inputWidth];

        for (int c = 0; c < channels; c++) {
            for (int i = 0; i < outputHeight; i++) {
                for (int j = 0; j < outputWidth; j++) {
                    int startY = i * stride;
                    int startX = j * stride;

                    // 计算实际覆盖的像素数
                    int count = 0;
                    for (int ph = 0; ph < poolHeight; ph++) {
                        for (int pw = 0; pw < poolWidth; pw++) {
                            int y = startY + ph;
                            int x = startX + pw;
                            if (y < inputHeight && x < inputWidth) {
                                count++;
                            }
                        }
                    }

                    // 将梯度均匀分配给覆盖的像素
                    double grad = gradOutput[c][i][j] / count;
                    for (int ph = 0; ph < poolHeight; ph++) {
                        for (int pw = 0; pw < poolWidth; pw++) {
                            int y = startY + ph;
                            int x = startX + pw;
                            if (y < inputHeight && x < inputWidth) {
                                gradInput[c][y][x] += grad;
                            }
                        }
                    }
                }
            }
        }

        return gradInput;
    }

    /**
     * 自适应平均池化
     * 将任意尺寸的特征图池化到指定输出尺寸
     *
     * @param input         输入特征图
     * @param outputHeight  目标输出高度
     * @param outputWidth   目标输出宽度
     * @return 池化后的特征图
     */
    public static double[][][] adaptiveAvgPool(double[][][] input, int outputHeight, int outputWidth) {
        int channels = input.length;
        int inputHeight = input[0].length;
        int inputWidth = input[0][0].length;

        double[][][] output = new double[channels][outputHeight][outputWidth];

        // 计算每个输出单元对应的输入区域
        for (int c = 0; c < channels; c++) {
            for (int i = 0; i < outputHeight; i++) {
                int startY = i * inputHeight / outputHeight;
                int endY = (i + 1) * inputHeight / outputHeight;

                for (int j = 0; j < outputWidth; j++) {
                    int startX = j * inputWidth / outputWidth;
                    int endX = (j + 1) * inputWidth / outputWidth;

                    double sum = 0;
                    int count = 0;

                    for (int y = startY; y < endY && y < inputHeight; y++) {
                        for (int x = startX; x < endX && x < inputWidth; x++) {
                            sum += input[c][y][x];
                            count++;
                        }
                    }

                    output[c][i][j] = count > 0 ? sum / count : 0;
                }
            }
        }

        return output;
    }

    /**
     * 池化结果封装类
     */
    public static class PoolingResult {
        public final double[][][] output;
        public final int[][][][] maxIndices;

        public PoolingResult(double[][][] output, int[][][][] maxIndices) {
            this.output = output;
            this.maxIndices = maxIndices;
        }
    }
}
