<script>
    import {page} from "$app/state";
</script>

<div class="m-4">
    <div class="mb-6 text-2xl font-bold">
        Unable to load page
    </div>

    <div>
        {#if page.error == null}
            Unknown error
        {:else}
            {page.error.message}
        {/if}
    </div>
</div>
