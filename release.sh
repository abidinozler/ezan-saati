#!/usr/bin/env bash
# Yeni sürüm yayınlar: derler, test eder, kaynağı GitHub'a gönderir ve APK'yı release olarak yükler.
# Telefondaki Obtainium bu release'leri izleyip güncellemeyi bildirir.
# Kullanım: GITHUB_TOKEN=... ./release.sh "Bu sürümde neler var"
set -euo pipefail

OWNER=abidinozler
REPO=ezan-saati
NOTES="${1:-Yeni sürüm}"
: "${GITHUB_TOKEN:?GITHUB_TOKEN gerekli}"

cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-/c/Program Files/Android/Android Studio/jbr}"
./gradlew assembleDebug testDebugUnitTest -q

APK="$LOCALAPPDATA/EzanSaatiBuild/app/outputs/apk/debug/app-debug.apk"
BUILD_TOOLS=$(ls -d "$LOCALAPPDATA/Android/Sdk/build-tools/"* | tail -1)
VERSION=$("$BUILD_TOOLS/aapt2.exe" dump badging "$APK" | sed -n "s/.*versionName='\([^']*\)'.*/\1/p")
TAG="v$VERSION"
cp "$APK" EzanSaati.apk
echo "Sürüm: $VERSION"

API=https://api.github.com/repos/$OWNER/$REPO
AUTH=(-H "Authorization: Bearer $GITHUB_TOKEN" -H "Accept: application/vnd.github+json")

# Kaynak kodu gönder (token yalnızca bu komut için kullanılır, git ayarına yazılmaz)
git add -A
git commit -q -m "Sürüm $VERSION" -m "$NOTES" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" || true
git push -q "https://x-access-token:$GITHUB_TOKEN@github.com/$OWNER/$REPO.git" HEAD:main

BODY=$(TAG="$TAG" VERSION="$VERSION" NOTES="$NOTES" node -e '
    const e = process.env
    console.log(JSON.stringify({ tag_name: e.TAG, target_commitish: "main", name: e.VERSION, body: e.NOTES }))')
RELEASE_ID=$(curl -fsS "${AUTH[@]}" -X POST "$API/releases" -d "$BODY" |
    node -e 'let s = ""; process.stdin.on("data", d => s += d).on("end", () => console.log(JSON.parse(s).id))')

curl -fsS "${AUTH[@]}" -H "Content-Type: application/vnd.android.package-archive" \
    --data-binary @EzanSaati.apk \
    "https://uploads.github.com/repos/$OWNER/$REPO/releases/$RELEASE_ID/assets?name=EzanSaati-$VERSION.apk" \
    > /dev/null

echo "Yayınlandı: https://github.com/$OWNER/$REPO/releases/tag/$TAG"
