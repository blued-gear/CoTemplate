<script>
    import favicon from "$lib/assets/favicon.png";

    let { children } = $props();
</script>

<svelte:head>
    <link rel="icon" href={favicon} />
    <title>Admin | CoTemplate</title>
</svelte:head>

{@render children?.()}
