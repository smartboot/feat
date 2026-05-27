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
 * 图像预处理工具类
 * <p>
 * 提供图像识别任务中常用的预处理操作，包括：
 * - 图像缩放和裁剪
 * - 归一化
 * - 数据增强（旋转、翻转等）
 * - 颜色空间转换
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ImagePreprocessing {

    /**
     * 归一化方法枚举
     */
    public enum Normalization {
        MIN_MAX,      // 归一化到[0, 1]
        STANDARD,     // 标准化（均值为0，方差为1）
        IMAGENET,     // ImageNet预训练模型的归一化
        NONE          // 不归一化
    }

    /**
     * 插值方法枚举
     */
    public enum Interpolation {
        NEAREST,      // 最近邻插值
        BILINEAR,     // 双线性插值
        BICUBIC       // 双三次插值
    }

    /**
     * 将图像缩放到指定尺寸
     *
     * @param image          输入图像 [channels][height][width]
     * @param targetHeight   目标高度
     * @param targetWidth    目标宽度
     * @param interpolation  插值方法
     * @return 缩放后的图像
     */
    public static double[][][] resize(double[][][] image, int targetHeight, int targetWidth,
                                       Interpolation interpolation) {
        int channels = image.length;
        int srcHeight = image[0].length;
        int srcWidth = image[0][0].length;

        double[][][] output = new double[channels][targetHeight][targetWidth];

        double scaleY = (double) srcHeight / targetHeight;
        double scaleX = (double) srcWidth / targetWidth;

        for (int c = 0; c < channels; c++) {
            for (int y = 0; y < targetHeight; y++) {
                for (int x = 0; x < targetWidth; x++) {
                    double srcY = y * scaleY;
                    double srcX = x * scaleX;

                    switch (interpolation) {
                        case NEAREST:
                            output[c][y][x] = nearestNeighbor(image[c], srcY, srcX, srcHeight, srcWidth);
                            break;
                        case BILINEAR:
                            output[c][y][x] = bilinear(image[c], srcY, srcX, srcHeight, srcWidth);
                            break;
                        case BICUBIC:
                            output[c][y][x] = bicubic(image[c], srcY, srcX, srcHeight, srcWidth);
                            break;
                    }
                }
            }
        }

        return output;
    }

    /**
     * 中心裁剪
     *
     * @param image          输入图像
     * @param cropHeight     裁剪高度
     * @param cropWidth      裁剪宽度
     * @return 裁剪后的图像
     */
    public static double[][][] centerCrop(double[][][] image, int cropHeight, int cropWidth) {
        int channels = image.length;
        int height = image[0].length;
        int width = image[0][0].length;

        int startY = (height - cropHeight) / 2;
        int startX = (width - cropWidth) / 2;

        return crop(image, startY, startX, cropHeight, cropWidth);
    }

    /**
     * 随机裁剪
     *
     * @param image       输入图像
     * @param cropHeight  裁剪高度
     * @param cropWidth   裁剪宽度
     * @return 裁剪后的图像
     */
    public static double[][][] randomCrop(double[][][] image, int cropHeight, int cropWidth) {
        int height = image[0].length;
        int width = image[0][0].length;

        int startY = (int) (Math.random() * (height - cropHeight + 1));
        int startX = (int) (Math.random() * (width - cropWidth + 1));

        return crop(image, startY, startX, cropHeight, cropWidth);
    }

    /**
     * 裁剪图像
     *
     * @param image     输入图像
     * @param startY    起始Y坐标
     * @param startX    起始X坐标
     * @param cropHeight 裁剪高度
     * @param cropWidth  裁剪宽度
     * @return 裁剪后的图像
     */
    public static double[][][] crop(double[][][] image, int startY, int startX,
                                     int cropHeight, int cropWidth) {
        int channels = image.length;
        double[][][] output = new double[channels][cropHeight][cropWidth];

        for (int c = 0; c < channels; c++) {
            for (int y = 0; y < cropHeight; y++) {
                for (int x = 0; x < cropWidth; x++) {
                    output[c][y][x] = image[c][startY + y][startX + x];
                }
            }
        }

        return output;
    }

    /**
     * 水平翻转
     *
     * @param image 输入图像
     * @return 水平翻转后的图像
     */
    public static double[][][] horizontalFlip(double[][][] image) {
        int channels = image.length;
        int height = image[0].length;
        int width = image[0][0].length;

        double[][][] output = new double[channels][height][width];

        for (int c = 0; c < channels; c++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    output[c][y][x] = image[c][y][width - 1 - x];
                }
            }
        }

        return output;
    }

    /**
     * 垂直翻转
     *
     * @param image 输入图像
     * @return 垂直翻转后的图像
     */
    public static double[][][] verticalFlip(double[][][] image) {
        int channels = image.length;
        int height = image[0].length;
        int width = image[0][0].length;

        double[][][] output = new double[channels][height][width];

        for (int c = 0; c < channels; c++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    output[c][y][x] = image[c][height - 1 - y][x];
                }
            }
        }

        return output;
    }

    /**
     * 随机水平翻转
     *
     * @param image     输入图像
     * @param probability 翻转概率
     * @return 可能翻转后的图像
     */
    public static double[][][] randomHorizontalFlip(double[][][] image, double probability) {
        if (Math.random() < probability) {
            return horizontalFlip(image);
        }
        return image;
    }

    /**
     * 旋转图像（90度的整数倍）
     *
     * @param image  输入图像
     * @param times  旋转次数（90度为单位，正数为顺时针）
     * @return 旋转后的图像
     */
    public static double[][][] rotate90(double[][][] image, int times) {
        times = ((times % 4) + 4) % 4; // 规范化到0-3

        if (times == 0) {
            return image;
        }

        int channels = image.length;
        int height = image[0].length;
        int width = image[0][0].length;

        double[][][] output;

        if (times == 2) {
            output = new double[channels][height][width];
        } else {
            output = new double[channels][width][height];
        }

        for (int c = 0; c < channels; c++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int newY, newX;
                    switch (times) {
                        case 1: // 90度顺时针
                            newY = x;
                            newX = height - 1 - y;
                            break;
                        case 2: // 180度
                            newY = height - 1 - y;
                            newX = width - 1 - x;
                            break;
                        case 3: // 270度顺时针
                            newY = width - 1 - x;
                            newX = y;
                            break;
                        default:
                            newY = y;
                            newX = x;
                    }
                    output[c][newY][newX] = image[c][y][x];
                }
            }
        }

        return output;
    }

    /**
     * 归一化图像
     *
     * @param image          输入图像
     * @param normalization  归一化方法
     * @return 归一化后的图像
     */
    public static double[][][] normalize(double[][][] image, Normalization normalization) {
        int channels = image.length;
        int height = image[0].length;
        int width = image[0][0].length;

        double[][][] output = new double[channels][height][width];

        switch (normalization) {
            case MIN_MAX:
                // 归一化到[0, 1]
                for (int c = 0; c < channels; c++) {
                    double min = Double.POSITIVE_INFINITY;
                    double max = Double.NEGATIVE_INFINITY;

                    // 找到最小值和最大值
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            min = Math.min(min, image[c][y][x]);
                            max = Math.max(max, image[c][y][x]);
                        }
                    }

                    // 归一化
                    double range = max - min;
                    if (range > 0) {
                        for (int y = 0; y < height; y++) {
                            for (int x = 0; x < width; x++) {
                                output[c][y][x] = (image[c][y][x] - min) / range;
                            }
                        }
                    }
                }
                break;

            case STANDARD:
                // 标准化：均值为0，方差为1
                for (int c = 0; c < channels; c++) {
                    double sum = 0;
                    double sumSq = 0;
                    int count = height * width;

                    // 计算均值
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            sum += image[c][y][x];
                        }
                    }
                    double mean = sum / count;

                    // 计算方差
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            double diff = image[c][y][x] - mean;
                            sumSq += diff * diff;
                        }
                    }
                    double std = Math.sqrt(sumSq / count);

                    // 标准化
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            output[c][y][x] = (image[c][y][x] - mean) / (std + 1e-8);
                        }
                    }
                }
                break;

            case IMAGENET:
                // ImageNet预训练模型的归一化
                // 均值: [0.485, 0.456, 0.406]
                // 标准差: [0.229, 0.224, 0.225]
                double[] mean = {0.485, 0.456, 0.406};
                double[] std = {0.229, 0.224, 0.225};

                for (int c = 0; c < channels && c < 3; c++) {
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            output[c][y][x] = (image[c][y][x] / 255.0 - mean[c]) / std[c];
                        }
                    }
                }
                break;

            case NONE:
                // 复制原图
                for (int c = 0; c < channels; c++) {
                    for (int y = 0; y < height; y++) {
                        System.arraycopy(image[c][y], 0, output[c][y], 0, width);
                    }
                }
                break;
        }

        return output;
    }

    /**
     * 调整亮度
     *
     * @param image      输入图像
     * @param factor     亮度因子（1.0为原图，>1变亮，<1变暗）
     * @return 调整后的图像
     */
    public static double[][][] adjustBrightness(double[][][] image, double factor) {
        int channels = image.length;
        int height = image[0].length;
        int width = image[0][0].length;

        double[][][] output = new double[channels][height][width];

        for (int c = 0; c < channels; c++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    output[c][y][x] = image[c][y][x] * factor;
                }
            }
        }

        return output;
    }

    /**
     * 添加高斯噪声
     *
     * @param image  输入图像
     * @param std    噪声标准差
     * @return 添加噪声后的图像
     */
    public static double[][][] addGaussianNoise(double[][][] image, double std) {
        int channels = image.length;
        int height = image[0].length;
        int width = image[0][0].length;

        double[][][] output = new double[channels][height][width];

        for (int c = 0; c < channels; c++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    output[c][y][x] = image[c][y][x] + randomNormal() * std;
                }
            }
        }

        return output;
    }

    /**
     * 颜色抖动（随机调整亮度、对比度、饱和度）
     *
     * @param image           输入图像（RGB）
     * @param brightnessRange 亮度调整范围
     * @param contrastRange   对比度调整范围
     * @return 调整后的图像
     */
    public static double[][][] colorJitter(double[][][] image,
                                            double brightnessRange,
                                            double contrastRange) {
        // 随机亮度调整
        double brightnessFactor = 1.0 + (Math.random() * 2 - 1) * brightnessRange;
        double[][][] result = adjustBrightness(image, brightnessFactor);

        // 随机对比度调整
        double contrastFactor = 1.0 + (Math.random() * 2 - 1) * contrastRange;
        result = adjustContrast(result, contrastFactor);

        return result;
    }

    /**
     * 调整对比度
     */
    private static double[][][] adjustContrast(double[][][] image, double factor) {
        int channels = image.length;
        int height = image[0].length;
        int width = image[0][0].length;

        double[][][] output = new double[channels][height][width];

        for (int c = 0; c < channels; c++) {
            // 计算通道均值
            double mean = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    mean += image[c][y][x];
                }
            }
            mean /= (height * width);

            // 调整对比度
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    output[c][y][x] = (image[c][y][x] - mean) * factor + mean;
                }
            }
        }

        return output;
    }

    // ============ 私有辅助方法 ============

    /**
     * 最近邻插值
     */
    private static double nearestNeighbor(double[][] image, double y, double x,
                                          int height, int width) {
        int py = Math.min((int) Math.round(y), height - 1);
        int px = Math.min((int) Math.round(x), width - 1);
        return image[py][px];
    }

    /**
     * 双线性插值
     */
    private static double bilinear(double[][] image, double y, double x,
                                   int height, int width) {
        int y0 = (int) Math.floor(y);
        int x0 = (int) Math.floor(x);
        int y1 = Math.min(y0 + 1, height - 1);
        int x1 = Math.min(x0 + 1, width - 1);

        double dy = y - y0;
        double dx = x - x0;

        double v00 = image[y0][x0];
        double v01 = image[y0][x1];
        double v10 = image[y1][x0];
        double v11 = image[y1][x1];

        return v00 * (1 - dy) * (1 - dx) +
               v01 * (1 - dy) * dx +
               v10 * dy * (1 - dx) +
               v11 * dy * dx;
    }

    /**
     * 双三次插值（简化版）
     */
    private static double bicubic(double[][] image, double y, double x,
                                  int height, int width) {
        // 简化为双线性插值
        return bilinear(image, y, x, height, width);
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
