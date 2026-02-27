#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  scripts/mvn-bypass-nexus.sh [maven-args...]

Examples:
  scripts/mvn-bypass-nexus.sh clean package -DskipTests
  scripts/mvn-bypass-nexus.sh -Pnative-linux -DskipTests package

Notes:
  - If no arguments are provided, defaults to: clean package
  - This script is for LOCAL emergency use when Nexus/proxy is down.
  - It forces Maven to resolve dependencies/plugins directly from Maven Central.
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

for arg in "$@"; do
  if [[ "$arg" == "-s" || "$arg" == "--settings" ]]; then
    echo "Error: do not pass -s/--settings. This script manages settings automatically." >&2
    exit 2
  fi
done

if [[ $# -eq 0 ]]; then
  set -- clean package
fi

tmp_settings="$(mktemp -t levain-central-settings-XXXXXX.xml)"
cleanup() {
  rm -f "$tmp_settings"
}
trap cleanup EXIT

cat > "$tmp_settings" <<'EOF'
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <mirrors>
    <mirror>
      <id>force-central</id>
      <name>Force all repositories to Maven Central</name>
      <url>https://repo.maven.apache.org/maven2</url>
      <mirrorOf>*</mirrorOf>
    </mirror>
  </mirrors>

  <profiles>
    <profile>
      <id>direct-central</id>
      <repositories>
        <repository>
          <id>central</id>
          <name>Maven Central</name>
          <url>https://repo.maven.apache.org/maven2</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>false</enabled></snapshots>
        </repository>
      </repositories>
      <pluginRepositories>
        <pluginRepository>
          <id>central</id>
          <name>Maven Central Plugins</name>
          <url>https://repo.maven.apache.org/maven2</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>false</enabled></snapshots>
        </pluginRepository>
      </pluginRepositories>
    </profile>
  </profiles>

  <activeProfiles>
    <activeProfile>direct-central</activeProfile>
  </activeProfiles>
</settings>
EOF

echo "Running Maven with temporary direct-central settings (bypassing Nexus)..."
echo "Command: mvn -B -s $tmp_settings $*"

mvn -B -s "$tmp_settings" "$@"
