#!/system/bin/sh
set -ex

echo "[vibestudio-bootstrap] Starting environment setup..."
export PATH="$PREFIX/bin:$PREFIX/bin/applets:/system/bin:$PATH"
export LD_LIBRARY_PATH="$PREFIX/lib:$LD_LIBRARY_PATH"
export TMPDIR="$PREFIX/tmp"
export TERM="xterm-256color"
export TERMUX_PKG_NO_MIRROR_SELECT="true"
export DPKG_ADMINDIR="$PREFIX/var/lib/dpkg"

echo "[vibestudio-bootstrap] PREFIX=$PREFIX"
echo "[vibestudio-bootstrap] HOME=$HOME"
echo "[vibestudio-bootstrap] PATH=$PATH"
echo "[vibestudio-bootstrap] LD_LIBRARY_PATH=$LD_LIBRARY_PATH"
echo "[vibestudio-bootstrap] DPKG_ADMINDIR=$DPKG_ADMINDIR"

chmod -R 755 "$PREFIX/bin" "$PREFIX/libexec" "$PREFIX/lib/apt/methods" 2>/dev/null || true

mkdir -p "$PREFIX/etc/dpkg/dpkg.cfg.d" "$PREFIX/var/lib/dpkg/updates" "$PREFIX/var/lib/dpkg/info" "$PREFIX/var/lib/dpkg/triggers" "$PREFIX/var/lib/dpkg/alternatives"
touch "$PREFIX/var/lib/dpkg/status" "$PREFIX/var/lib/dpkg/available"

# Patch pkg script if it contains hardcoded /data/data/com.termux/files/usr
if [ -x "$PREFIX/bin/pkg" ]; then
    sed -i "s|/data/data/com.termux/files/usr|$PREFIX|g" "$PREFIX/bin/pkg" 2>/dev/null || true
fi

echo "[vibestudio-bootstrap] Checking available package managers..."
# Link default mirror to chosen_mirrors
if [ -f "$PREFIX/etc/termux/mirrors/default" ]; then
    rm -f "$PREFIX/etc/termux/chosen_mirrors"
    ln -sf "$PREFIX/etc/termux/mirrors/default" "$PREFIX/etc/termux/chosen_mirrors"
fi

if [ -x "$PREFIX/bin/pkg" ]; then
    echo "[vibestudio-bootstrap] Found pkg at $PREFIX/bin/pkg"
    echo "[vibestudio-bootstrap] Running pkg update..."
    "$PREFIX/bin/pkg" update -y
    echo "[vibestudio-bootstrap] Installing ca-certificates termux-keyring..."
    "$PREFIX/bin/pkg" install -y ca-certificates termux-keyring
elif [ -x "$PREFIX/bin/apt-get" ]; then
    echo "[vibestudio-bootstrap] Found apt-get at $PREFIX/bin/apt-get"
    echo "[vibestudio-bootstrap] Running apt-get update..."
    "$PREFIX/bin/apt-get" update -y
    echo "[vibestudio-bootstrap] Installing ca-certificates termux-keyring..."
    "$PREFIX/bin/apt-get" install -y ca-certificates termux-keyring
else
    echo "[vibestudio-bootstrap] Warning: Neither pkg nor apt-get found at $PREFIX/bin"
    exit 1
fi

echo "[vibestudio-bootstrap] Environment setup completed!"
exit 0
