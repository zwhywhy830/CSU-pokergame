package com.cards.io;

import javafx.scene.image.Image;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 牌面图像导出器接口：把 JavaFX {@link Image} 写入文件。
 * 导出格式可替换，当前实现 {@link PngCardImageWriter} 为极简 PNG 编码器。
 */
public interface CardImageWriter {

    /** 将图像写入指定文件。 */
    void write(Image image, Path file) throws IOException;
}
