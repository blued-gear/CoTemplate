import {API_PATH} from "$lib/js/constants";
import type {Err} from "./errors";

export async function login(template: string, username: string, password: string): Promise<Err | null> {
    const body = new URLSearchParams();
    body.set("template", template);
    body.set("username", username);
    body.set("password", password);

    const resp = await fetch(`${API_PATH}/auth/login`, {
        method: "POST",
        body: body,
    });

    if(resp.status !== 200)
        return {
            code: resp.status,
            message: await resp.text(),
        };
    return null;
}

export async function logout(): Promise<Err | null> {
    const resp = await fetch(`${API_PATH}/auth/logout`, {
        method: "POST",
    });

    if(resp.status !== 200)
        return {
            code: resp.status,
            message: await resp.text(),
        };
    return null;
}
