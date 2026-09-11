package com.cards.io;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * 极简 PNG 编码器实现：把 JavaFX Image 写成 PNG 文件。
 * 仅用于全不透明 RGB 图像（我们绘制的牌面/牌背均为不透明），
 * 因此无需引入 javafx-swing / ImageIO 桥接依赖。
 */
public final class PngCardImageWriter implements CardImageWriter {

    @Override
    public void write(Image image, Path file) throws IOException {
        int w = (int) Math.round(image.getWidth());
        int h = (int) Math.round(image.getHeight());
        PixelReader pr = image.getPixelReader();

        // 每行: 过滤器字节0 + RGB
        ByteArrayOutputStream raw = new ByteArrayOutputStream(w * h * 3 + h);
        byte[] scan = new byte[1 + w * 3];
        for (int y = 0; y < h; y++) {
            scan[0] = 0;
            int p = 1;
            for (int x = 0; x < w; x++) {
                int argb = pr.getArgb(x, y);
                scan[p++] = (byte) ((argb >> 16) & 0xFF);
                scan[p++] = (byte) ((argb >> 8) & 0xFF);
                scan[p++] = (byte) (argb & 0xFF);
            }
            raw.write(scan);
        }

        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        ByteArrayOutputStream idat = new ByteArrayOutputStream();
        try {
            deflater.setInput(raw.toByteArray());
            deflater.finish();
            byte[] buf = new byte[8192];
            while (!deflater.finished()) {
                int n = deflater.deflate(buf);
                idat.write(buf, 0, n);
            }
        } finally {
            deflater.end();
        }

        byte[] signature = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(file)))) {
            out.write(signature);
            writeChunk(out, "IHDR", ihdr(w, h));
            writeChunk(out, "IDAT", idat.toByteArray());
            writeChunk(out, "IEND", new byte[0]);
        }
    }

    private static byte[] ihdr(int w, int h) {
        byte[] d = new byte[13];
        d[0] = (byte) (w >> 24);
        d[1] = (byte) (w >> 16);
        d[2] = (byte) (w >> 8);
        d[3] = (byte) w;
        d[4] = (byte) (h >> 24);
        d[5] = (byte) (h >> 16);
        d[6] = (byte) (h >> 8);
        d[7] = (byte) h;
        d[8] = 8; // bit depth
        d[9] = 2; // color type: true color RGB
        return d;
    }

    private static void writeChunk(DataOutputStream out, String type, byte[] data) throws IOException {
        out.writeInt(data.length);
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        out.write(typeBytes);
        out.write(data);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        out.writeInt((int) crc.getValue());
    }
}
