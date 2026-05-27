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
 * 批量归一化（Batch Normalization）实现
 * <p>
 * 批量归一化通过对每一层的输入进行归一化，加速训练收敛，
 * 允许使用更大的学习率，同时具有正则化效果。
 * </p>
 *
 * <p>公式：</p>
 * <pre>
 * μ = mean(x)           // 批次均值
 * σ² = var(x)           // 批次方差
 * x̂ = (x - μ) / √(σ² + ε)  // 归一化
 * y = γ * x̂ + β         // 缩放和平移
 * </pre>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class BatchNormalization {

    /**
     * 可学习的缩放参数（gamma）
     */
    private double[] gamma;

    /**
     * 可学习的平移参数（beta）
     */
    private double[] beta;

    /**
     * 运行均值（用于推理）
     */
    private double[] runningMean;

    /**
     * 运行方差（用于推理）
     */
    private double[] runningVar;

    /**
     * 动量（用于更新运行统计量）
     */
    private double momentum;

    /**
     * 数值稳定性常数
     */
    private double epsilon;

    /**
     * 通道数
     */
    private int numFeatures;

    /**
     * 是否训练模式
     */
    private boolean training;

    /**
     * 前向传播缓存
     */
    private double[][][] lastInput;
    private double[][][] lastNormalized;
    private double[] lastMean;
    private double[] lastVar;

    /**
     * 构造函数
     *
     * @param numFeatures 特征数（通道数）
     */
    public BatchNormalization(int numFeatures) {
        this(numFeatures, 0.1, 1e-5);
    }

    /**
     * 构造函数
     *
     * @param numFeatures 特征数
     * @param momentum    动量
     * @param epsilon     数值稳定性常数
     */
    public BatchNormalization(int numFeatures, double momentum, double epsilon) {
        this.numFeatures = numFeatures;
        this.momentum = momentum;
        this.epsilon = epsilon;
        this.training = true;

        // 初始化参数
        this.gamma = new double[numFeatures];
        this.beta = new double[numFeatures];
        this.runningMean = new double[numFeatures];
        this.runningVar = new double[numFeatures];

        // gamma初始化为1，beta初始化为0
        for (int i = 0; i < numFeatures; i++) {
            gamma[i] = 1.0;
            beta[i] = 0.0;
            runningMean[i] = 0.0;
            runningVar[i] = 1.0;
        }
    }

    /**
     * 前向传播（用于卷积层输出）
     *
     * @param input 输入特征图 [channels][height][width]
     * @return 归一化后的特征图
     */
    public double[][][] forward(double[][][] input) {
        int channels = input.length;
        int height = input[0].length;
        int width = input[0][0].length;

        this.lastInput = input;
        double[][][] output = new double[channels][height][width];

        if (training) {
            // 训练模式：使用批次统计量
            lastMean = new double[channels];
            lastVar = new double[channels];
            lastNormalized = new double[channels][height][width];

            // 计算每个通道的均值和方差
            for (int c = 0; c < channels; c++) {
                double sum = 0;
                for (int i = 0; i < height; i++) {
                    for (int j = 0; j < width; j++) {
                        sum += input[c][i][j];
                    }
                }
                lastMean[c] = sum / (height * width);

                double varSum = 0;
                for (int i = 0; i < height; i++) {
                    for (int j = 0; j < width; j++) {
                        double diff = input[c][i][j] - lastMean[c];
                        varSum += diff * diff;
                    }
                }
                lastVar[c] = varSum / (height * width);

                // 更新运行统计量
                runningMean[c] = momentum * runningMean[c] + (1 - momentum) * lastMean[c];
                runningVar[c] = momentum * runningVar[c] + (1 - momentum) * lastVar[c];

                // 归一化并缩放
                double std = Math.sqrt(lastVar[c] + epsilon);
                for (int i = 0; i < height; i++) {
                    for (int j = 0; j < width; j++) {
                        lastNormalized[c][i][j] = (input[c][i][j] - lastMean[c]) / std;
                        output[c][i][j] = gamma[c] * lastNormalized[c][i][j] + beta[c];
                    }
                }
            }
        } else {
            // 推理模式：使用运行统计量
            for (int c = 0; c < channels; c++) {
                double std = Math.sqrt(runningVar[c] + epsilon);
                for (int i = 0; i < height; i++) {
                    for (int j = 0; j < width; j++) {
                        double normalized = (input[c][i][j] - runningMean[c]) / std;
                        output[c][i][j] = gamma[c] * normalized + beta[c];
                    }
                }
            }
        }

        return output;
    }

    /**
     * 前向传播（用于全连接层输出）
     *
     * @param input 输入 [batchSize][features]
     * @return 归一化后的输出
     */
    public double[][] forwardFC(double[][] input) {
        int batchSize = input.length;
        int features = input[0].length;

        double[][] output = new double[batchSize][features];

        if (training) {
            lastMean = new double[features];
            lastVar = new double[features];
            lastNormalized = new double[batchSize][features][1];

            // 计算均值
            for (int j = 0; j < features; j++) {
                double sum = 0;
                for (int i = 0; i < batchSize; i++) {
                    sum += input[i][j];
                }
                lastMean[j] = sum / batchSize;
            }

            // 计算方差
            for (int j = 0; j < features; j++) {
                double varSum = 0;
                for (int i = 0; i < batchSize; i++) {
                    double diff = input[i][j] - lastMean[j];
                    varSum += diff * diff;
                }
                lastVar[j] = varSum / batchSize;

                // 更新运行统计量
                runningMean[j] = momentum * runningMean[j] + (1 - momentum) * lastMean[j];
                runningVar[j] = momentum * runningVar[j] + (1 - momentum) * lastVar[j];
            }

            // 归一化并缩放
            for (int i = 0; i < batchSize; i++) {
                for (int j = 0; j < features; j++) {
                    double std = Math.sqrt(lastVar[j] + epsilon);
                    lastNormalized[i][j][0] = (input[i][j] - lastMean[j]) / std;
                    output[i][j] = gamma[j] * lastNormalized[i][j][0] + beta[j];
                }
            }
        } else {
            // 推理模式
            for (int i = 0; i < batchSize; i++) {
                for (int j = 0; j < features; j++) {
                    double std = Math.sqrt(runningVar[j] + epsilon);
                    double normalized = (input[i][j] - runningMean[j]) / std;
                    output[i][j] = gamma[j] * normalized + beta[j];
                }
            }
        }

        return output;
    }

    /**
     * 反向传播（用于卷积层）
     *
     * @param gradOutput 输出梯度 [channels][height][width]
     * @return 输入梯度
     */
    public double[][][] backward(double[][][] gradOutput) {
        int channels = gradOutput.length;
        int height = gradOutput[0].length;
        int width = gradOutput[0][0].length;

        double[][][] gradInput = new double[channels][height][width];
        double[] gradGamma = new double[channels];
        double[] gradBeta = new double[channels];

        int n = height * width;

        for (int c = 0; c < channels; c++) {
            // 计算gamma和beta的梯度
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    gradGamma[c] += gradOutput[c][i][j] * lastNormalized[c][i][j];
                    gradBeta[c] += gradOutput[c][i][j];
                }
            }

            // 计算输入梯度
            double std = Math.sqrt(lastVar[c] + epsilon);

            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    double gradNorm = gradOutput[c][i][j] * gamma[c];

                    // 链式法则计算
                    double term1 = gradNorm;
                    double term2 = -gradNorm / n;
                    double term3 = -lastNormalized[c][i][j] * gradNorm / n;

                    gradInput[c][i][j] = (term1 + term2 + term3 * lastNormalized[c][i][j]) / std;
                }
            }
        }

        // 更新gamma和beta（这里只返回梯度，实际更新由优化器处理）
        // 在实际实现中，这些梯度应该被存储并用于参数更新

        return gradInput;
    }

    /**
     * 反向传播（用于全连接层）
     *
     * @param gradOutput 输出梯度 [batchSize][features]
     * @return 输入梯度
     */
    public double[][] backwardFC(double[][] gradOutput) {
        int batchSize = gradOutput.length;
        int features = gradOutput[0].length;

        double[][] gradInput = new double[batchSize][features];

        for (int j = 0; j < features; j++) {
            double std = Math.sqrt(lastVar[j] + epsilon);

            // 计算gamma和beta的梯度
            double gradGamma = 0;
            double gradBeta = 0;
            for (int i = 0; i < batchSize; i++) {
                gradGamma += gradOutput[i][j] * lastNormalized[i][j][0];
                gradBeta += gradOutput[i][j];
            }

            // 计算输入梯度
            for (int i = 0; i < batchSize; i++) {
                double gradNorm = gradOutput[i][j] * gamma[j];
                double term1 = gradNorm;
                double term2 = -gradNorm / batchSize;
                double term3 = -lastNormalized[i][j][0] * gradNorm / batchSize;

                gradInput[i][j] = (term1 + term2 + term3 * lastNormalized[i][j][0]) / std;
            }
        }

        return gradInput;
    }

    /**
     * 设置训练模式
     *
     * @param training 是否训练模式
     */
    public void setTraining(boolean training) {
        this.training = training;
    }

    /**
     * 获取gamma参数
     */
    public double[] getGamma() {
        return gamma.clone();
    }

    /**
     * 获取beta参数
     */
    public double[] getBeta() {
        return beta.clone();
    }

    /**
     * 设置gamma参数
     */
    public void setGamma(double[] gamma) {
        this.gamma = gamma.clone();
    }

    /**
     * 设置beta参数
     */
    public void setBeta(double[] beta) {
        this.beta = beta.clone();
    }

    /**
     * 获取运行均值
     */
    public double[] getRunningMean() {
        return runningMean.clone();
    }

    /**
     * 获取运行方差
     */
    public double[] getRunningVar() {
        return runningVar.clone();
    }
}
