package com.bodega.service;

import com.bodega.dao.UsuarioDAO;
import com.bodega.model.EstadoCuenta;
import com.bodega.model.Rol;
import com.bodega.model.Usuario;
import com.bodega.util.PasswordUtil;
import com.bodega.util.SesionActivaRegistry;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Lógica de negocio de gestión de usuarios. Cualquier ADMINISTRADOR puede registrar
 * tanto a otros administradores como a vendedores (requisito de multi-administradores).
 */
public class UsuarioService {

    private final UsuarioDAO usuarioDAO;

    public UsuarioService() {
        this.usuarioDAO = new UsuarioDAO();
    }

    /** Resultado de una operación de gestión de usuarios. */
    public static class ResultadoOperacion {
        public final boolean exitoso;
        public final String mensaje;
        /** Id del usuario afectado, solo poblado por {@link #registrar} (para poder encadenar la activación de MFA); {@code null} en el resto de operaciones. */
        public final Integer usuarioId;

        public ResultadoOperacion(boolean exitoso, String mensaje) {
            this(exitoso, mensaje, null);
        }

        public ResultadoOperacion(boolean exitoso, String mensaje, Integer usuarioId) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
            this.usuarioId = usuarioId;
        }
    }

    /**
     * @param filtroRol     rol a filtrar, o {@code null} para no filtrar
     * @param filtroEstado  estado a filtrar, o {@code null} para no filtrar
     * @param textoBusqueda texto a buscar, o {@code null}/vacío para no filtrar
     * @return usuarios que cumplen los filtros
     * @throws SQLException si falla la consulta
     */
    public List<Usuario> listar(Rol filtroRol, EstadoCuenta filtroEstado, String textoBusqueda) throws SQLException {
        return usuarioDAO.listar(filtroRol, filtroEstado, textoBusqueda);
    }

    /**
     * Registra un nuevo usuario (administrador o vendedor) con contraseña temporal,
     * forzando el cambio de contraseña en su próximo login.
     *
     * @param nuevoUsuario       usuario a crear, con nombres, apellidos, usuario, correo, teléfono, rol y estado
     * @param passwordPlano      contraseña temporal en texto plano (se hashea con BCrypt antes de guardar)
     * @param confirmarPassword  confirmación de la contraseña temporal
     * @param idAdminCreador     id del administrador que registra el nuevo usuario
     * @return resultado exitoso con el id creado en el mensaje, o un mensaje de error de negocio
     *         (contraseñas no coinciden, muy corta, usuario/correo duplicado) si no
     * @throws SQLException si falla la inserción
     */
    public ResultadoOperacion registrar(Usuario nuevoUsuario, String passwordPlano, String confirmarPassword,
                                         int idAdminCreador) throws SQLException {

        if (passwordPlano == null || !passwordPlano.equals(confirmarPassword)) {
            return new ResultadoOperacion(false, "Las contraseñas ingresadas no coinciden.");
        }
        if (!PasswordUtil.cumplePoliticaMinima(passwordPlano)) {
            return new ResultadoOperacion(false,
                    "La contraseña temporal debe tener al menos 8 caracteres, con mayúscula, minúscula y número.");
        }
        if (usuarioDAO.existeNombreUsuarioOCorreo(nuevoUsuario.getNombreUsuario(), nuevoUsuario.getCorreo())) {
            return new ResultadoOperacion(false, "Ya existe un usuario con ese nombre de usuario o correo electrónico.");
        }

        nuevoUsuario.setPasswordHash(PasswordUtil.hash(passwordPlano));
        nuevoUsuario.setDebeCambiarPassword(true);
        nuevoUsuario.setCreadoPorId(idAdminCreador);

        int id = usuarioDAO.crear(nuevoUsuario);
        return new ResultadoOperacion(true, "Usuario registrado correctamente con ID " + id + ".", id);
    }

    /**
     * Cambia el rol de seguridad de un usuario. Un administrador no puede cambiarse el
     * rol a sí mismo (evita que se quede sin poder administrar el sistema por accidente).
     *
     * @param usuarioId          id del usuario a modificar
     * @param nuevoRol           ADMINISTRADOR o VENDEDOR
     * @param idAdminSolicitante id del administrador que ejecuta el cambio
     * @return resultado exitoso, o un mensaje de error si intenta modificarse a sí mismo
     * @throws SQLException si falla la actualización
     */
    public ResultadoOperacion cambiarRol(int usuarioId, Rol nuevoRol, int idAdminSolicitante) throws SQLException {
        if (usuarioId == idAdminSolicitante) {
            return new ResultadoOperacion(false, "No puede modificar su propio rol de seguridad.");
        }
        usuarioDAO.cambiarRol(usuarioId, nuevoRol);
        return new ResultadoOperacion(true, "Rol actualizado correctamente. Las sesiones activas de este usuario quedarán invalidadas en su próxima petición.");
    }

    /**
     * Activa o desactiva una cuenta. Un administrador no puede desactivar su propia cuenta
     * (evita bloquearse a sí mismo fuera del sistema).
     *
     * @param usuarioId          id del usuario a modificar
     * @param nuevoEstado        ACTIVO o INACTIVO
     * @param idAdminSolicitante id del administrador que ejecuta el cambio
     * @return resultado exitoso, o un mensaje de error si intenta desactivarse a sí mismo
     * @throws SQLException si falla la actualización
     */
    public ResultadoOperacion cambiarEstado(int usuarioId, EstadoCuenta nuevoEstado, int idAdminSolicitante) throws SQLException {
        if (usuarioId == idAdminSolicitante && nuevoEstado == EstadoCuenta.INACTIVO) {
            return new ResultadoOperacion(false, "No puede desactivar su propia cuenta.");
        }
        usuarioDAO.cambiarEstado(usuarioId, nuevoEstado);
        return new ResultadoOperacion(true, "Estado de la cuenta actualizado correctamente.");
    }

    /**
     * Edita el nombre, apellido, correo y teléfono de cualquier usuario (usado por un
     * administrador desde Gestión de Usuarios; a diferencia de {@link #actualizarDatosPropios},
     * que un VENDEDOR usa sobre sí mismo desde Mi Perfil). No modifica el nombre de usuario
     * (identificador de login) ni la contraseña.
     *
     * @param usuarioId id del usuario a editar
     * @param nombres   nuevos nombres
     * @param apellidos nuevos apellidos
     * @param correo    nuevo correo
     * @param telefono  nuevo teléfono (opcional)
     * @return resultado exitoso, o un mensaje de error si el correo ya lo usa otro usuario
     * @throws SQLException si falla la actualización
     */
    public ResultadoOperacion editarDatos(int usuarioId, String nombres, String apellidos, String correo, String telefono)
            throws SQLException {
        if (nombres == null || nombres.isBlank() || apellidos == null || apellidos.isBlank()
                || correo == null || correo.isBlank()) {
            return new ResultadoOperacion(false, "Nombres, apellidos y correo son obligatorios.");
        }
        if (usuarioDAO.existeCorreoEnOtroUsuario(correo, usuarioId)) {
            return new ResultadoOperacion(false, "Ya existe otro usuario con ese correo electrónico.");
        }

        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setNombres(nombres);
        usuario.setApellidos(apellidos);
        usuario.setCorreo(correo);
        usuario.setTelefono(telefono);
        usuarioDAO.actualizarDatos(usuario);
        return new ResultadoOperacion(true, "Datos del usuario actualizados correctamente.");
    }

    /**
     * Levanta el bloqueo temporal de una cuenta por intentos fallidos, sin tocar su contraseña.
     *
     * @param usuarioId id del usuario a desbloquear
     * @return resultado exitoso de la operación
     * @throws SQLException si falla la actualización
     */
    public ResultadoOperacion desbloquear(int usuarioId) throws SQLException {
        usuarioDAO.desbloquear(usuarioId);
        return new ResultadoOperacion(true, "Cuenta desbloqueada correctamente.");
    }

    /**
     * Genera una contraseña temporal aleatoria y la asigna, forzando el cambio en el
     * próximo login. Usado por un administrador para restablecer la clave de otro usuario.
     *
     * @param usuarioId id del usuario cuya contraseña se restablece
     * @return la contraseña temporal en texto plano, para que el administrador se la
     *         comunique al usuario (nunca se almacena en texto plano en la base de datos)
     * @throws SQLException si falla la actualización
     */
    public String restablecerPassword(int usuarioId) throws SQLException {
        String passwordTemporal = generarPasswordTemporal();
        usuarioDAO.restablecerPassword(usuarioId, PasswordUtil.hash(passwordTemporal));
        // Si el usuario tenía una sesión abierta en otro dispositivo, se la cierra: de lo
        // contrario seguiría navegando libremente con la contraseña vieja ya invalidada,
        // sin que se le exija la temporal nueva hasta que esa sesión expire por su cuenta
        // (el confinamiento a /perfil que aplica AuthenticationFilter se calcula al abrir
        // sesión, no en cada petición — mismo motivo por el que MfaService hace esto mismo).
        SesionActivaRegistry.invalidarSiExiste(usuarioId);
        return passwordTemporal;
    }

    /**
     * @param id id del usuario
     * @return el usuario si existe, o {@link Optional#empty()} si no
     * @throws SQLException si falla la consulta
     */
    public Optional<Usuario> buscarPorId(int id) throws SQLException {
        return usuarioDAO.buscarPorId(id);
    }

    /**
     * Permite a un usuario editar sus propios datos de contacto (Mi Perfil).
     *
     * @param usuario usuario con el id de la fila a actualizar y los nuevos datos de contacto
     * @return resultado exitoso
     * @throws SQLException si falla la actualización
     */
    public ResultadoOperacion actualizarDatosPropios(Usuario usuario) throws SQLException {
        usuarioDAO.actualizarDatos(usuario);
        return new ResultadoOperacion(true, "Sus datos de perfil se actualizaron correctamente.");
    }

    /**
     * Cambio de contraseña autoiniciado por el propio usuario desde Mi Perfil: requiere
     * validar la contraseña actual con BCrypt antes de aceptar la nueva.
     *
     * @param usuarioId               id del usuario
     * @param passwordActual          contraseña actual en texto plano, para verificación
     * @param passwordNueva           nueva contraseña en texto plano
     * @param confirmarPasswordNueva  confirmación de la nueva contraseña
     * @return resultado exitoso, o un mensaje de error de negocio (usuario inexistente,
     *         contraseña actual incorrecta, nuevas no coinciden, muy corta) si no
     * @throws SQLException si falla la actualización
     */
    public ResultadoOperacion cambiarPasswordPropia(int usuarioId, String passwordActual, String passwordNueva,
                                                     String confirmarPasswordNueva) throws SQLException {
        Optional<Usuario> usuarioOpt = usuarioDAO.buscarPorId(usuarioId);
        if (usuarioOpt.isEmpty()) {
            return new ResultadoOperacion(false, "Usuario no encontrado.");
        }
        if (!PasswordUtil.verificar(passwordActual, usuarioOpt.get().getPasswordHash())) {
            return new ResultadoOperacion(false, "La contraseña actual ingresada es incorrecta.");
        }
        if (passwordNueva == null || !passwordNueva.equals(confirmarPasswordNueva)) {
            return new ResultadoOperacion(false, "Las contraseñas nuevas ingresadas no coinciden.");
        }
        if (!PasswordUtil.cumplePoliticaMinima(passwordNueva)) {
            return new ResultadoOperacion(false,
                    "La nueva contraseña debe tener al menos 8 caracteres, con mayúscula, minúscula y número.");
        }

        usuarioDAO.actualizarPasswordPropia(usuarioId, PasswordUtil.hash(passwordNueva));
        return new ResultadoOperacion(true, "Contraseña actualizada correctamente.");
    }

    /**
     * @return una contraseña aleatoria de 12 caracteres que siempre cumple
     *         {@link PasswordUtil#cumplePoliticaMinima} (se garantiza por construcción al
     *         menos una mayúscula, una minúscula y un dígito, no dejado al azar), para que
     *         un restablecimiento de contraseña nunca entregue una clave que el propio
     *         sistema luego rechazaría si el usuario intentara reutilizarla tal cual.
     */
    private String generarPasswordTemporal() {
        String mayusculas = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String minusculas = "abcdefghijkmnpqrstuvwxyz";
        String digitos = "23456789";
        String simbolos = "!@#$";
        String todos = mayusculas + minusculas + digitos + simbolos;

        java.security.SecureRandom random = new java.security.SecureRandom();
        java.util.List<Character> caracteres = new java.util.ArrayList<>();
        caracteres.add(mayusculas.charAt(random.nextInt(mayusculas.length())));
        caracteres.add(minusculas.charAt(random.nextInt(minusculas.length())));
        caracteres.add(digitos.charAt(random.nextInt(digitos.length())));
        for (int i = 3; i < 12; i++) {
            caracteres.add(todos.charAt(random.nextInt(todos.length())));
        }
        java.util.Collections.shuffle(caracteres, random);

        StringBuilder sb = new StringBuilder();
        caracteres.forEach(sb::append);
        return sb.toString();
    }
}
