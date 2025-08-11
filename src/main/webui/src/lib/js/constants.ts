import {Configuration, DefaultApi} from "$lib/js/api";

export const BASE_PATH = (() => {
    if(location.hostname === 'localhost') {
        return "//localhost:8080";
    } else {
        const subpathEnd = location.href.indexOf("/ui/");
        return location.href.substring(0, subpathEnd);
    }
})();
export const API_PATH = BASE_PATH + "/api";

export const API = new DefaultApi(new Configuration({
    basePath: BASE_PATH,
}));
