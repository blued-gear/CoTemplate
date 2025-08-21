<script lang="ts">
    import {Button, Input, Label, Modal} from "flowbite-svelte";
    import type {TeamCreatedDto} from "$lib/js/api";
    import {API} from "$lib/js/constants";
    import {parseHttpException} from "$lib/js/api-ext/errors";
    import MessageToast from "$lib/components/MessageToast.svelte";

    interface Props {
        open: boolean;
        tplId: string;
    }
    let { open = $bindable(), tplId }: Props = $props();

    let step = $state(0);
    let errMsg: string | null = $state(null);
    let name = $state("");
    let newTeamInfo: TeamCreatedDto | null = $state(null);

    $effect(() => {
        if(!open) {
            step = 0;
            newTeamInfo = null;
        }
    });

    async function onNameEnter(e: KeyboardEvent) {
        if(e.key !== "Enter") return;
        if(name === "") return;
        await onCreate();
    }

    async function onCreate() {
        errMsg = null;
        try {
            newTeamInfo = await API.createTeam(tplId, name);
            step++;
        } catch(e) {
            const err = await parseHttpException(e);
            if(err != null) {
                console.error("unable to create team", err);
                errMsg = err.message;
            } else {
                console.error("unable to create team", e);
                errMsg = "unknown error";
            }
        }
    }
</script>

<Modal bind:open={open} title="Create new Team" size="sm">
    <div class="mt-2 flex flex-col gap-2">
        <MessageToast show={errMsg !== null}>
            {#snippet content()}
                Unable to create team:
                {errMsg}
            {/snippet}
        </MessageToast>

    {#if step === 0}
        <Label>
            <span>Team name:</span>
            <Input bind:value={name} onkeypress={onNameEnter}></Input>
            <span>The name may contain only a-z, A-Z, 0-9, '_', ':' and must be between 4 and 128 characters long.</span>
        </Label>

        <Button disabled={name === ""} class="mt-8" onclick={onCreate}>Create</Button>
    {:else if step === 1}
        <div>
            The team was created.
            Please store the following info somewhere save as it is needed for the login.
            <br/>
            <span class="font-bold">The credentials can not be reset.</span>
        </div>

        <div class="mt-2">
            <Label>
                <span>Username</span>
                <Input type="text" readonly value={newTeamInfo?.name} />
            </Label>
            <Label>
                <span>Password</span>
                <Input type="text" readonly value={newTeamInfo?.password} />
            </Label>
        </div>
    {:else}
        ERROR IN PAGE LOGIC
    {/if}
    </div>
</Modal>
