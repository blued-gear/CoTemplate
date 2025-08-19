<script lang="ts">
    import {Button} from "flowbite-svelte";
    import Icon from "@iconify/svelte";

    interface Props {
        icon: string;
        onClick: ((e: MouseEvent) => void) | (() => void);
        small?: boolean;
        classO?: string;
        stopClickPropagation?: boolean;
    }
    const { icon, onClick, small = false, classO = "", stopClickPropagation = false }: Props = $props();

    const btnClass = $derived.by(() => {
        const classes: Record<string, boolean> = {};
        if(small) {
            classes["p-1!"] = true;
            classes["rounded-xl!"] = true;
        } else {
            classes["p-2!"] = true;
        }

        classO.split(" ").forEach(c => classes[c] = true);

        return classes;
    });

    function clicked(e: MouseEvent) {
        if(stopClickPropagation)
            e.stopPropagation();
        onClick(e);
    }
</script>

<Button class={btnClass} onclick={clicked}><Icon icon="{icon}"></Icon></Button>
