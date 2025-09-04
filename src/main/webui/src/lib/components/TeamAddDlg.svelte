<script lang="ts">
    import {Button, Input, Label, Modal} from "flowbite-svelte";
    import type {TeamCreatedDto} from "$lib/js/api";
    import {API} from "$lib/js/constants";
    import {parseHttpException} from "$lib/js/api-ext/errors";
    import MessageToast from "$lib/components/MessageToast.svelte";
    import {ROLE_GUEST} from "$lib/js/api-ext/roles";
    import {login} from "$lib/js/api-ext/auth";

    interface Props {
        open: boolean;
        tplId: string;
        userRole: string;
    }
    let { open = $bindable(), tplId, userRole }: Props = $props();

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
                errMsg = `unable to create team: ${err.message}`;
            } else {
                console.error("unable to create team", e);
                errMsg = "unable to create team: unknown error";
            }
        }
    }

    async function onFinish() {
        errMsg = null;

        if(userRole !== ROLE_GUEST) {
            open = false;
        } else {
            // if guest then login as created team
            try {
                const err = await login(tplId, newTeamInfo!.name, newTeamInfo!.password);
                if (err == null) {
                    location.reload();
                } else {
                    console.error("unable to login as created team", err);
                    errMsg = `unable to login: ${err?.message ?? "unknown error"}`;
                }
            } catch (e) {
                const err = await parseHttpException(e);
                if (err != null) {
                    console.error("unable to login as created team", err);
                    errMsg = `unable to login: ${err.message}`;
                } else {
                    console.error("unable to login as created team", e);
                    errMsg = "unable to login: unknown error";
                }
            }
        }
    }
</script>

<Modal bind:open={open} title="Create new Team" size="sm">
    <div class="mt-2 flex flex-col gap-2">
        <MessageToast show={errMsg !== null}>
            {#snippet content()}
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
            <br/>

            Other members of your team can use these credentials on the
            <a href="../">login page</a>
            with the Template ID
            <span class="mx-1 italic">{tplId}</span> .
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

        <Button onclick={onFinish}>Ok</Button>
    {:else}
        ERROR IN PAGE LOGIC
    {/if}
    </div>
</Modal>
