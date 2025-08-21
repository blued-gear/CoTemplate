import {error} from "@sveltejs/kit";
import {API, ERROR_PAGE_UNKNOWN_CODE} from "$lib/js/constants";
import {parseHttpException} from "$lib/js/api-ext/errors";
import {ROLE_ADMIN} from "$lib/js/api-ext/roles";

export interface PageData {
    hasAuth: boolean;
}

export async function load(): Promise<PageData> {
    try {
        const info = await API.getUserInfo();
        return {
            hasAuth: !info.isGuest && info.info!.role === ROLE_ADMIN,
        };
    } catch(e) {
        const err = await parseHttpException(e);
        console.error("unable to load auth info", err ?? e);
        error(ERROR_PAGE_UNKNOWN_CODE, "user_info");
    }
}
