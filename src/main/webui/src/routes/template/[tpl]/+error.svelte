<script lang="ts">
    import {page} from "$app/state";
    import {ERROR_PAGE_UNKNOWN_CODE} from "$lib/js/constants";

    const customErrMsg = $derived.by(() => {
        switch(page.error?.message) {
            case "template_info":
                return "unable to load template data";
            case "user_info":
                return "unable to load user info";
            default:
                console.error("unrecognized error while loading template page", page.error);
                return "unknown error";
        }
    });
</script>

<div class="m-4">
    <div class="mb-6 text-2xl font-bold">
        Unable to load page
    </div>

    <div>
        {#if page.status === ERROR_PAGE_UNKNOWN_CODE}
            {customErrMsg}
        {:else}
            REST request failed.
            Status: {page.status}
            <br/>
            {page.error?.message}
        {/if}
    </div>
</div>
