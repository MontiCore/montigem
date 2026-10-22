#!/usr/bin/env bash
# Minimal curl-only re-implementation of the "trigger and wait for a GitLab
# pipeline" GitHub Action:
#   https://github.com/digital-blueprint/gitlab-pipeline-trigger-action
#
# It only covers the core behaviour of that action (trigger a pipeline via a
# trigger token, then poll it until it reaches a final status). While polling,
# it also reports which jobs started running and which succeeded or failed,
# and prints a per-job summary at the end. Artifact / job-log downloading is
# intentionally left out to keep this a plain curl script; the only other
# dependency is jq (preinstalled on GitHub-hosted runners), used to parse the
# job list.
#
# Required environment variables:
#   GITLAB_HOST            GitLab hostname, e.g. "gitlab.example.com" (default: gitlab.com)
#   GITLAB_PROJECT_ID      Project ID or URL-encoded path
#   GITLAB_REF             Branch or tag to run the pipeline on
#   GITLAB_TRIGGER_TOKEN   Pipeline trigger token
#   GITLAB_ACCESS_TOKEN    Access token used to poll the pipeline status (needs read_api scope)
#
# Optional environment variables:
#   GITLAB_INPUTS_JSON     JSON object of pipeline inputs (spec:inputs), e.g. '{"umlp_ref":"dev"}' (default: {})
#   GITLAB_POLL_INTERVAL   Seconds between status polls (default: 15, matches the original action)
#   GITLAB_POLL_MAX_OUTAGE_MINUTES
#                          Minutes a failing status poll (network error, HTTP 429 or 5xx, e.g.
#                           during a GitLab maintenance) is retried before giving up (default: 60,
#                           the GitLab job timeout)
#   GITLAB_REQUEST_TIMEOUT Seconds after which a single API request is aborted (default: 30)
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
max_outage_minutes="${GITLAB_POLL_MAX_OUTAGE_MINUTES:-60}"
request_timeout="${GITLAB_REQUEST_TIMEOUT:-30}"

# Extracts the string/number value of a top-level JSON field without a JSON parser.
json_field() {
    local field="$1"
    grep -o "\"${field}\"[[:space:]]*:[[:space:]]*\(\"[^\"]*\"\|[0-9]*\)" \
        | head -1 \
        | sed -E 's/^"[^"]*"[[:space:]]*:[[:space:]]*"?([^"]*)"?$/\1/'
}

# Runs curl with the given arguments and prints the response body followed by a line with the
# HTTP status code. Never fails: without a response (network error, timeout) the code is 000.
curl_with_code() {
    local out
    out=$(curl -sS -w '\n%{http_code}' \
        --connect-timeout 10 \
        --max-time "$request_timeout" \
        "$@") || true
    case "${out##*$'\n'}" in
        [0-9][0-9][0-9])
            printf '%s' "$out"
            ;;
        *)
            printf '%s\n000' "$out"
            ;;
    esac
}

# GET request to the GitLab API, output as for curl_with_code.
gitlab_get() {
    curl_with_code \
        -H "PRIVATE-TOKEN: ${access_token}" \
        -H "Accept: application/json" \
        "$1"
}

if ! command -v jq >/dev/null 2>&1; then
    echo "jq is required to report the pipeline's jobs" >&2
    exit 1
fi

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

