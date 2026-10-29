"use strict";

const rows = document.getElementById("requestRows");
const message = document.getElementById("message");
const refreshButton = document.getElementById("refreshButton");
const advisorView = document.body.dataset.mode === "advisor";

async function loadRequests() {
    refreshButton.disabled = true;
    message.textContent = "Loading requests...";
    rows.replaceChildren();

    try {
        const requests = await portal.request("/api/requests");

        if (!Array.isArray(requests)) {
            throw new Error("Unexpected response from the server.");
        }

        requests.forEach(request => {
            const row = document.createElement("tr");

            const values = [
                request.request_id,
                request.student_name,
                request.student_id,
                `${request.code} - ${request.title}`,
                request.section_number,
                request.schedule
            ];

            values.forEach(value => {
                const cell = document.createElement("td");
                cell.textContent = value ?? "";
                row.appendChild(cell);
            });

            const statusCell = document.createElement("td");
            const badge = document.createElement("span");
            const status = String(request.status ?? "");

            badge.classList.add("status");

            if (["PENDING", "APPROVED", "REJECTED"].includes(status)) {
                badge.classList.add(status.toLowerCase());
            }

            badge.textContent = status;
            statusCell.appendChild(badge);
            row.appendChild(statusCell);

            if (advisorView) {
                const actions = document.createElement("td");

                if (status === "PENDING") {
                    ["APPROVED", "REJECTED"].forEach(decision => {
                        const button = document.createElement("button");

                        button.type = "button";
                        button.textContent =
                            decision === "APPROVED" ? "Approve" : "Reject";
                        button.style.margin = "3px";

                        button.addEventListener("click", async () => {
                            actions.querySelectorAll("button").forEach(item => {
                                item.disabled = true;
                            });

                            try {
                                await portal.mutate(
                                    `/api/requests/${request.request_id}/status`,
                                    "PATCH",
                                    { status: decision }
                                );

                                await loadRequests();
                            } catch (error) {
                                message.textContent = error.message;

                                actions.querySelectorAll("button").forEach(item => {
                                    item.disabled = false;
                                });
                            }
                        });

                        actions.appendChild(button);
                    });
                } else {
                    actions.textContent = "Decision recorded";
                }

                row.appendChild(actions);
            }

            rows.appendChild(row);
        });

        message.textContent = requests.length
            ? `${requests.length} request(s) found.`
            : "No registration requests yet.";
    } catch (error) {
        rows.replaceChildren();
        message.textContent = error.message;
    } finally {
        refreshButton.disabled = false;
    }
}

refreshButton.addEventListener("click", loadRequests);

const signOutButton = document.getElementById("signOutButton");

if (signOutButton) {
    signOutButton.addEventListener("click", () => portal.signOut());
}

loadRequests();