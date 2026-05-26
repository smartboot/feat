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
 * 线性回归算法实现
 * <p>
 * 线性回归是一种用于预测连续值的监督学习算法，通过拟合一条直线（或超平面）
 * 来建立输入特征与目标变量之间的线性关系。
 * </p>
 *
 * <p>数学模型: y = w₁x₁ + w₂x₂ + ... + wₙxₙ + b</p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class LinearRegression {

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
     * 默认构造函数
     */
    public LinearRegression() {
        this(0.01, 1000);
    }

    /**
     * 构造函数
     *
     * @param learningRate 学习率
     * @param iterations   迭代次数
     */
    public LinearRegression(double learningRate, int iterations) {
        this.learningRate = learningRate;
        this.iterations = iterations;
    }

    /**
     * 训练模型
     *
     * @param X 特征矩阵，每行是一个样本，每列是一个特征
     * @param y 目标值向量
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
                double prediction = predictSingle(X[i]);
                double error = prediction - y[i];

                for (int j = 0; j < nFeatures; j++) {
                    weightGradients[j] += error * X[i][j];
                }
                biasGradient += error;
            }

            // 更新权重和偏置
            for (int j = 0; j < nFeatures; j++) {
                weights[j] -= learningRate * weightGradients[j] / nSamples;
            }
            bias -= learningRate * biasGradient / nSamples;
        }
    }

    /**
     * 预测单个样本
     *
     * @param x 特征向量
     * @return 预测值
     */
    private double predictSingle(double[] x) {
        double result = bias;
        for (int i = 0; i < weights.length; i++) {
            result += weights[i] * x[i];
        }
        return result;
    }

    /**
     * 预测
     *
     * @param X 特征矩阵
     * @return 预测值数组
     */
    public double[] predict(double[][] X) {
        double[] predictions = new double[X.length];
        for (int i = 0; i < X.length; i++) {
            predictions[i] = predictSingle(X[i]);
        }
        return predictions;
    }

    /**
     * 计算均方误差 (MSE)
     *
     * @param X 特征矩阵
     * @param y 真实值
     * @return MSE值
     */
    public double score(double[][] X, double[] y) {
        double[] predictions = predict(X);
        double mse = 0.0;
        for (int i = 0; i < y.length; i++) {
            mse += Math.pow(predictions[i] - y[i], 2);
        }
        return mse / y.length;
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
     * 使用正规方程直接求解（适用于小数据集）
     * <p>
     * 公式: w = (X^T * X)^(-1) * X^T * y
     * </p>
     *
     * @param X 特征矩阵
     * @param y 目标值向量
     */
    public void fitNormalEquation(double[][] X, double[] y) {
        int nSamples = X.length;
        int nFeatures = X[0].length;

        // 添加偏置列（全为1）
        double[][] XWithBias = new double[nSamples][nFeatures + 1];
        for (int i = 0; i < nSamples; i++) {
            XWithBias[i][0] = 1.0;
            System.arraycopy(X[i], 0, XWithBias[i], 1, nFeatures);
        }

        // 计算 X^T * X
        double[][] XtX = matrixMultiply(transpose(XWithBias), XWithBias);

        // 计算 X^T * y
        double[] Xty = matrixVectorMultiply(transpose(XWithBias), y);

        // 求解 (X^T * X)^(-1) * X^T * y
        double[][] XtXInv = matrixInverse(XtX);
        double[] result = matrixVectorMultiply(XtXInv, Xty);

        bias = result[0];
        weights = new double[nFeatures];
        System.arraycopy(result, 1, weights, 0, nFeatures);
    }

    /**
     * 矩阵转置
     */
    private double[][] transpose(double[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        double[][] result = new double[cols][rows];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result[j][i] = matrix[i][j];
            }
        }
        return result;
    }

    /**
     * 矩阵乘法
     */
    private double[][] matrixMultiply(double[][] a, double[][] b) {
        int rows = a.length;
        int cols = b[0].length;
        int inner = a[0].length;
        double[][] result = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                for (int k = 0; k < inner; k++) {
                    result[i][j] += a[i][k] * b[k][j];
                }
            }
        }
        return result;
    }

    /**
     * 矩阵与向量乘法
     */
    private double[] matrixVectorMultiply(double[][] matrix, double[] vector) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        double[] result = new double[rows];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result[i] += matrix[i][j] * vector[j];
            }
        }
        return result;
    }

    /**
     * 矩阵求逆（使用高斯-约旦消元法）
     */
    private double[][] matrixInverse(double[][] matrix) {
        int n = matrix.length;
        double[][] augmented = new double[n][2 * n];

        // 创建增广矩阵 [A | I]
        for (int i = 0; i < n; i++) {
            System.arraycopy(matrix[i], 0, augmented[i], 0, n);
            augmented[i][n + i] = 1.0;
        }

        // 高斯-约旦消元
        for (int i = 0; i < n; i++) {
            // 主元归一化
            double pivot = augmented[i][i];
            for (int j = 0; j < 2 * n; j++) {
                augmented[i][j] /= pivot;
            }

            // 消去其他行
            for (int k = 0; k < n; k++) {
                if (k != i) {
                    double factor = augmented[k][i];
                    for (int j = 0; j < 2 * n; j++) {
                        augmented[k][j] -= factor * augmented[i][j];
                    }
                }
            }
        }

        // 提取逆矩阵
        double[][] inverse = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(augmented[i], n, inverse[i], 0, n);
        }
        return inverse;
    }
}
