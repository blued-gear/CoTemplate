<script lang="ts">
    import {Button, Input, Label} from "flowbite-svelte";
    import type {PageData} from "./+page";
    import type {TemplateDetailsDto} from "$lib/js/api";
    import {API} from "$lib/js/constants";
    import {parseHttpException} from "$lib/js/api-ext/errors";
    import MessageToast from "$lib/components/MessageToast.svelte";
    import IconButton from "$lib/components/IconButton.svelte";
    import {invalidateAll} from "$app/navigation";
    import {login} from "$lib/js/api-ext/auth";

    type TemplateDetails = TemplateDetailsDto & { id: string };

    interface Props {
        data: PageData;
    }
    const { data }: Props = $props();

    let errMsg: string | null = $state(null);
    let templates: TemplateDetails[] = $state([]);
    let authName = $state("");
    let authPass = $state("");

    async function loadTemplates() {
        errMsg = null;

        try {
            const resp = await API.getTemplates();
            templates = Object.entries(resp.templates).map(([k, v]) => {
                return {
                    ...v,
                    id: k,
                };
            });
        } catch(e) {
            const err = await parseHttpException(e);
            console.error("failed to load templates", err ?? e);
            errMsg = err?.message ?? "unknown reason";
        }
    }

    async function onRmTemplate(name: string) {
        try {
            await API.deleteTemplate(name);
        } catch(e) {
            const err = await parseHttpException(e);
            console.error(`failed to delete template ${name}`, err ?? e);
            errMsg = err?.message ?? "unknown reason";
            return;
        }

        await loadTemplates();
    }

    async function onLogin() {
        try {
            const err = await login("_", authName, authPass);
            if(err != null) {
                console.error("failed to login", err);
                errMsg = err.message;
                return;
            }
        } catch(e) {
            const err = await parseHttpException(e);
            console.error(`failed to delete template ${name}`, err ?? e);
            errMsg = err?.message ?? "unknown reason";
            return;
        }

        await invalidateAll();
        await loadTemplates();
    }

    if(data.hasAuth)
        loadTemplates();
</script>

<div class="p-3">
    {#if !data.hasAuth}
        <MessageToast show={errMsg != null}>
            {#snippet content()}
                Unable to log in:
                {errMsg}
            {/snippet}
        </MessageToast>

        <form class="flex flex-col gap-4" onsubmit={onLogin}>
            <Label>
                <span>Username</span>
                <Input required bind:value={authName}/>
            </Label>
            <Label>
                <span>Password</span>
                <Input required type="password" bind:value={authPass}/>
            </Label>
            <Button type="submit">Login</Button>
        </form>
    {:else}
        <MessageToast show={errMsg != null}>
            {#snippet content()}
                Unable to delete template:
                {errMsg}
            {/snippet}
        </MessageToast>

        <div class="mb-2">Templates:</div>
        <div class="w-full flex flex-col gap-3 overflow-auto">
            {#each templates as tpl}
                <div class="p-1 flex flex-row gap-2 border-b border-gray-800">
                    <IconButton icon="mdi:trash" onClick={async () => await onRmTemplate(tpl.id)}/>
                    <a href={`./template/${tpl.id}`} class="underline text-blue-600">{tpl.name}</a>
                    <div>Created: {new Date(tpl.createdAt ?? 0).toLocaleString()}</div>
                    <div>Items: {tpl.templateCount}</div>
                </div>
            {/each}
        </div>
    {/if}
</div>
