<script lang="ts">
    import type {TeamCreatePolicy} from "$lib/js/api";
    import {TeamCreatePolicy as TeamCreatePolicyVals} from "$lib/js/api";
    import {Input, Label} from "flowbite-svelte";

    interface Props {
        sizeW: number;
        sizeH: number;
        teamCreatePolicy: TeamCreatePolicy;
    }
    let { sizeW = $bindable(), sizeH = $bindable(), teamCreatePolicy = $bindable() }: Props = $props();

    let tcpEveryone = $derived(teamCreatePolicy === TeamCreatePolicyVals.Everyone);
    let tcpOwner = $derived(teamCreatePolicy === TeamCreatePolicyVals.Owner);

    function setTcpEveryone() {
        teamCreatePolicy = TeamCreatePolicyVals.Everyone;
    }

    function setTcpOwner() {
        teamCreatePolicy = TeamCreatePolicyVals.Owner;
    }
</script>

<div class="flex flex-col gap-2">
    <div class="flex flex-row gap-2">
        <div>
            <Label for="tpl_props_w" class="mb-2">Width</Label>
            <Input type="number" id="tpl_props_w" required min="1" max="8192" bind:value={sizeW} />
        </div>
        <div>
            <Label for="tpl_props_h" class="mb-2">Height</Label>
            <Input type="number" id="tpl_props_h" required min="1" max="8192" bind:value={sizeH} />
        </div>
    </div>

    <span>Who can create new teams:</span>
    <div class="flex flex-row gap-2">
        <Label>
            <input type="radio" name="tcp" checked={tcpEveryone} onclick={setTcpEveryone} />
            Everyone
        </Label>
        <Label>
            <input type="radio" name="tcp" checked={tcpOwner} onclick={setTcpOwner} />
            Owner
        </Label>
    </div>
</div>
