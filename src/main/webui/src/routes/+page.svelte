<script lang="ts">
    import {TabItem, Tabs} from "flowbite-svelte";
    import OpenTemplate from "$lib/components/OpenTemplate.svelte";
    import CreateTemplate from "$lib/components/CreateTemplate.svelte";
</script>

<div class="flex p-4 w-screen h-screen justify-center items-center">
    <div class="w-xl max-w-5/6 h-max p-5 flex flex-col justify-center items-center border rounded-xs">
        <Tabs tabStyle="underline">
            <TabItem title="Open" open>
                <OpenTemplate />
            </TabItem>
            <TabItem title="Create">
                <CreateTemplate />
            </TabItem>
        </Tabs>
    </div>
</div>
