#!/usr/bin/env bash
# Minimal curl-only re-implementation of the "trigger and wait for a GitLab
# pipeline" GitHub Action:
#   https://github.com/digital-blueprint/gitlab-pipeline-trigger-action
#
# It only covers the core behaviour of that action (trigger a pipeline via a
# trigger token, then poll it until it reaches a final status). While polling,
# it also reports which jobs started running and which succeeded or failed,
# and prints a per-job summary at the end. For every failed job, it downloads
# the job log and prints an excerpt (see job-log-excerpt.sh next to this
# script) in a collapsible group, and lists the failed jobs with the most
# relevant lines of their logs in the GitHub step summary. Artifact
# downloading is left out to keep this a plain curl script; the only other
# dependency is jq (preinstalled on GitHub-hosted runners), used to parse the
# job list.
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
#   GITLAB_FAILED_JOB_LOGS "true" to print log excerpts of failed jobs, anything else
#                           to report only their status (default: false)
#   EXCERPT_MAX_LINES      Maximum length of a log excerpt in lines (default: 200),
#                           see job-log-excerpt.sh for further options
#
# Outputs:
#   Written as "status=..." / "web_url=..." to $GITHUB_OUTPUT if that
#   variable is set, and always printed to stdout. Failed jobs are listed in
#   $GITHUB_STEP_SUMMARY if that variable is set.
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
report_job_logs="${GITLAB_FAILED_JOB_LOGS:-false}"
excerpt_script="$(dirname "${BASH_SOURCE[0]}")/job-log-excerpt.sh"

# Extracts the string/number value of a top-level JSON field without a JSON parser.
json_field() {
    local field="$1"
    grep -o "\"${field}\"[[:space:]]*:[[:space:]]*\(\"[^\"]*\"\|[0-9]*\)" \
        | head -1 \
        | sed -E 's/^"[^"]*"[[:space:]]*:[[:space:]]*"?([^"]*)"?$/\1/'
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

# Last seen status, "stage/name", allow_failure flag and web URL of each job, keyed by job id
declare -A job_status=() job_label=() job_allow_failure=() job_url=()

# Prints the pipeline's jobs (regular jobs and bridge/trigger jobs) as
# tab-separated lines: id, status, stage, name, allow_failure, web_url, kind
# ("jobs" or "bridges").
# A failed request only produces a warning, as the job list is informational.
fetch_jobs() {
    local kind response code body
    for kind in jobs bridges; do
        response=$(curl -sS -w '\n%{http_code}' \
            -H "PRIVATE-TOKEN: ${access_token}" \
            -H "Accept: application/json" \
            "${status_url}/${kind}?per_page=100") || continue
        code=$(printf '%s' "$response" | tail -n1)
        body=$(printf '%s' "$response" | sed '$d')
        if [[ "$code" -lt 200 || "$code" -ge 300 ]]; then
            echo "Warning: could not fetch pipeline ${kind} (HTTP ${code})" >&2
            continue
        fi
        printf '%s' "$body" \
            | jq -r --arg kind "$kind" \
                '.[] | [.id, .status, .stage, .name, .allow_failure, .web_url, $kind] | @tsv' \
            || echo "Warning: could not parse pipeline ${kind}" >&2
    done
}

# Markdown for $GITHUB_STEP_SUMMARY, one section per failed job
failed_jobs_summary=""

# Lines of a log excerpt that are also shown in the step summary
summary_pattern='FAILED|AssertionFailedError|AssertionError|^[0-9]+: +Error:|Execution failed|^[0-9]+: > '
summary_exclude_pattern='\[BROWSER|^[0-9]+: > (Run with|Get more help)'

# Downloads the log of a failed job and prints an excerpt of it in a
# collapsible group; adds the job and the excerpt's most relevant lines to
# failed_jobs_summary. A failed download only produces a warning.
report_failed_job_log() {
    local id="$1" label="$2" url="$3" trace excerpt headlines token=""
    trace=$(mktemp)
    if ! curl -sS --fail --max-time 300 \
        -H "PRIVATE-TOKEN: ${access_token}" \
        -o "$trace" \
        "${host}/api/v4/projects/${project_id}/jobs/${id}/trace" </dev/null; then
        echo "Warning: could not fetch the log of job ${label} (${url})" >&2
        rm -f "$trace"
        failed_jobs_summary+=$'\n'"### ${label}"$'\n\n'"[Job log](${url}) (could not be fetched)"$'\n'
        return 0
    fi
    if ! excerpt=$("$excerpt_script" "$trace"); then
        echo "Warning: could not extract an excerpt from the log of job ${label}" >&2
        excerpt=""
    fi
    rm -f "$trace"

    if [[ "${GITHUB_ACTIONS:-}" == "true" ]]; then
        echo "::group::Log excerpt of failed job ${label}"
        # Lines of the log must not be interpreted as workflow commands (::add-mask::, ::endgroup::, ...)
        token=$(od -An -N16 -tx1 /dev/urandom | tr -d ' \n')
        echo "::stop-commands::${token}"
    else
        echo "----- Log excerpt of failed job ${label} -----"
    fi
    echo "Full log: ${url}"
    if [[ -n "$excerpt" ]]; then
        printf '%s\n' "$excerpt"
    else
        echo "(no line of the log matched, see the full log)"
    fi
    if [[ -n "$token" ]]; then
        echo "::${token}::"
        echo "::endgroup::"
    else
        echo "----- End of log excerpt of failed job ${label} -----"
    fi

    # Breaks up ``` in the log, which would end the Markdown code block
    # shellcheck disable=SC2016
    headlines=$(printf '%s\n' "$excerpt" | grep -E "$summary_pattern" \
        | grep -v -E "$summary_exclude_pattern" | head -n 20 | sed 's/```/` ` `/g' || true)
    failed_jobs_summary+=$'\n'"### ${label}"$'\n\n'"[Job log](${url})"$'\n'
    if [[ -n "$headlines" ]]; then
        failed_jobs_summary+=$'\n```text\n'"${headlines}"$'\n```\n'
    fi
}

# Prints a line for every job whose status changed since the last poll.
report_job_changes() {
    local id st stage name allow_failure url kind label
    while IFS=$'\t' read -r id st stage name allow_failure url kind; do
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
                # Bridge (trigger) jobs have no log of their own
                if [[ "$report_job_logs" == "true" && "$kind" == "jobs" ]]; then
                    report_failed_job_log "$id" "$label" "$url"
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

if [[ -n "${GITHUB_STEP_SUMMARY:-}" && -n "$failed_jobs_summary" ]]; then
    {
        echo "## Failed GitLab jobs"
        echo
        echo "Pipeline: ${web_url} (status: ${status})"
        printf '%s\n' "$failed_jobs_summary"
    } >>"$GITHUB_STEP_SUMMARY"
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
