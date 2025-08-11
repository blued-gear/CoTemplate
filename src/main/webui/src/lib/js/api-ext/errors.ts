import {ResponseError} from "$lib/js/api";

export interface Err {
    code: number;
    message: string;
}

interface ExceptionBody {
    message: string;
}

export async function parseHttpException(e: Error): Promise<Err | null> {
    if(!(e instanceof ResponseError))
        return null;
    if(!e.response.headers.get('content-type')?.startsWith("application/json"))
        return null;

    const errBody = (await e.response.json()) as ExceptionBody;
    if(errBody.message == undefined)
        return null;

    return {
        code: e.response.status,
        message: errBody.message,
    };
}
