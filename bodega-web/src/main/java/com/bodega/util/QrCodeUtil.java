package com.bodega.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/** Generación de códigos QR como imagen PNG, usada en comprobantes y en el POS. */
public final class QrCodeUtil {

    private QrCodeUtil() {
        // Clase utilitaria: no instanciable
    }

    /**
     * @param contenido texto a codificar en el QR
     * @param tamanoPx  ancho y alto del QR generado, en píxeles (cuadrado)
     * @return el QR generado como PNG en blanco y negro
     * @throws WriterException si el contenido no se puede codificar como QR
     * @throws IOException     si falla la escritura del PNG
     */
    public static byte[] generarPng(String contenido, int tamanoPx) throws WriterException, IOException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix matriz = new QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, tamanoPx, tamanoPx, hints);

        BufferedImage imagen = new BufferedImage(tamanoPx, tamanoPx, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < tamanoPx; x++) {
            for (int y = 0; y < tamanoPx; y++) {
                imagen.setRGB(x, y, matriz.get(x, y) ? 0x000000 : 0xFFFFFF);
            }
        }

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);
        return salida.toByteArray();
    }
}
