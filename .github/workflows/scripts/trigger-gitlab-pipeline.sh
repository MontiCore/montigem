#!/usr/bin/env bash
# Minimal curl-only re-implementation of the "trigger and wait for a GitLab
# pipeline" GitHub Action:
#   https://github.com/digital-blueprint/gitlab-pipeline-trigger-action
#
# It only covers the core behaviour of that action (trigger a pipeline via a
# trigger token, then poll it until it reaches a final status). Artifact /
# job-log downloading is intentionally left out to keep this a plain,
# dependency-free curl script.
#
# Required environment variables:
#   GITLAB_HOST            GitLab hostname, e.g. "gitlab.example.com" (default: gitlab.com)
#   GITLAB_PROJECT_ID      Project ID or URL-encoded path
#   GITLAB_REF             Branch or tag to run the pipeline on
#   GITLAB_TRIGGER_TOKEN   Pipeline trigger token
#
# Optional environment variables:
#   GITLAB_ACCESS_TOKEN    Access token used to poll the pipeline status
#                           (needs read_api scope; not needed for public projects)
#   GITLAB_INPUTS_JSON     JSON object of pipeline inputs (spec:inputs), e.g. '{"umlp_ref":"dev"}' (default: {})
#   GITLAB_POLL_INTERVAL   Seconds between status polls (default: 15, matches the original action)
#
# Outputs:
#   Written as "status=..." / "web_url=..." to $GITHUB_OUTPUT if that
#   variable is set, and always printed to stdout.
#
# Exit code is 0 only if the pipeline finished with status "success".

set -euo pipefail

host="${GITLAB_HOST:-git.rwth-aachen.de}" # Default is RWTH GitLab
case "$host" in
    http://*|https://*) ;;
    *) host="https://${host}" ;;
esac

project_id="${GITLAB_PROJECT_ID:-129999}" # Default is montigem-pipeline project
ref="${GITLAB_REF:?GITLAB_REF is required}"
trigger_token="${GITLAB_TRIGGER_TOKEN:?GITLAB_TRIGGER_TOKEN is required}"
access_token="${GITLAB_ACCESS_TOKEN:?GITLAB_ACCESS_TOKEN is required}"
inputs_json="${GITLAB_INPUTS_JSON:-{}}"
poll_interval="${GITLAB_POLL_INTERVAL:-15}"

# Extracts the string/number value of a top-level JSON field without a JSON parser.
json_field() {
    local field="$1"
    grep -o "\"${field}\"[[:space:]]*:[[:space:]]*\(\"[^\"]*\"\|[0-9]*\)" \
        | head -1 \
        | sed -E 's/.*:[[:space:]]*"?([^"]*)"?$/\1/'
}

echo "Triggering GitLab pipeline for ${project_id} (${ref}) on ${host}..."

# GitLab's trigger API expects "inputs" as individual multipart form fields
# (inputs[KEY]=VALUE), not as a nested JSON object in the request body --
# see https://docs.gitlab.com/ee/api/pipeline_triggers.html#trigger-a-pipeline-with-a-token
# (sending it as JSON instead fails with "Invalid JSON format").
form_args=(--form "token=${trigger_token}" --form "ref=${ref}")
while IFS='=' read -r key value; do
    [[ -z "$key" ]] && continue
    form_args+=(--form "inputs[${key}]=${value}")
done < <(printf '%s' "$inputs_json" | grep -o '"[^"]*"[[:space:]]*:[[:space:]]*"[^"]*"' \
    | sed -E 's/^"([^"]*)"[[:space:]]*:[[:space:]]*"([^"]*)"$/\1=\2/')

trigger_response=$(curl -sS -w '\n%{http_code}' -X POST \
    "${form_args[@]}" \
    "${host}/api/v4/projects/${project_id}/trigger/pipeline")

trigger_status_code=$(printf '%s' "$trigger_response" | tail -n1)
trigger_body=$(printf '%s' "$trigger_response" | sed '$d')

if [[ "$trigger_status_code" -lt 200 || "$trigger_status_code" -ge 300 ]]; then
    echo "Failed to trigger pipeline (HTTP ${trigger_status_code}): ${trigger_body}" >&2
    exit 1
fi

pipeline_id=$(printf '%s' "$trigger_body" | json_field id)
web_url=$(printf '%s' "$trigger_body" | json_field web_url)

if [[ -z "$pipeline_id" ]]; then
    echo "Could not determine pipeline id from response: ${trigger_body}" >&2
    exit 1
fi

echo "Pipeline triggered: ${web_url} (id ${pipeline_id})"

status_url="${host}/api/v4/projects/${project_id}/pipelines/${pipeline_id}"
status="pending"

echo "Polling pipeline status every ${poll_interval}s..."
while true; do
    sleep "$poll_interval"

    poll_response=$(curl -sS -w '\n%{http_code}' \
        -H "PRIVATE-TOKEN: ${access_token}" \
        -H "Accept: application/json" \
        "$status_url")

    poll_status_code=$(printf '%s' "$poll_response" | tail -n1)
    poll_body=$(printf '%s' "$poll_response" | sed '$d')

    if [[ "$poll_status_code" -lt 200 || "$poll_status_code" -ge 300 ]]; then
        echo "GitLab API returned HTTP ${poll_status_code} while polling: ${poll_body}" >&2
        exit 1
    fi

    status=$(printf '%s' "$poll_body" | json_field status)
    echo "Pipeline status: ${status}"

    case "$status" in
        success|failed|canceled|skipped)
            break
            ;;
    esac
done

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
    {
        echo "status=${status}"
        echo "web_url=${web_url}"
    } >>"$GITHUB_OUTPUT"
fi

echo "status=${status}"
echo "web_url=${web_url}"

[[ "$status" == "success" ]]