# Not retried: after a failed response the pipeline may have been created anyway.
trigger_response=$(curl_with_code -X POST \
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

# Last seen status, "stage/name", allow_failure flag and web URL of each job, keyed by job id
declare -A job_status=() job_label=() job_allow_failure=() job_url=()

# Prints the pipeline's jobs (regular jobs and bridge/trigger jobs) as
# tab-separated lines: id, status, stage, name, allow_failure, web_url.
# A failed request only produces a warning, as the job list is informational.
fetch_jobs() {
    local kind response code body
    for kind in jobs bridges; do
        response=$(gitlab_get "${status_url}/${kind}?per_page=100")
        code=$(printf '%s' "$response" | tail -n1)
        body=$(printf '%s' "$response" | sed '$d')
        if [[ "$code" -lt 200 || "$code" -ge 300 ]]; then
            echo "Warning: could not fetch pipeline ${kind} (HTTP ${code})" >&2
            continue
        fi
        printf '%s' "$body" \
            | jq -r '.[] | [.id, .status, .stage, .name, .allow_failure, .web_url] | @tsv' \
            || echo "Warning: could not parse pipeline ${kind}" >&2
    done
}

# Prints a line for every job whose status changed since the last poll.
report_job_changes() {
    local id st stage name allow_failure url label
    while IFS=$'\t' read -r id st stage name allow_failure url; do
        [[ -z "$id" || "${job_status[$id]:-}" == "$st" ]] && continue
        label="${stage}/${name}"
        job_status[$id]="$st"
        job_label[$id]="$label"
        job_allow_failure[$id]="$allow_failure"
        job_url[$id]="$url"

        case "$st" in
            running)  echo "  Job started:   ${label}" ;;
            success)  echo "  Job succeeded: ${label}" ;;
            failed)
                if [[ "$allow_failure" == "true" ]]; then
                    echo "  Job failed (allowed to fail): ${label} - ${url}"
                else
                    echo "  Job failed:    ${label} - ${url}"
                fi
                ;;
            canceled) echo "  Job canceled:  ${label}" ;;
            skipped)  echo "  Job skipped:   ${label}" ;;
            manual)   echo "  Job manual:    ${label} (waiting to be started manually)" ;;
            # created, pending, preparing, scheduled, waiting_for_resource: not worth reporting
        esac
    done < <(fetch_jobs)
}

echo "Polling pipeline status every ${poll_interval}s..."
# Start of the current series of failing polls (epoch seconds), empty while polls succeed,
# and the outage duration in minutes last reported
outage_since=""
reported_minutes=""
while true; do
    sleep "$poll_interval"

    poll_response=$(gitlab_get "$status_url")
    poll_status_code=$(printf '%s' "$poll_response" | tail -n1)
    poll_body=$(printf '%s' "$poll_response" | sed '$d')

    case "$poll_status_code" in
        2[0-9][0-9])
            outage_since=""
            reported_minutes=""
            ;;
        000|429|5[0-9][0-9])
            # GitLab unreachable, overloaded or in maintenance; the pipeline itself keeps running
            now=$(date +%s)
            outage_since="${outage_since:-$now}"
            if (( now - outage_since >= max_outage_minutes * 60 )); then
                echo "GitLab API unreachable for ${max_outage_minutes} min (last: HTTP ${poll_status_code});" \
                    "the pipeline may still be running: ${web_url}" >&2
                exit 1
            fi
            # Reported when the outage starts and then once per minute
            outage_minutes=$(( (now - outage_since) / 60 ))
            if [[ "$outage_minutes" != "$reported_minutes" ]]; then
                echo "Warning: GitLab API returned HTTP ${poll_status_code} while polling," \
                    "retrying (failing for ${outage_minutes} min)" >&2
                reported_minutes="$outage_minutes"
            fi
            continue
            ;;
        *)
            echo "GitLab API returned HTTP ${poll_status_code} while polling: ${poll_body}" >&2
            exit 1
            ;;
    esac

    status=$(printf '%s' "$poll_body" | json_field status)
    echo "Pipeline status: ${status}"

    # Fetched after the pipeline status, so the final poll also captures the jobs' final states
    report_job_changes

    case "$status" in
        success|failed|canceled|skipped)
            break
            ;;
    esac
done

if [[ ${#job_status[@]} -gt 0 ]]; then
    echo "Job summary:"
    for id in $(printf '%s\n' "${!job_status[@]}" | sort -n); do
        printf '  %-10s %s\n' "${job_status[$id]}" "${job_label[$id]}"
        # Annotate failed jobs in the GitHub Actions UI
        if [[ "${GITHUB_ACTIONS:-}" == "true" && "${job_status[$id]}" == "failed" \
            && "${job_allow_failure[$id]}" != "true" ]]; then
            echo "::error title=GitLab job failed::${job_label[$id]} failed: ${job_url[$id]}"
        fi
    done
fi

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
    {
        echo "status=${status}"
        echo "web_url=${web_url}"
    } >>"$GITHUB_OUTPUT"
fi

echo "status=${status}"
echo "web_url= ${web_url}"

[[ "$status" == "success" ]]
