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
 * 均方误差损失函数 (Mean Squared Error Loss)
 * <p>
 * MSE 是回归问题中最常用的损失函数之一，计算预测值与真实值之间差值的平方的平均值。
 * </p>
 *
 * <p>数学公式:</p>
 * <ul>
 *   <li>损失: L = (1/n) * Σ(y_pred - y_true)²</li>
 *   <li>梯度: dL/dy_pred = (2/n) * (y_pred - y_true)</li>
 * </ul>
 *
 * <p>特性：</p>
 * <ul>
 *   <li>对异常值敏感（因为平方会放大误差）</li>
 *   <li>处处可导，便于梯度下降优化</li>
 *   <li>凸函数，有全局最优解</li>
 * </ul>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class MSELoss {

    /**
     * 是否使用正则化
     */
    private boolean useRegularization;

    /**
     * L2正则化系数
     */
    private double lambda;

    /**
     * 默认构造函数
     */
    public MSELoss() {
        this(false, 0.0);
    }

    /**
     * 构造函数
     *
     * @param useRegularization 是否使用L2正则化
     * @param lambda            正则化系数
     */
    public MSELoss(boolean useRegularization, double lambda) {
        this.useRegularization = useRegularization;
        this.lambda = lambda;
    }

    /**
     * 计算均方误差损失
     *
     * @param predictions 预测值数组
     * @param targets     真实值数组
     * @return MSE损失值
     */
    public double compute(double[] predictions, double[] targets) {
        if (predictions.length != targets.length) {
            throw new IllegalArgumentException("预测值和真实值长度必须相同");
        }

        double sum = 0.0;
        int n = predictions.length;

        for (int i = 0; i < n; i++) {
            double diff = predictions[i] - targets[i];
            sum += diff * diff;
        }

        return sum / n;
    }

    /**
     * 计算均方误差损失（矩阵形式）
     *
     * @param predictions 预测值矩阵 [n_samples, n_outputs]
     * @param targets     真实值矩阵 [n_samples, n_outputs]
     * @return MSE损失值
     */
    public double compute(double[][] predictions, double[][] targets) {
        if (predictions.length != targets.length) {
            throw new IllegalArgumentException("样本数量必须相同");
        }

        double sum = 0.0;
        int nSamples = predictions.length;
        int nOutputs = predictions[0].length;

        for (int i = 0; i < nSamples; i++) {
            for (int j = 0; j < nOutputs; j++) {
                double diff = predictions[i][j] - targets[i][j];
                sum += diff * diff;
            }
        }

        return sum / (nSamples * nOutputs);
    }

    /**
     * 计算损失并添加L2正则化
     *
     * @param predictions 预测值数组
     * @param targets     真实值数组
     * @param weights     模型权重（用于正则化）
     * @return 带正则化的MSE损失
     */
    public double computeWithRegularization(double[] predictions, double[] targets, double[] weights) {
        double mse = compute(predictions, targets);

        if (useRegularization && weights != null) {
            double reg = 0.0;
            for (double w : weights) {
                reg += w * w;
            }
            mse += (lambda / 2.0) * reg / predictions.length;
        }

        return mse;
    }

    /**
     * 计算梯度
     *
     * @param predictions 预测值数组
     * @param targets     真实值数组
     * @return 梯度数组（与predictions同维度）
     */
    public double[] gradient(double[] predictions, double[] targets) {
        if (predictions.length != targets.length) {
            throw new IllegalArgumentException("预测值和真实值长度必须相同");
        }

        int n = predictions.length;
        double[] grad = new double[n];

        for (int i = 0; i < n; i++) {
            grad[i] = 2.0 * (predictions[i] - targets[i]) / n;
        }

        return grad;
    }

    /**
     * 计算梯度（矩阵形式）
     *
     * @param predictions 预测值矩阵
     * @param targets     真实值矩阵
     * @return 梯度矩阵
     */
    public double[][] gradient(double[][] predictions, double[][] targets) {
        if (predictions.length != targets.length) {
            throw new IllegalArgumentException("样本数量必须相同");
        }

        int nSamples = predictions.length;
        int nOutputs = predictions[0].length;
        double[][] grad = new double[nSamples][nOutputs];

        for (int i = 0; i < nSamples; i++) {
            for (int j = 0; j < nOutputs; j++) {
                grad[i][j] = 2.0 * (predictions[i][j] - targets[i][j]) / (nSamples * nOutputs);
            }
        }

        return grad;
    }

    /**
     * 计算带正则化的梯度
     *
     * @param predictions 预测值数组
     * @param targets     真实值数组
     * @param weights     模型权重
     * @param weightGrad  权重梯度输出数组
     * @return 预测梯度
     */
    public double[] gradientWithRegularization(double[] predictions, double[] targets,
                                                double[] weights, double[] weightGrad) {
        double[] grad = gradient(predictions, targets);

        if (useRegularization && weights != null && weightGrad != null) {
            for (int i = 0; i < weights.length; i++) {
                weightGrad[i] = (lambda * weights[i]) / predictions.length;
            }
        }

        return grad;
    }

    /**
     * 计算RMSE（均方根误差）
     *
     * @param predictions 预测值数组
     * @param targets     真实值数组
     * @return RMSE值
     */
    public static double rmse(double[] predictions, double[] targets) {
        if (predictions.length != targets.length) {
            throw new IllegalArgumentException("预测值和真实值长度必须相同");
        }

        double sum = 0.0;
        for (int i = 0; i < predictions.length; i++) {
            double diff = predictions[i] - targets[i];
            sum += diff * diff;
        }

        return Math.sqrt(sum / predictions.length);
    }

    /**
     * 计算MAE（平均绝对误差）
     *
     * @param predictions 预测值数组
     * @param targets     真实值数组
     * @return MAE值
     */
    public static double mae(double[] predictions, double[] targets) {
        if (predictions.length != targets.length) {
            throw new IllegalArgumentException("预测值和真实值长度必须相同");
        }

        double sum = 0.0;
        for (int i = 0; i < predictions.length; i++) {
            sum += Math.abs(predictions[i] - targets[i]);
        }

        return sum / predictions.length;
    }

    /**
     * 计算R²决定系数
     *
     * @param predictions 预测值数组
     * @param targets     真实值数组
     * @return R²值，范围 (-∞, 1]，越接近1表示拟合越好
     */
    public static double r2Score(double[] predictions, double[] targets) {
        if (predictions.length != targets.length) {
            throw new IllegalArgumentException("预测值和真实值长度必须相同");
        }

        double meanTarget = 0.0;
        for (double target : targets) {
            meanTarget += target;
        }
        meanTarget /= targets.length;

        double ssRes = 0.0;  // 残差平方和
        double ssTot = 0.0;  // 总平方和

        for (int i = 0; i < targets.length; i++) {
            double diffRes = targets[i] - predictions[i];
            double diffTot = targets[i] - meanTarget;
            ssRes += diffRes * diffRes;
            ssTot += diffTot * diffTot;
        }

        if (ssTot == 0) {
            return 1.0;  // 所有目标值相同
        }

        return 1.0 - (ssRes / ssTot);
    }

    /**
     * Huber损失（MSE和MAE的结合，对异常值更鲁棒）
     *
     * @param predictions 预测值数组
     * @param targets     真实值数组
     * @param delta       阈值参数
     * @return Huber损失值
     */
    public static double huberLoss(double[] predictions, double[] targets, double delta) {
        if (predictions.length != targets.length) {
            throw new IllegalArgumentException("预测值和真实值长度必须相同");
        }

        double sum = 0.0;
        for (int i = 0; i < predictions.length; i++) {
            double diff = Math.abs(predictions[i] - targets[i]);
            if (diff <= delta) {
                sum += 0.5 * diff * diff;
            } else {
                sum += delta * diff - 0.5 * delta * delta;
            }
        }

        return sum / predictions.length;
    }

    /**
     * Huber损失梯度
     *
     * @param predictions 预测值数组
     * @param targets     真实值数组
     * @param delta       阈值参数
     * @return 梯度数组
     */
    public static double[] huberGradient(double[] predictions, double[] targets, double delta) {
        if (predictions.length != targets.length) {
            throw new IllegalArgumentException("预测值和真实值长度必须相同");
        }

        int n = predictions.length;
        double[] grad = new double[n];

        for (int i = 0; i < n; i++) {
            double diff = predictions[i] - targets[i];
            if (Math.abs(diff) <= delta) {
                grad[i] = diff / n;
            } else {
                grad[i] = delta * Math.signum(diff) / n;
            }
        }

        return grad;
    }

    /**
     * 平滑L1损失（Smooth L1 Loss / Huber损失的变体）
     *
     * @param predictions 预测值数组
     * @param targets     真实值数组
     * @param beta        平滑参数
     * @return Smooth L1损失值
     */
    public static double smoothL1Loss(double[] predictions, double[] targets, double beta) {
        if (predictions.length != targets.length) {
            throw new IllegalArgumentException("预测值和真实值长度必须相同");
        }

        double sum = 0.0;
        for (int i = 0; i < predictions.length; i++) {
            double diff = Math.abs(predictions[i] - targets[i]);
            if (diff < beta) {
                sum += 0.5 * diff * diff / beta;
            } else {
                sum += diff - 0.5 * beta;
            }
        }

        return sum / predictions.length;
    }
}
