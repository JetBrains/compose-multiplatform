cd /androidx-main
~/bin/repo init -u https://android.googlesource.com/platform/manifest \
    -b androidx-main --depth=1 --partial-clone --clone-filter=blob:limit=10M
~/bin/repo sync -c --no-tags --no-clone-bundle -j4

mkdir -p /mnt/agent
ln -sf /androidx-main/tools/ /mnt/agent/tools
ln -sf /androidx-main/prebuilts/ /mnt/agent/prebuilts
ln -sf /androidx-main/external /mnt/agent/external
