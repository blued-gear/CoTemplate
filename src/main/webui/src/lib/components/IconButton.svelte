<script lang="ts">
    import {Button} from "flowbite-svelte";
    import Icon from "@iconify/svelte";

    interface Props {
        icon: string;
        onClick: () => void;
        small?: boolean;
        classO?: string;
    }
    const { icon, onClick, small = false, classO = "" }: Props = $props();

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
    })
</script>

<Button class={btnClass} onclick={onClick}><Icon icon="{icon}"></Icon></Button>
