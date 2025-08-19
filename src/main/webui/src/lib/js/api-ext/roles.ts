export const ROLE_ADMIN = "ADMIN";
export const ROLE_TEMPLATE_OWNER = "TEMPLATE_OWNER";
export const ROLE_TEMPLATE_TEAM = "TEMPLATE_TEAM";
export const ROLE_GUEST = "GUEST";

export const POWER_VIEW = 0;
export const POWER_EDIT_OWN_IMG = 1;
export const POWER_EDIT_TPL = 2;
export const POWER_ADMIN = 3;

export function rolePower(role: string): number {
    switch(role) {
        case ROLE_ADMIN: return POWER_ADMIN;
        case ROLE_TEMPLATE_OWNER: return POWER_EDIT_TPL;
        case ROLE_TEMPLATE_TEAM: return POWER_EDIT_OWN_IMG;
        case ROLE_GUEST: return POWER_VIEW;
        default: return -1;
    }
}
