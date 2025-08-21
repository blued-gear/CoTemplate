<script lang="ts">
    import type {TemplateCreatedDto} from "$lib/js/api";
    import {Button, Input, Label} from "flowbite-svelte";
    import {goto} from "$app/navigation";
    import {login} from "$lib/js/api-ext/auth";
    import {parseHttpException} from "$lib/js/api-ext/errors";

    interface Props {
        createdDto: TemplateCreatedDto;
    }
    const { createdDto }: Props = $props();

    async function onContinue() {
        try {
            const err = await login(createdDto.uniqueName, createdDto.ownerUsername, createdDto.ownerPassword);
            if(err != null)
                console.log("unable to login after template creation", err);
        } catch(e: any) {
            const err = await parseHttpException(e);
            if(err == null)
                console.log("unable to login after template creation", e);
            else
                console.log("unable to login after template creation", err);
        }

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
