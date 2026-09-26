package com.bodega.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Properties;

/**
 * Cifrado de columnas sensibles a nivel de aplicación (AES-256-GCM), usado por los DAO
 * antes de persistir un valor en claro (ej. {@code clientes.direccion}) y para
 * descifrarlo al leerlo de vuelta. Se eligió cifrar en Java y no con {@code AES_ENCRYPT()}
 * de MySQL para que la clave nunca viaje dentro de una sentencia SQL (ver el comentario
 * de decisión en el módulo "Gestión de Clientes" de schema.sql) y para tener cifrado
 * autenticado (GCM detecta manipulación del texto cifrado).
 * <p>
 * Cada valor cifrado incluye su propio IV aleatorio de 12 bytes al inicio, seguido del
 * texto cifrado + tag de autenticación de GCM, todo codificado en Base64 para poder
 * guardarse en una columna de texto.
 */
public final class CifradoUtil {

    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int TAMANO_IV_BYTES = 12;
    private static final int TAMANO_TAG_BITS = 128;

    private static final SecretKeySpec CLAVE = cargarClave();
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private CifradoUtil() {
        // Clase utilitaria: no instanciable
    }

    /**
     * @param textoPlano valor en claro a cifrar, o {@code null}
     * @return el valor cifrado (IV + texto cifrado + tag) codificado en Base64,
     *         o {@code null} si {@code textoPlano} es {@code null}
     */
    public static String cifrar(String textoPlano) {
        if (textoPlano == null) {
            return null;
        }
        try {
            byte[] iv = new byte[TAMANO_IV_BYTES];
            ALEATORIO.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE, CLAVE, new GCMParameterSpec(TAMANO_TAG_BITS, iv));
            byte[] textoCifrado = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));

            byte[] combinado = new byte[iv.length + textoCifrado.length];
            System.arraycopy(iv, 0, combinado, 0, iv.length);
            System.arraycopy(textoCifrado, 0, combinado, iv.length, textoCifrado.length);

            return Base64.getEncoder().encodeToString(combinado);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Error al cifrar el valor.", e);
        }
    }

    /**
     * @param valorCifrado valor previamente cifrado con {@link #cifrar(String)}, o {@code null}
     * @return el valor en claro, o {@code null} si {@code valorCifrado} es {@code null}
     */
    public static String descifrar(String valorCifrado) {
        if (valorCifrado == null) {
            return null;
        }
        try {
            byte[] combinado = Base64.getDecoder().decode(valorCifrado);
            byte[] iv = new byte[TAMANO_IV_BYTES];
            System.arraycopy(combinado, 0, iv, 0, TAMANO_IV_BYTES);
            byte[] textoCifrado = new byte[combinado.length - TAMANO_IV_BYTES];
            System.arraycopy(combinado, TAMANO_IV_BYTES, textoCifrado, 0, textoCifrado.length);

            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE, CLAVE, new GCMParameterSpec(TAMANO_TAG_BITS, iv));
            byte[] textoPlano = cipher.doFinal(textoCifrado);

            return new String(textoPlano, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Error al descifrar el valor (¿clave incorrecta o dato corrupto?).", e);
        }
    }

    private static SecretKeySpec cargarClave() {
        Properties props = new Properties();
        try (InputStream input = CifradoUtil.class.getClassLoader()
                .getResourceAsStream("database.properties")) {
            if (input == null) {
                throw new RuntimeException("No se encontró database.properties en el classpath.");
            }
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Error al cargar database.properties: " + e.getMessage(), e);
        }

        String claveBase64 = props.getProperty("app.encryption.key");
        if (claveBase64 == null || claveBase64.isBlank()) {
            throw new RuntimeException("Falta la propiedad app.encryption.key en database.properties.");
        }
        byte[] claveBytes = Base64.getDecoder().decode(claveBase64.trim());
        if (claveBytes.length != 32) {
            throw new RuntimeException("app.encryption.key debe decodificar a 32 bytes (AES-256); tiene " + claveBytes.length + ".");
        }
        return new SecretKeySpec(claveBytes, "AES");
    }
}
