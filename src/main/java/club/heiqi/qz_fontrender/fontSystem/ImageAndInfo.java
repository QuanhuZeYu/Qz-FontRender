package club.heiqi.qz_fontrender.fontSystem;

import com.github.bsideup.jabel.Desugar;

import java.awt.image.BufferedImage;

@Desugar
public record ImageAndInfo(BufferedImage image, CharacterInfo info) {

}
