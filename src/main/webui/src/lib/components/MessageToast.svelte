<script lang="ts">
    import Icon from "@iconify/svelte";
    import {Toast} from "flowbite-svelte";
    import type {Snippet} from "svelte";

    interface Props {
        content: Snippet<any>;
        show: boolean;
        color?: string;
        iconName?: Icon;
    }
    const { content, show, color = "red", iconName = "mdi:alert-circle-outline" } = $props();
</script>

<Toast toastStatus={show} color={color} class="mb-2 max-w-full!">
    {#snippet icon()}
        <Icon class="h-5 w-5" icon={iconName} />
        <span class="sr-only">Warning icon</span>
    {/snippet}

    {@render content()}
</Toast>
