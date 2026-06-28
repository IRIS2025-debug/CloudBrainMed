"""
CT 金属伪影检测 — 数据增强
纯 numpy 实现，无额外依赖
"""
import numpy as np
from scipy.ndimage import map_coordinates, gaussian_filter


class Compose:
    """组合多个增强"""
    def __init__(self, transforms):
        self.transforms = transforms

    def __call__(self, image, mask):
        for t in self.transforms:
            image, mask = t(image, mask)
        return {"image": image, "mask": mask}


class RandomHorizontalFlip:
    def __init__(self, prob=0.5):
        self.prob = prob
    def __call__(self, image, mask):
        if np.random.rand() < self.prob:
            image = np.fliplr(image).copy()
            mask = np.fliplr(mask).copy()
        return image, mask


class RandomVerticalFlip:
    def __init__(self, prob=0.3):
        self.prob = prob
    def __call__(self, image, mask):
        if np.random.rand() < self.prob:
            image = np.flipud(image).copy()
            mask = np.flipud(mask).copy()
        return image, mask


class RandomRotation:
    """简易旋转（90° 倍数）"""
    def __init__(self, angles=None):
        self.angles = angles or [0, 1, 2, 3]  # 0°, 90°, 180°, 270°
    def __call__(self, image, mask):
        k = np.random.choice(self.angles)
        if k > 0:
            image = np.rot90(image, k).copy()
            mask = np.rot90(mask, k).copy()
        return image, mask


class ElasticTransform:
    """弹性变形（需要 scipy）"""
    def __init__(self, alpha=20, sigma=3, prob=0.3):
        self.alpha = alpha
        self.sigma = sigma
        self.prob = prob
    def __call__(self, image, mask):
        if np.random.rand() >= self.prob:
            return image, mask
        shape = image.shape
        dx = gaussian_filter((np.random.rand(*shape) * 2 - 1), self.sigma) * self.alpha
        dy = gaussian_filter((np.random.rand(*shape) * 2 - 1), self.sigma) * self.alpha
        x, y = np.meshgrid(np.arange(shape[1]), np.arange(shape[0]))
        indices = (y + dy).reshape(-1), (x + dx).reshape(-1)
        image = map_coordinates(image, indices, order=1, mode="reflect").reshape(shape)
        mask = map_coordinates(mask, indices, order=0, mode="reflect").reshape(shape)
        return image, mask


class RandomGamma:
    """Gamma 校正"""
    def __init__(self, gamma_range=(0.8, 1.2), prob=0.3):
        self.gamma_range = gamma_range
        self.prob = prob
    def __call__(self, image, mask):
        if np.random.rand() >= self.prob:
            return image, mask
        # 将 [-inf, inf] 的 Z-score 缩放到 [0, 1] 做 gamma
        img_min, img_max = image.min(), image.max()
        img_norm = (image - img_min) / (img_max - img_min + 1e-7)
        gamma = np.random.uniform(*self.gamma_range)
        img_norm = np.power(np.clip(img_norm, 0, 1), gamma)
        image = img_norm * (img_max - img_min) + img_min
        return image, mask


class GaussianNoise:
    """高斯噪声"""
    def __init__(self, std=0.01, prob=0.3):
        self.std = std
        self.prob = prob
    def __call__(self, image, mask):
        if np.random.rand() >= self.prob:
            return image, mask
        noise = np.random.randn(*image.shape) * self.std
        return image + noise, mask


def build_train_augmentation(config):
    """从配置构建训练增强管线"""
    aug = config.get("augmentation", {})
    if not aug.get("enabled", True):
        return None

    transforms = []

    if aug.get("hflip_prob", 0) > 0:
        transforms.append(RandomHorizontalFlip(prob=aug["hflip_prob"]))
    if aug.get("vflip_prob", 0) > 0:
        transforms.append(RandomVerticalFlip(prob=aug["vflip_prob"]))
    if aug.get("rotate_limit", 0) > 0:
        transforms.append(RandomRotation())
    if aug.get("elastic", False):
        transforms.append(ElasticTransform(prob=0.3))
    if aug.get("gamma_range"):
        transforms.append(RandomGamma(gamma_range=tuple(aug["gamma_range"]), prob=0.3))
    if aug.get("noise_std", 0) > 0:
        transforms.append(GaussianNoise(std=aug["noise_std"], prob=0.3))

    return Compose(transforms) if transforms else None
