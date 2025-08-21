<script lang="ts">
    import type {TemplateCreatedDto} from "$lib/js/api";
    import {Fileupload, Input, Label} from "flowbite-svelte";
    import {API} from "$lib/js/constants";
    import CreateTemplateStepInfo from "$lib/components/CreateTemplateStepInfo.svelte";
    import {parseHttpException} from "$lib/js/api-ext/errors";
    import MessageToast from "$lib/components/MessageToast.svelte";

    let errMsg: string | null = $state(null);
    let tplName = $state("");
    let file: FileList | null = $state(null);
    let createdTplInfo: TemplateCreatedDto | null = $state(null);

    async function onImport() {
        if(file == null || file.length !== 1 || tplName == "")
            return;

        try {
            createdTplInfo = await API.importTemplate(tplName, file.item(0)!);
        } catch(e) {
            console.error(e);
            const err = await parseHttpException(e);
            errMsg = err?.message ?? "unknown error";
        }
    }
</script>

<div>
    {#if createdTplInfo == null}
        <MessageToast show={errMsg != null}>
            {#snippet content()}
                Unable to import template:
                {errMsg}
            {/snippet}
        </MessageToast>

        <form class="flex flex-col gap-4" onsubmit={onImport}>
            <div>
                <Label for="new_template" class="mb-2">Template Name</Label>
                <Input type="text" id="new_template" required bind:value={tplName} />
                <span>The name may contain only a-z, A-Z, 0-9, '_', ':' and must be between 4 and 128 characters long.</span>
            </div>

            <Fileupload bind:files={file} required />

            <button type="submit" class="mt-4">Import</button>
        </form>
    {:else}
        <CreateTemplateStepInfo createdDto={createdTplInfo} />
    {/if}
</div>
