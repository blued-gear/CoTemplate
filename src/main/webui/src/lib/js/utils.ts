export async function sleep(ms: number) {
    await new Promise(resolve => setTimeout(resolve, ms));
}

export function preventDefault<E extends Event, R>(handler: (ev: E) => Promise<R>): ((ev: E) => Promise<R>) {
    return (ev: E) => {
        ev.preventDefault();
        return handler(ev);
    }
}
