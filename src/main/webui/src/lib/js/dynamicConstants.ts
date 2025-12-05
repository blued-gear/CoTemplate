import {API} from "$lib/js/constants";

export interface DynamicConstants {
    maxTemplateSize: number;
}

const dynamicConstants = load();

export default dynamicConstants;

async function load(): Promise<DynamicConstants> {
    try {
        const maxTemplateSize = await API.maxTemplateSize();
        return {
            maxTemplateSize,
        };
    } catch(e) {
        console.error("unable to load dynamic constants from server", e);
        throw Error("fatal");
    }
}
