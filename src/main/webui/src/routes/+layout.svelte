<script lang="ts">
    import '../app.css';
    import favicon from '$lib/assets/favicon.svg';
    import {Popover} from "flowbite-svelte";
    import IconButton from "$lib/components/IconButton.svelte";

    let { children } = $props();
</script>

<svelte:head>
	<link rel="icon" href={favicon} />
    <title>CoTemplate</title>
</svelte:head>

<div class="text-black dark:text-white bg-white dark:bg-gray-800 min-h-screen flex flex-col">
    <div class="grow">
        {@render children?.()}
    </div>

    <div class="p-1 flex gap-3">
        <IconButton icon="mdi:cookie" onClick={()=>{}} />
        <Popover>
            This page uses only technical necessary cookies.
        </Popover>

        <a href="https://projects.chocolatecakecodes.goip.de/blued_gear/cotemplate" title="Sourcecode">
            <IconButton icon="mdi:code-tags" onClick={()=>{}} />
        </a>

        <IconButton icon="mdi:brightness-6" onClick={()=>{ document.body.classList.toggle("dark") }} />
    </div>
</div>
