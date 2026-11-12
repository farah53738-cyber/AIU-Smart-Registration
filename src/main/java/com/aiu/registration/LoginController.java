
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
                <title>Sign in | AIU Course Registration System</title>

                <style>
                :root{font-family:"Segoe UI",Arial,sans-serif;color:#2b2340;background:#f7f5fa}
                *{box-sizing:border-box}body{margin:0;min-height:100vh;display:grid;place-items:center;padding:25px;background:radial-gradient(circle at 20% 10%,#eee4fa,transparent 38%),#f7f5fa}
                form{width:min(100%,410px);background:#fff;padding:38px;border:1px solid #eae4f1;border-radius:20px;box-shadow:0 25px 60px rgba(64,43,90,.09)}
                .logo{height:51px;width:51px;display:grid;place-items:center;background:linear-gradient(140deg,#594078,#8c6ba7);color:#fff;border-radius:14px;font-size:19px;font-weight:800;margin-bottom:24px}
                h1{color:#332747;font-size:26px;letter-spacing:-.03em;margin:0 0 8px}p{color:#756d80;font-size:13px;line-height:1.6;margin:0 0 25px}
                label{display:block;font-size:12px;font-weight:650;color:#4d425d;margin:15px 0 7px}input{width:100%;padding:12px 13px;border:1px solid #dcd5e5;border-radius:9px;font:inherit;font-size:14px}input:focus-visible,button:focus-visible{outline:3px solid #b28ad5;outline-offset:2px}
                button{width:100%;background:#694e88;color:#fff;border:0;border-radius:9px;padding:13px;cursor:pointer;font-family:inherit;font-size:13px;font-weight:700;margin-top:22px}button:hover{background:#563a79}
                .help{color:#80758b;text-align:center;margin-top:18px;font-size:11px}.feedback{padding:12px;border-radius:8px;font-size:12px;margin:12px 0}.error{background:#fff0f3;color:#9e345b}.ok{background:#eaf5f0;color:#347257}
                </style>
            </head>

            <body>
                <form method="post" action="/login">
                    <div class="logo" aria-hidden="true">AIU</div><h1>AIU Course Registration System</h1><p>Sign in with your university account to access registration and academic advising.</p><div id="feedback" role="status" aria-live="polite"></div>

                  

                    <label for="username">University ID</label>
                    <input
                        id="username"
                        name="username"
                        type="text"
                        value=""
                        autocomplete="username"
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

                    <button type="submit">Sign in to your portal</button><div class="help">AIU Course Registration System · Academic services</div>
                </form>
                <script>
                const params = new URLSearchParams(location.search);
                const box = document.getElementById('feedback');
                if (params.has('error')) {box.className='feedback error';box.textContent='Those details could not be verified. Check your university ID and password.';}
                else if (params.has('logout')) {box.className='feedback ok';box.textContent='You have signed out successfully.';}
                </script>
            </body>
            </html>
            """
            .replace("CSRF_NAME", csrfName)
            .replace("CSRF_VALUE", csrfValue);
    }
}
