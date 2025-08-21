<script lang="ts">
    import {Button, Input, Label, Popover, Toast} from "flowbite-svelte";
    import Icon from "@iconify/svelte";
    import {login} from "$lib/js/api-ext/auth";
    import {parseHttpException} from "$lib/js/api-ext/errors";
    import {goto} from "$app/navigation";
    import MessageToast from "$lib/components/MessageToast.svelte";

    let tplName = $state("");
    let tplUser = $state("");
    let tplPass = $state("");

    let loginErrMsg = $state<string | null>(null);
    let toastMissingPass = $state(false);
    const toastError = $derived(loginErrMsg != null);

    async function submit() {
        toastMissingPass = false;
        loginErrMsg = null;

        if(tplUser === "") {
            openTemplate();
        } else {
            await loginAndOpen();
        }
    }

    function openTemplate() {
        goto(`./template/${tplName}`);
    }

    async function loginAndOpen() {
        if(tplPass === "") {
            toastMissingPass = true;
            return;
        }

        try {
            const err = await login(tplName, tplUser, tplPass);
            if(err == null) {
                openTemplate();
            } else {
                loginErrMsg = err?.message ?? "unknown error";
            }
        } catch(e: any) {
            console.error(e);
            const err = await parseHttpException(e);
            loginErrMsg = err?.message ?? "unknown error";
        }
    }
</script>

<form onsubmit={submit}>
    <MessageToast show={toastError}>
        {#snippet content()}
            Unable to login:
            {loginErrMsg}
        {/snippet}
    </MessageToast>

    <div class="flex flex-col">
        <div>
            <Label for="open_template" class="mb-2">Template ID</Label>
            <Input type="text" id="open_template" required bind:value={tplName} />
        </div>

        <div class="mt-4">
            <Label for="open_user" class="mb-2">
                <span>Username</span>
                <Button outline pill class="ml-2 p-2! w-fit"><Icon icon="mdi-light:information"/></Button>
                <Popover class="max-w-56">
                    Username and password are optional if you want to open the template in readonly mode.
                </Popover>
            </Label>
            <Input type="text" id="open_user" bind:value={tplUser} />
        </div>
        <div>
            <Label for="open_pass" class="mb-2">Password</Label>
            <Input type="password" id="open_pass" bind:value={tplPass} />
        </div>
        <Toast class="mt-2" bind:toastStatus={toastMissingPass}>
            Password is required
        </Toast>

        <button type="submit" class="mt-8">Open</button>
    </div>
</form>
