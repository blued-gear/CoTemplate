import {Configuration, DefaultApi} from "$lib/js/api";

export const BASE_PATH = (() => {
    const subpathEnd = location.href.indexOf("/ui/");
    return location.href.substring(0, subpathEnd);
})();
export const API_PATH = BASE_PATH + "/api";

export const API = new DefaultApi(new Configuration({
    basePath: BASE_PATH,
}));

export const ERROR_PAGE_UNKNOWN_CODE = 599;

export const STORAGE_SELECTED_ITEMS = "SelectedItems";
