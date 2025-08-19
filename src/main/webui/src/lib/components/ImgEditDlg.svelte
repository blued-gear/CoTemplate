<script lang="ts">
    import {Button, Fileupload, Input, Label, Modal} from "flowbite-svelte";
    import type {ImgProperties} from "$lib/js/types";

    interface Props {
        open: boolean;
        create: boolean;
        onSubmit: (img: ImgProperties) => void;
        initialData?: ImgProperties | null;
        title?: string;
    }
    let { open = $bindable(), create, onSubmit, initialData = null, title = "" }: Props = $props();

    let desc = $derived(initialData?.description ?? "");
    let x = $derived(initialData?.x ?? 0);
    let y = $derived(initialData?.y ?? 0);
    let z = $derived(initialData?.z ?? 0);
    let selectedImage: FileList | null = $state(null);

    // reset selected file on open
    $effect(() => {
        if(!open) {
            selectedImage = null;
        }
    });

    function submit() {
        if(create) {
            if (selectedImage == null || selectedImage.length === 0) return;
            onSubmit({
                description: desc,
                x: x,
                y: y,
                z: z,
                imgData: selectedImage.item(0)!,
                id: null,
            });
        } else {
            onSubmit({
                description: desc,
                x: x,
                y: y,
                z: z,
                imgData: selectedImage?.item(0) ?? null,
                id: initialData?.id ?? null,
            });
        }
    }
</script>

<Modal bind:open={open} title={title} form size="md" onsubmit={submit}>
    <div class="mt-2 flex flex-col gap-2">
        <Label>
            <span>Description</span>
            <Input type="text" bind:value={desc} />
        </Label>

        <div>
            <Fileupload bind:files={selectedImage} required={create} />
            {#if !create}
            <span>(optional)</span>
            {/if}
        </div>

        <Label>
            <span>X:</span>
            <Input type="number" bind:value={x} />
        </Label>
        <Label>
            <span>Y:</span>
            <Input type="number" bind:value={y} />
        </Label>
        <Label>
            <span>Z:</span>
            <Input type="number" bind:value={z} />
        </Label>

        <Button type="submit" class="mt-2">Ok</Button>
    </div>
</Modal>
