<script lang="ts">
    import type {TemplateCreatedDto} from "$lib/js/api";
    import {Button, Input, Label} from "flowbite-svelte";
    import {goto} from "$app/navigation";

    interface Props {
        createdDto: TemplateCreatedDto;
    }
    const { createdDto }: Props = $props();

    function onContinue() {
        //TODO check if this works with app subpath
        goto(`/template/${createdDto.uniqueName}`);
    }
</script>

<div class="flex flex-col gap-2">
    <div>
        The CoTemplate was created.
        Please store the following info somewhere save as the ID is needed for others to join
        and the "Owner" credentials for managing the CoTemplate.
        <br/>
        <span class="font-bold">The password can not be reset.</span>
    </div>

    <div>
        <Label for="created_template" class="mb-2">Template ID</Label>
        <Input type="text" id="created_template" readonly value={createdDto.uniqueName} />
    </div>
    <div>
        <Label for="created_user" class="mb-2">Owner username</Label>
        <Input type="text" id="created_user" readonly value={createdDto.ownerUsername} />
    </div>
    <div>
        <Label for="created_pass" class="mb-2">Owner password</Label>
        <Input type="text" id="created_pass" readonly value={createdDto.ownerPassword} />
    </div>

    <Button class="mt-8" onclick={onContinue}>Continue</Button>
</div>
