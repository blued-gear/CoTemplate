<script lang="ts">
    import {Input, Label, Toast} from "flowbite-svelte";
    import TemplateSettings from "$lib/components/TemplateSettings.svelte";
    import {
        type TeamCreatePolicy,
        TeamCreatePolicy as TeamCreatePolicyVals,
        type TemplateCreatedDto,
        type TemplateCreateDto
    } from "$lib/js/api";
    import {API} from "$lib/js/constants";
    import {parseHttpException} from "$lib/js/api-ext/errors";

    interface Props {
        onCreated: (resp: TemplateCreatedDto) => void;
    }
    const { onCreated }: Props = $props();

    let tplName = $state("");
    let tplSizeW = $state(1);
    let tplSizeH = $state(1);
    let tplTcp: TeamCreatePolicy = $state(TeamCreatePolicyVals.Everyone);

    let apiErrMsg = $state<string | null>(null);
    let toastError = $derived(apiErrMsg != null);

    async function submit() {
        apiErrMsg = null;

        try {
            const reqData: TemplateCreateDto = {
                name: tplName,
                width: tplSizeW,
                height: tplSizeH,
                teamCreatePolicy: tplTcp
            };
            const resp = await API.createTemplate(reqData);
            onCreated(resp);
        } catch(e: any) {
            console.error(e);
            const err = await parseHttpException(e);
            apiErrMsg = err?.message ?? "unknown error";
        }
    }
</script>

<form onsubmit={submit}>
    <Toast class="mb-2 max-w-full!" toastStatus={toastError}>
        Unable to create template:
        {apiErrMsg}
    </Toast>

    <div class="flex flex-col">
        <div>
            <Label for="new_template" class="mb-2">Template Name</Label>
            <Input type="text" id="new_template" required bind:value={tplName} />
            <span>The name may contain only a-z, A-Z, 0-9, '_', ':' and must be between 4 and 128 characters long.</span>
        </div>

        <div class="mt-3 mb-2 text-sm">(The following settings can be changed later)</div>
        <TemplateSettings bind:sizeW={tplSizeW} bind:sizeH={tplSizeH} bind:teamCreatePolicy={tplTcp} />

        <button type="submit" class="mt-8">Create</button>
    </div>
</form>
