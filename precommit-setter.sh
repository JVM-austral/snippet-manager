#!/usr/bin/env bash
# File: scripts/install-pre-commit.sh
# Installs a pre-commit hook that runs ktlintCheck and tests

set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || true)"
if [ -z "${REPO_ROOT}" ]; then
  echo "Not inside a Git repository."
  exit 1
fi
cd "$REPO_ROOT"

HOOKS_DIR="$(git rev-parse --git-path hooks)"
mkdir -p "$HOOKS_DIR"
HOOK_FILE="$HOOKS_DIR/pre-commit"

cat > "$HOOK_FILE" << 'HOOK'
#!/usr/bin/env bash
set -euo pipefail

# Run ktlint check and tests before committing
if [ ! -x "./gradlew" ]; then
  echo "[pre-commit] Missing ./gradlew. Add Gradle wrapper."
  exit 1
fi

echo "[pre-commit] Running ktlintCheck..."
if ! ./gradlew -q --no-daemon ktlintCheck; then
  echo
  echo "[pre-commit] ktlintCheck failed. Fix issues or run: ./gradlew ktlintFormat"
  exit 1
fi

echo "[pre-commit] Running tests..."
if ! ./gradlew -q --no-daemon test; then
  echo
  echo "[pre-commit] Tests failed."
  exit 1
fi

echo "[pre-commit] All checks passed."
exit 0
HOOK

chmod +x "$HOOK_FILE"
echo "Installed pre-commit hook at: $HOOK_FILE"