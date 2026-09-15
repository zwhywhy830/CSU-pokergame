package com.cards.render;

import com.cards.model.Card;
import javafx.scene.image.Image;

/**
 * 扑克牌图像渲染器接口：把 {@link Card} 绘制成 JavaFX {@link Image}。
 * 绘制方式可替换（Canvas 矢量绘制、位图加载等），当前实现为 {@link CanvasCardRenderer}。
 */
public interface CardRenderer {

    /** 高清牌面宽度（像素）。 */
    double WIDTH = 240;
    /** 高清牌面高度（像素）。 */
    double HEIGHT = 340;

    /** 渲染牌面（正面）。 */
    Image face(Card card);

    /** 渲染牌背：red=true 为红色牌背，false 为蓝色牌背。 */
    Image back(boolean red);
}
