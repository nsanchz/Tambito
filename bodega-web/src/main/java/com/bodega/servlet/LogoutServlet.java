package com.bodega.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/** Invalida la sesión activa y redirige al formulario de inicio de sesión. */
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

    /**
     * @param req  petición HTTP
     * @param resp respuesta HTTP
     * @throws ServletException no se lanza en la práctica (firma heredada de HttpServlet)
     * @throws IOException      si falla el redirect
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        resp.sendRedirect(req.getContextPath() + "/login");
    }
}
