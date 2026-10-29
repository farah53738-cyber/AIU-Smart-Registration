
package com.aiu.registration;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

@Controller
public class LoginController {

    @GetMapping(value = "/login", produces = "text/html")
    @ResponseBody
    public String login(HttpServletRequest request) {

        CsrfToken csrf = (CsrfToken) request.getAttribute("_csrf");

        String csrfName = HtmlUtils.htmlEscape(csrf.getParameterName());
        String csrfValue = HtmlUtils.htmlEscape(csrf.getToken());

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport"
                      content="width=device-width, initial-scale=1">
                <title>AIU Login</title>

                <style>
                    body {
                        font-family: Arial, sans-serif;
                        background: #f8f5fc;
                        padding: 40px 20px;
                    }

                    form {
                        max-width: 350px;
                        margin: 40px auto;
                        background: white;
                        padding: 30px;
                        border-radius: 14px;
                    }

                    h1 {
                        color: #765a98;
                    }

                    input, button {
                        box-sizing: border-box;
                        width: 100%;
                        padding: 12px;
                        margin: 8px 0 16px;
                    }

                    button {
                        background: #765a98;
                        color: white;
                        border: 0;
                        border-radius: 7px;
                        cursor: pointer;
                    }

                    button:hover {
                        background: #634b83;
                    }
                </style>
            </head>

            <body>
                <form method="post" action="/login">
                    <h1>AIU Sign In</h1>

                    <p>Use student1, student2, or advisor.</p>

                    <label for="username">Username</label>
                    <input
                        id="username"
                        name="username"
                        type="text"
                        value=""
                        autocomplete="off"
                        required
                    >

                    <label for="password">Password</label>
                    <input
                        id="password"
                        name="password"
                        type="password"
                        autocomplete="current-password"
                        required
                    >

                    <input type="hidden"
                           name="CSRF_NAME"
                           value="CSRF_VALUE">

                    <button type="submit">Sign in</button>
                </form>
            </body>
            </html>
            """
            .replace("CSRF_NAME", csrfName)
            .replace("CSRF_VALUE", csrfValue);
    }
}
