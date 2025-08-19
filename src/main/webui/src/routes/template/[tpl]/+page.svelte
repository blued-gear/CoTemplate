<script lang="ts">
    import {Button, Drawer, Popover, Toast, Tooltip} from "flowbite-svelte";
    import {sineIn} from "svelte/easing";
    import type {PageData} from "./+page.ts";
    import {API, API_PATH, STORAGE_SELECTED_ITEMS} from "$lib/js/constants";
    import IconButton from "$lib/components/IconButton.svelte";
    import {TeamCreatePolicy, type TemplateItemDto} from "$lib/js/api";
    import {POWER_EDIT_TPL, POWER_VIEW, ROLE_GUEST} from "$lib/js/api-ext/roles";
    import {logout} from "$lib/js/api-ext/auth";
    import type {ImgProperties} from "$lib/js/types";
    import ImgEditDlg from "$lib/components/ImgEditDlg.svelte";
    import Icon from "@iconify/svelte";
    import {parseHttpException} from "$lib/js/api-ext/errors";
    import TemplateSettings from "$lib/components/TemplateSettings.svelte";
    import {invalidateAll} from "$app/navigation";
    import TeamAddDlg from "$lib/components/TeamAddDlg.svelte";

    const drawerTransitionRight = {
        x: 320,
        duration: 200,
        easing: sineIn
    };

    type ImageItem = TemplateItemDto & { selected: boolean };

    interface Props {
        data: PageData;
    }
    const { data }: Props = $props();

    let errMsg: string | null = $state(null);
    let imgUrl = $state("");
    let imgDrawerHidden = $state(true);
    let settingsDrawerHidden = $state(true);
    let images: ImageItem[] = $state([]);
    let showImgEditDlg = $state(false);
    let showImgAddDlg = $state(false);
    let showTeamAddDlg = $state(false);
    let editingImg: ImgProperties | null = $state(null);
    let tplSettingsSizeW = $derived(data.tplInfo.width!);
    let tplSettingsSizeH = $derived(data.tplInfo.height!);
    let tplSettingsTCP = $derived(data.tplInfo.teamCreatePolicy);

    function computeImgUrl(): string {
        const selected: string[] = getSelectedItems();
        let selectedArg: string;
        if(selected.length > 0)
            selectedArg = selected.join(",");
        else
            selectedArg = "all";
        return `${API_PATH}/templates/${data.tplId}/template?images=${selectedArg}#${Date.now()}`;// add fragment to force reload
    }

    function getSelectedItems(): string[] {
        const storedStr: string | null = localStorage.getItem(STORAGE_SELECTED_ITEMS);
        if(storedStr == null)
            return [];
        const stored: Record<string, string[]> = JSON.parse(storedStr);
        return stored[data.tplId] ?? [];
    }

    function setSelectedItems(ids: string[]) {
        const storedStr: string = localStorage.getItem(STORAGE_SELECTED_ITEMS) ?? "{}";
        const stored: Record<string, string[]> = JSON.parse(storedStr);
        stored[data.tplId] = ids;
        localStorage.setItem(STORAGE_SELECTED_ITEMS, JSON.stringify(stored));
    }

    function toggleSelectedImage(img: ImageItem) {
        const selected: string[] = getSelectedItems();
        const idx = selected.indexOf(img.id);
        if(idx === -1)
            selected.push(img.id);
        else
            selected.splice(idx, 1);

        setSelectedItems(selected);

        img.selected = !img.selected;
    }

    async function loadImageInfo() {
        try {
            const resp = await API.getTemplateItems(data.tplId);
            const selected: string[] = getSelectedItems();
            images = resp.items.map((img) => {
                const sel = $state(selected.includes(img.id));
                return {
                    ...img,
                    selected: sel,
                };
            }).sort((a, b) => {
                const ownerOrder = a.team.localeCompare(b.team);
                if(ownerOrder !== 0) return ownerOrder;
                return a.description.localeCompare(b.description);
            });
        } catch(e) {
            const err = await parseHttpException(e);
            if(err != null) {
                console.error("unable to fetch images", err);
                errMsg = `unable to fetch images: ${err.message}`;
            } else {
                console.error("unable to fetch images", e);
                errMsg = `unable to fetch images`;
            }
        }
    }

    async function onAddImg(img: ImgProperties) {
        try {
            await API.addTemplateItem(data.tplId, img.description, img.imgData!, img.x, img.y, img.z);
        } catch(e) {
            const err = await parseHttpException(e);
            if(err != null) {
                console.error("unable to add image", err);
                errMsg = `unable to add image: ${err.message}`;
            } else {
                console.error("unable to add image", e);
                errMsg = `unable to add image`;
            }
        }

        await reload();
    }

    function editImg(e: MouseEvent, img: ImageItem) {
        e.stopPropagation();// prevent toggling selection

        editingImg = {
            id: img.id,
            description: img.description,
            x: img.x!,
            y: img.y!,
            z: img.z!,
            imgData: null,
        };
        showImgEditDlg = true;
    }

    async function onEditedImg(img: ImgProperties) {
        try {
            if(img.imgData != null) {
                await API.updateTemplateItemImage(img.id!, data.tplId, img.imgData);
            }

            await API.updateTemplateItemDetails(img.id!, data.tplId, {
                description: img.description,
                x: img.x,
                y: img.y,
                z: img.z,
            });
        } catch(e) {
            const err = await parseHttpException(e);
            if(err != null) {
                console.error("unable to add image", err);
                errMsg = `unable to add image: ${err.message}`;
            } else {
                console.error("unable to add image", e);
                errMsg = `unable to add image`;
            }
        }

        await reload();
    }

    async function onApplySettings() {
        errMsg = null;

        if(tplSettingsSizeW !== data.tplInfo.width || tplSettingsSizeH !== data.tplInfo.height) {
            try {
                await API.updateTemplateSize(data.tplId, { width: tplSettingsSizeW, height: tplSettingsSizeH });
            } catch(e) {
                const err = await parseHttpException(e);
                if(err != null) {
                    console.error("unable to update tpl size", err);
                    errMsg = `unable to update size: ${err.message}`;
                } else {
                    console.error("unable to update tpl size", e);
                    errMsg = `unable to update size`;
                }
                return;
            }
        }

        if(tplSettingsTCP !== data.tplInfo.teamCreatePolicy) {
            try {
                await API.updateTemplateTeamCreatePolicy(data.tplId, { policy: tplSettingsTCP });
            } catch(e) {
                const err = await parseHttpException(e);
                if(err != null) {
                    console.error("unable to update tpl TCP", err);
                    errMsg = `unable to update team policy: ${err.message}`;
                } else {
                    console.error("unable to update tpl TCP", e);
                    errMsg = `unable to update team policy`;
                }
                return;
            }
        }

        await invalidateAll();
    }

    async function onLogout() {
        const err = await logout();
        if(err != null) {
            console.error("error when logging out", err);
        }

        location.reload();
    }

    async function reload() {
        imgUrl = computeImgUrl();
        await loadImageInfo();
    }
    reload();
