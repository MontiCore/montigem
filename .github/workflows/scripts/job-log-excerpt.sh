#!/usr/bin/env bash
# Prints the part of a CI job log that usually explains why the job failed:
# failed Gradle tasks and tests ("FAILED"), assertion errors, Gradle's
# "What went wrong" block, browser console errors ("[BROWSER] ERROR") and
# lines starting with "Error:", each with a few lines of context.
#
# Used by trigger-gitlab-pipeline.sh for the traces of failed GitLab jobs; it
# can be run on any saved log as well:
#   job-log-excerpt.sh build.log
#   job-log-excerpt.sh < build.log
#
# The log is cleaned first: ANSI escape sequences, GitLab section markers and
# carriage returns are removed, and JSON Web Tokens are replaced by
# "<redacted-jwt>". Identical matching lines are shown at most
# EXCERPT_MAX_REPEATS times; if the excerpt is longer than EXCERPT_MAX_LINES,
# its beginning and end are kept (Gradle's summary is at the end).
#
# Optional environment variables:
#   EXCERPT_MAX_LINES    Maximum number of printed lines (default: 200)
#   EXCERPT_BEFORE       Context lines before a matching line (default: 2)
#   EXCERPT_AFTER        Context lines after a matching line (default: 6)
#   EXCERPT_MAX_REPEATS  How often an identical matching line is shown (default: 2)
#   EXCERPT_MAX_WIDTH    Lines are cut to this many characters (default: 300)
#
# Prints nothing if no line matches.

set -euo pipefail

max_lines="${EXCERPT_MAX_LINES:-200}"
before="${EXCERPT_BEFORE:-2}"
after="${EXCERPT_AFTER:-6}"
max_repeats="${EXCERPT_MAX_REPEATS:-2}"
max_width="${EXCERPT_MAX_WIDTH:-300}"

pattern='FAILED|What went wrong|AssertionFailedError|AssertionError|\[BROWSER[^]]*\] ERROR|(^|[[:space:]])Error:'

# Removes terminal control sequences and GitLab's collapsible-section markers
# ("section_start:<time>:<name>[<options>]\r\e[0K"), keeps only the text after
# the last carriage return of a line, redacts JWTs.
clean() {
    sed -E \
        -e $'s/\x1b\\[[0-9;?]*[A-Za-z]//g' \
        -e 's/section_(start|end):[0-9]+:[A-Za-z0-9_.-]+(\[[^]]*\])?//g' \
        -e $'s/\r$//' \
        -e $'s/^.*\r//' \
        -e 's/eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]*/<redacted-jwt>/g'
}

# Prints matching lines with context as "<line number>: <text>", blocks that are
# not adjacent separated by "--".
select_lines() {
    # The pattern is passed via the environment, as awk -v would interpret its backslashes
    EXCERPT_PATTERN="$pattern" awk -v before="$before" -v after="$after" \
        -v max_repeats="$max_repeats" -v max_width="$max_width" '
        BEGIN { pattern = ENVIRON["EXCERPT_PATTERN"] }
        function emit(n, text) {
            if (last_printed > 0 && n > last_printed + 1) print "--"
            print n ": " substr(text, 1, max_width)
            last_printed = n
        }
        {
            buf[NR % (before + 1)] = $0
            is_match = ($0 ~ pattern)
            if (is_match) {
                key = $0
                sub(/^[[:space:]]+/, "", key)
                if (++seen[key] > max_repeats) is_match = 0
            }
            if (is_match) {
                start = NR - before
                if (start <= last_printed) start = last_printed + 1
                if (start < 1) start = 1
                for (n = start; n < NR; n++) emit(n, buf[n % (before + 1)])
                emit(NR, $0)
                remaining = after
            } else if (remaining > 0) {
                emit(NR, $0)
                remaining--
            }
        }'
}

# Keeps the first and the last lines if there are more than max_lines.
cap() {
    awk -v max="$max_lines" '
        { line[NR] = $0 }
        END {
            if (NR <= max) { for (i = 1; i <= NR; i++) print line[i]; exit }
            head = int(max * 3 / 5); tail = max - head
            for (i = 1; i <= head; i++) print line[i]
            print "... (" NR - head - tail " lines of the excerpt omitted) ..."
            for (i = NR - tail + 1; i <= NR; i++) print line[i]
        }'
}

if [[ $# -gt 0 ]]; then
    clean <"$1" | select_lines | cap
else
    clean | select_lines | cap
fi
