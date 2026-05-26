/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.alg;

/**
 * 逻辑回归算法实现
 * <p>
 * 逻辑回归是一种用于二分类问题的监督学习算法，通过 Sigmoid 函数将线性回归的输出
 * 映射到 (0, 1) 区间，表示样本属于正类的概率。
 * </p>
 *
 * <p>数学模型: p = sigmoid(w·x + b) = 1 / (1 + e^(-(w·x + b)))</p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class LogisticRegression {

    /**
     * 权重系数
     */
    private double[] weights;

    /**
     * 偏置项
     */
    private double bias;

    /**
     * 学习率
     */
    private double learningRate;

    /**
     * 迭代次数
     */
    private int iterations;

    /**
     * 正则化系数
     */
    private double lambda;

    /**
     * 默认构造函数
     */
    public LogisticRegression() {
        this(0.01, 1000, 0.0);
    }

    /**
     * 构造函数
     *
     * @param learningRate 学习率
     * @param iterations   迭代次数
     */
    public LogisticRegression(double learningRate, int iterations) {
        this(learningRate, iterations, 0.0);
    }

    /**
     * 构造函数（带L2正则化）
     *
     * @param learningRate 学习率
     * @param iterations   迭代次数
     * @param lambda       L2正则化系数
     */
    public LogisticRegression(double learningRate, int iterations, double lambda) {
        this.learningRate = learningRate;
        this.iterations = iterations;
        this.lambda = lambda;
    }

    /**
     * Sigmoid 函数
     * <p>
     * sigmoid(z) = 1 / (1 + e^(-z))
     * </p>
     *
     * @param z 输入值
     * @return sigmoid 输出值，范围 (0, 1)
     */
    public static double sigmoid(double z) {
        // 防止数值溢出
        if (z < -500) return 0.0;
        if (z > 500) return 1.0;
        return 1.0 / (1.0 + Math.exp(-z));
    }

    /**
     * 训练模型
     *
     * @param X 特征矩阵，每行是一个样本
     * @param y 标签向量（0或1）
     */
    public void fit(double[][] X, double[] y) {
        int nSamples = X.length;
        int nFeatures = X[0].length;

        // 初始化权重和偏置
        weights = new double[nFeatures];
        bias = 0.0;

        // 梯度下降训练
        for (int iter = 0; iter < iterations; iter++) {
            double[] weightGradients = new double[nFeatures];
            double biasGradient = 0.0;

            // 计算梯度
            for (int i = 0; i < nSamples; i++) {
                double prediction = predictProbabilitySingle(X[i]);
                double error = prediction - y[i];

                for (int j = 0; j < nFeatures; j++) {
                    weightGradients[j] += error * X[i][j];
                }
                biasGradient += error;
            }

            // 更新权重（带L2正则化）
            for (int j = 0; j < nFeatures; j++) {
                double regularization = lambda * weights[j] / nSamples;
                weights[j] -= learningRate * (weightGradients[j] / nSamples + regularization);
            }
            bias -= learningRate * biasGradient / nSamples;
        }
    }

    /**
     * 预测单个样本的概率
     *
     * @param x 特征向量
     * @return 属于正类的概率
     */
    private double predictProbabilitySingle(double[] x) {
        double z = bias;
        for (int i = 0; i < weights.length; i++) {
            z += weights[i] * x[i];
        }
        return sigmoid(z);
    }

    /**
     * 预测概率
     *
     * @param X 特征矩阵
     * @return 概率数组
     */
    public double[] predictProbability(double[][] X) {
        double[] probabilities = new double[X.length];
        for (int i = 0; i < X.length; i++) {
            probabilities[i] = predictProbabilitySingle(X[i]);
        }
        return probabilities;
    }

    /**
     * 预测类别
     *
     * @param X 特征矩阵
     * @return 预测类别（0或1）
     */
    public int[] predict(double[][] X) {
        double[] probabilities = predictProbability(X);
        int[] predictions = new int[X.length];
        for (int i = 0; i < probabilities.length; i++) {
            predictions[i] = probabilities[i] >= 0.5 ? 1 : 0;
        }
        return predictions;
    }

    /**
     * 计算准确率
     *
     * @param X 特征矩阵
     * @param y 真实标签
     * @return 准确率 (0.0 - 1.0)
     */
    public double score(double[][] X, double[] y) {
        int[] predictions = predict(X);
        int correct = 0;
        for (int i = 0; i < y.length; i++) {
            if (predictions[i] == y[i]) {
                correct++;
            }
        }
        return (double) correct / y.length;
    }

    /**
     * 计算对数损失（Log Loss / Cross Entropy Loss）
     *
     * @param X 特征矩阵
     * @param y 真实标签
     * @return 对数损失值
     */
    public double logLoss(double[][] X, double[] y) {
        double[] probabilities = predictProbability(X);
        double loss = 0.0;
        int n = y.length;

        for (int i = 0; i < n; i++) {
            // 防止 log(0) 的情况
            double p = Math.max(probabilities[i], 1e-15);
            double pNeg = Math.max(1 - probabilities[i], 1e-15);
            loss -= (y[i] * Math.log(p) + (1 - y[i]) * Math.log(pNeg));
        }

        // 添加L2正则化项
        double regularization = 0.0;
        if (lambda > 0) {
            for (double w : weights) {
                regularization += w * w;
            }
            regularization = lambda * regularization / (2 * n);
        }

        return loss / n + regularization;
    }

    /**
     * 获取权重
     *
     * @return 权重数组
     */
    public double[] getWeights() {
        return weights.clone();
    }

    /**
     * 获取偏置
     *
     * @return 偏置值
     */
    public double getBias() {
        return bias;
    }

    /**
     * 多分类逻辑回归（Softmax回归）
     * <p>
     * 用于处理多分类问题
     * </p>
     */
    public static class Multinomial {

        /**
         * 每个类别的权重矩阵
         */
        private double[][] weights;

        /**
         * 每个类别的偏置
         */
        private double[] biases;

        /**
         * 学习率
         */
        private double learningRate;

        /**
         * 迭代次数
         */
        private int iterations;

        /**
         * 类别数量
         */
        private int nClasses;

        /**
         * 构造函数
         *
         * @param nClasses     类别数量
         * @param learningRate 学习率
         * @param iterations   迭代次数
         */
        public Multinomial(int nClasses, double learningRate, int iterations) {
            this.nClasses = nClasses;
            this.learningRate = learningRate;
            this.iterations = iterations;
        }

        /**
         * Softmax 函数
         *
         * @param z 输入向量
         * @return 概率分布
         */
        private double[] softmax(double[] z) {
            double[] exp = new double[z.length];
            double sum = 0.0;
            double max = Double.NEGATIVE_INFINITY;

            // 数值稳定性处理
            for (double v : z) {
                if (v > max) max = v;
            }

            for (int i = 0; i < z.length; i++) {
                exp[i] = Math.exp(z[i] - max);
                sum += exp[i];
            }

            for (int i = 0; i < z.length; i++) {
                exp[i] /= sum;
            }
            return exp;
        }

        /**
         * 训练模型
         *
         * @param X 特征矩阵
         * @param y 标签向量（类别索引）
         */
        public void fit(double[][] X, int[] y) {
            int nSamples = X.length;
            int nFeatures = X[0].length;

            // 初始化权重和偏置
            weights = new double[nClasses][nFeatures];
            biases = new double[nClasses];

            // 梯度下降训练
            for (int iter = 0; iter < iterations; iter++) {
                double[][] weightGradients = new double[nClasses][nFeatures];
                double[] biasGradients = new double[nClasses];

                // 计算梯度
                for (int i = 0; i < nSamples; i++) {
                    double[] z = new double[nClasses];
                    for (int k = 0; k < nClasses; k++) {
                        for (int j = 0; j < nFeatures; j++) {
                            z[k] += weights[k][j] * X[i][j];
                        }
                        z[k] += biases[k];
                    }

                    double[] probs = softmax(z);

                    for (int k = 0; k < nClasses; k++) {
                        double error = probs[k] - (y[i] == k ? 1 : 0);
                        for (int j = 0; j < nFeatures; j++) {
                            weightGradients[k][j] += error * X[i][j];
                        }
                        biasGradients[k] += error;
                    }
                }

                // 更新权重和偏置
                for (int k = 0; k < nClasses; k++) {
                    for (int j = 0; j < nFeatures; j++) {
                        weights[k][j] -= learningRate * weightGradients[k][j] / nSamples;
                    }
                    biases[k] -= learningRate * biasGradients[k] / nSamples;
                }
            }
        }

        /**
         * 预测概率
         *
         * @param X 特征矩阵
         * @return 每个样本属于每个类别的概率
         */
        public double[][] predictProbability(double[][] X) {
            double[][] probabilities = new double[X.length][nClasses];
            for (int i = 0; i < X.length; i++) {
                double[] z = new double[nClasses];
                for (int k = 0; k < nClasses; k++) {
                    for (int j = 0; j < weights[k].length; j++) {
                        z[k] += weights[k][j] * X[i][j];
                    }
                    z[k] += biases[k];
                }
                probabilities[i] = softmax(z);
            }
            return probabilities;
        }

        /**
         * 预测类别
         *
         * @param X 特征矩阵
         * @return 预测的类别索引
         */
        public int[] predict(double[][] X) {
            double[][] probs = predictProbability(X);
            int[] predictions = new int[X.length];
            for (int i = 0; i < X.length; i++) {
                int maxIdx = 0;
                for (int j = 1; j < nClasses; j++) {
                    if (probs[i][j] > probs[i][maxIdx]) {
                        maxIdx = j;
                    }
                }
                predictions[i] = maxIdx;
            }
            return predictions;
        }
    }
}