</script>

<div>
    <div class="p-2 flex gap-3 shadow">
        <IconButton icon="mdi:image-multiple-outline" onClick={() => imgDrawerHidden = false} />

        <div class="flex-1 flex justify-center items-center gap-1">
            <span>{data.tplInfo.name}</span>
            <IconButton small icon="mdi:reload" classO="h-fit" onClick={reload} />
        </div>

        {#if data.userRole !== ROLE_GUEST}
        <div>
            {data.teamName}
            <IconButton icon="mdi:logout" onClick={onLogout} />
            <Tooltip>Logout</Tooltip>
        </div>
        {/if}

        <IconButton icon="mdi:cog" onClick={() => settingsDrawerHidden = false} />
    </div>

    <div>
        <img class="w-full h-full contain-content" src="{imgUrl}" alt="template" />
    </div>

    <Drawer bind:hidden={imgDrawerHidden} class="overflow-y-auto">
        {#if data.userRole !== ROLE_GUEST}
        <Button class="w-full" onclick={() => showImgAddDlg = true}>Add Image</Button>
        {/if}

        {#each images as img}
            <div class={{"mt-4": true, "p-1": true, "border": true, "border-green-400": img.selected, "border-gray-600": !img.selected}} onclick={() => toggleSelectedImage(img)}>
                <div class="flex flex-row justify-between">
                    <div>{img.team}</div>
                    {#if img.team === data.teamName}
                        <IconButton icon="mdi:pencil" onClick={(e: MouseEvent) => editImg(e, img)}/>
                    {/if}
                </div>
                <img class="max-w-full max-h-20 contain-content" src={`${API_PATH}/templates/${data.tplId}/items/${img.id}/image`} alt="{img.description}" />
                <div class="w-full max-h-20 overflow-hidden">{img.description}</div>
                <Popover>{img.description}</Popover>
            </div>
        {/each}
    </Drawer>

    <Drawer placement="right" transitionParams={drawerTransitionRight} bind:hidden={settingsDrawerHidden}>
        <div class="flex flex-col gap-6">
            <!-- TODO show when tpl was created and how much time left until deletion -->

            <Button disabled={data.userPower < (data.tplInfo.teamCreatePolicy === TeamCreatePolicy.Everyone ? POWER_VIEW : POWER_EDIT_TPL)} onclick={() => showTeamAddDlg = true}>Create Team</Button>

            <div class="p-2 flex flex-col gap-4 border rounded-sm">
                <div>Template Settings</div>
                <TemplateSettings bind:sizeW={tplSettingsSizeW} bind:sizeH={tplSettingsSizeH} bind:teamCreatePolicy={tplSettingsTCP} />
                <Button disabled={data.userPower < POWER_EDIT_TPL} onclick={onApplySettings}>Apply</Button>
            </div>
        </div>
    </Drawer>

    <ImgEditDlg bind:open={showImgAddDlg} create onSubmit={onAddImg} />
    <ImgEditDlg bind:open={showImgEditDlg} create={false} initialData={editingImg} onSubmit={onEditedImg} />
    <TeamAddDlg bind:open={showTeamAddDlg} tplId={data.tplId} />

    <Toast toastStatus={errMsg != null} color="red" class="absolute mb-4 ml-4 z-50">
        {#snippet icon()}
            <Icon class="h-5 w-5" icon="mdi:alert-circle-outline" />
            <span class="sr-only">Warning icon</span>
        {/snippet}
        {errMsg}
    </Toast>
</div>
