window.portal = {
    async request(url, options = {}) {
        const response = await fetch(url, {
            ...options,
            headers: {
                Accept: "application/json",
                ...(options.headers || {})
            },
            cache: "no-store"
        });

        if (response.redirected || response.status === 401) {
            throw new Error("Please sign in again.");
        }

        const data = await response.json().catch(() => ({}));

        if (!response.ok) {
            throw new Error(data.message || "Request failed.");
        }

        return data;
    },

    async mutate(url, method, body) {
        const csrf = await this.request("/api/csrf");

        return this.request(url, {
            method,
            headers: {
                "Content-Type": "application/json",
                [csrf.headerName]: csrf.token
            },
            body: JSON.stringify(body)
        });
    },

    async signOut() {
        const csrf = await this.request("/api/csrf");
        const form = document.createElement("form");
        form.method = "POST";
        form.action = "/logout";
        const field = document.createElement("input");
        field.type = "hidden";
        field.name = csrf.parameterName;
        field.value = csrf.token;
        form.append(field);
        document.body.append(form);
        form.submit();
    }
};