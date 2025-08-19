import {error, type LoadEvent} from "@sveltejs/kit";
import type {TemplateDetailsDto} from "$lib/js/api";
import {API, ERROR_PAGE_UNKNOWN_CODE} from "$lib/js/constants";
import {parseHttpException} from "$lib/js/api-ext/errors";
import {ROLE_GUEST, rolePower} from "$lib/js/api-ext/roles";

export interface PageData {
    tplId: string;
    tplInfo: TemplateDetailsDto;
    teamName: string | null;
    userRole: string;
    userPower: number;
    serverTemplateMaxAge: number;
}

export async function load({ params }: LoadEvent): Promise<PageData> {
    const tplId = params.tpl!;

    let tplInfo: TemplateDetailsDto;
    try {
        tplInfo = await API.templateDetails(tplId);
    } catch(e) {
        console.error(e);
        const err = await parseHttpException(e);
        if(err != null) {
            error(err.code, err.message);
        } else {
            error(ERROR_PAGE_UNKNOWN_CODE, "template_info");
        }
    }

    let team: string | null = null;
    let role = ROLE_GUEST;
    try {
        const info = await API.getUserInfo();
        if(!info.isGuest && info.info?.template === tplId) {
            team = info.info.team;
            role = info.info.role;
        }
    } catch(e) {
        console.error(e);
        const err = await parseHttpException(e);
        if(err != null) {
            error(err.code, err.message);
        } else {
            error(ERROR_PAGE_UNKNOWN_CODE, "user_info");
        }
    }

    let maxAge = 0;
    try {
        maxAge = await API.maxTemplateAge();
    } catch(e) {
        console.error("get maxTemplateAge", e);
    }

    return {
        tplId: tplId,
        tplInfo: tplInfo,
        teamName: team,
        userRole: role,
        userPower: rolePower(role),
        serverTemplateMaxAge: maxAge,
    };
}
