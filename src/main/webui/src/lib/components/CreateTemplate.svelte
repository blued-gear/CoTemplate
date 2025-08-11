<script lang="ts">
    import CreateTemplateStepForm from "$lib/components/CreateTemplateStepForm.svelte";
    import CreateTemplateStepInfo from "$lib/components/CreateTemplateStepInfo.svelte";
    import type {TemplateCreatedDto} from "$lib/js/api";

    let createdTplInfo: TemplateCreatedDto | null = $state(null);

    function onCreated(resp: TemplateCreatedDto) {
        createdTplInfo = resp;
    }
</script>

<div>
{#if createdTplInfo == null}
    <CreateTemplateStepForm onCreated={onCreated} />
{:else}
    <CreateTemplateStepInfo createdDto={createdTplInfo} />
{/if}
</div>
