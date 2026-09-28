#!/usr/bin/env bash
#
# Signs a release APK with the current key and the proof of rotation from the original 2009 key
# (signing/lineage), so a device with an APK signed by the old key accepts it as an update.
#
# Usage:
#   sign-apk.sh sign <unsigned.apk> <signed.apk>
#   sign-apk.sh check-lineage
#
# The APK is signed with APK Signature Scheme v3 only. The app's minSdk is 28 (Android 9), the
# first version that recognizes a rotated key, so the original key is never needed.
#
# The key comes from the environment:
#   KEYSTORE_FILE, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD
#   LINEAGE_FILE     lineage to use (default: lineage next to this script)
#   APKSIGNER        path to apksigner (default: found in the Android SDK)
#
# Keep key and lineage paths free of spaces: apksigner.bat on Windows can't handle them.

set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
lineage="${LINEAGE_FILE:-$here/lineage}"

# The app's minSdk, which is also the first API level that should use the rotated key.
min_sdk=28

# SHA-256 of the original certificate, which is always the first signer in the lineage. The
# override is for tests that use throwaway keys.
original_digest="${ORIGINAL_DIGEST:-d42189b36569f69acf5559931d3d3fc527632f74deceb9df6142aae339b0d131}"

die() {
    echo "error: $*" >&2
    exit 1
}

# apksigner.bat needs Windows paths when this runs under Git Bash.
native() {
    if command -v cygpath >/dev/null 2>&1; then
        cygpath -w "$1"
    else
        printf '%s' "$1"
    fi
}

find_apksigner() {
    if [ -n "${APKSIGNER:-}" ]; then
        printf '%s' "$APKSIGNER"
        return
    fi

    if command -v apksigner >/dev/null 2>&1; then
        command -v apksigner
        return
    fi

    local sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}" newest name
    [ -n "$sdk" ] || die "apksigner not found; set APKSIGNER or ANDROID_HOME"

    newest="$(ls -d "$sdk"/build-tools/*/ 2>/dev/null | sort -V | tail -n 1)"
    for name in apksigner apksigner.bat; do
        if [ -f "${newest}${name}" ]; then
            printf '%s' "${newest}${name}"
            return
        fi
    done

    die "no apksigner in $sdk/build-tools"
}

apksigner="$(find_apksigner)"

# Scratch directory for signing, removed on exit.
work=""
trap '[ -z "$work" ] || rm -rf "$work"' EXIT

# The SHA-256 digest of each certificate in the lineage, oldest first.
lineage_digests() {
    "$apksigner" lineage --in "$(native "$lineage")" --print-certs |
        sed -n 's/.*lineage certificate SHA-256 digest: *//p' | tr -d '\r'
}

check_lineage() {
    [ -f "$lineage" ] || die "lineage file not found: $lineage"

    local digests
    mapfile -t digests < <(lineage_digests)

    [ "${#digests[@]}" -ge 2 ] || die "the lineage has ${#digests[@]} signer(s); it needs at least 2"
    [ "${digests[0]}" = "$original_digest" ] ||
        die "the lineage doesn't start with the original certificate (found ${digests[0]})"

    echo "lineage OK: ${#digests[@]} signers, newest certificate ${digests[${#digests[@]} - 1]}"
}

sign_apk() {
    [ $# -eq 2 ] || die "usage: sign-apk.sh sign <unsigned.apk> <signed.apk>"
    local input="$1" output="$2"
    [ -f "$input" ] || die "no such APK: $input"

    : "${KEYSTORE_FILE:?KEYSTORE_FILE is not set}" "${KEYSTORE_PASSWORD:?KEYSTORE_PASSWORD is not set}"
    : "${KEY_ALIAS:?KEY_ALIAS is not set}" "${KEY_PASSWORD:?KEY_PASSWORD is not set}"

    check_lineage >/dev/null

    local newest
    newest="$(lineage_digests | tail -n 1)"

    # Sign a copy in a scratch directory: apksigner leaves a partial file behind when it fails,
    # and its Windows launcher can't take the spaces that repository paths may contain.
    work="$(mktemp -d)"
    cp "$input" "$work/in.apk"

    echo "signing $(basename "$input"): newest key + lineage, v3 only"

    "$apksigner" sign \
        --ks "$(native "$KEYSTORE_FILE")" --ks-pass env:KEYSTORE_PASSWORD \
        --ks-key-alias "$KEY_ALIAS" --key-pass env:KEY_PASSWORD \
        --lineage "$(native "$lineage")" --rotation-min-sdk-version "$min_sdk" \
        --v1-signing-enabled false --v2-signing-enabled false --v4-signing-enabled false \
        --out "$(native "$work/out.apk")" "$(native "$work/in.apk")" ||
        die "apksigner failed for $input"

    local report
    report="$("$apksigner" verify --min-sdk-version "$min_sdk" --print-certs "$(native "$work/out.apk")")" ||
        die "the signed APK doesn't verify"

    grep -qi "certificate SHA-256 digest: $newest" <<<"$report" ||
        die "the signed APK isn't signed by the newest key in the lineage ($newest)"

    mkdir -p "$(dirname "$output")"
    cp "$work/out.apk" "$output"
    echo "signed $output (signer $newest)"
}

case "${1:-}" in
    sign)
        shift
        sign_apk "$@"
        ;;
    check-lineage)
        check_lineage
        ;;
    *)
        sed -n '2,/^set -euo/p' "$0" | sed '$d' | sed 's/^# \{0,1\}//' >&2
        exit 2
        ;;
esac
