package com.bodega.util;

import org.mindrot.jbcrypt.BCrypt;

/** Utilidad para el hashing y verificación de contraseñas con BCrypt. */
public final class PasswordUtil {

    private static final int ROUNDS_SALT = 12;

    private PasswordUtil() {
    }

    /**
     * @param passwordPlano contraseña en texto plano
     * @return el hash BCrypt (con salt de {@value ROUNDS_SALT} rounds), listo para almacenar en la base de datos
     */
    public static String hash(String passwordPlano) {
        return BCrypt.hashpw(passwordPlano, BCrypt.gensalt(ROUNDS_SALT));
    }

    /**
     * @param passwordPlano   contraseña en texto plano a verificar
     * @param hashAlmacenado  hash BCrypt almacenado en la base de datos
     * @return {@code true} si la contraseña corresponde al hash; {@code false} también si algún argumento es nulo/vacío
     */
    public static boolean verificar(String passwordPlano, String hashAlmacenado) {
        if (passwordPlano == null || hashAlmacenado == null || hashAlmacenado.isBlank()) {
            return false;
        }
        return BCrypt.checkpw(passwordPlano, hashAlmacenado);
    }

    /**
     * Política mínima de complejidad exigida a toda contraseña nueva (autocambio, alta de
     * usuario o restablecimiento manual): al menos 8 caracteres, con mayúscula, minúscula
     * y dígito. No exige símbolos para no frustrar a usuarios no técnicos (cajeros de una
     * bodega), manteniendo un piso razonable frente a ataques de diccionario/fuerza bruta.
     *
     * @param passwordPlano contraseña en texto plano a evaluar
     * @return {@code true} si cumple la política mínima
     */
    public static boolean cumplePoliticaMinima(String passwordPlano) {
        if (passwordPlano == null || passwordPlano.length() < 8) {
            return false;
        }
        boolean tieneMayuscula = passwordPlano.chars().anyMatch(Character::isUpperCase);
        boolean tieneMinuscula = passwordPlano.chars().anyMatch(Character::isLowerCase);
        boolean tieneDigito = passwordPlano.chars().anyMatch(Character::isDigit);
        return tieneMayuscula && tieneMinuscula && tieneDigito;
    }
}
