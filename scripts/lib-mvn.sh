# shellcheck shell=bash
# Shared helpers for mvn-test.sh and mvn-verify.sh (sourced, not executed directly).
#
# Behaviour:
#   - Runs ./mvnw in backend/ and redirects the full output to backend/target/mvn-last.log
#   - On success, prints a short summary; on failure, prints a compact error report
#   - Colors and clickable file paths are enabled only when stdout is a terminal, so
#     captured output (for example in CI) stays plain and small.
#     Disable with NO_COLOR=1, force with FORCE_COLOR=1.
#
# Tuning for the compact error report (environment variables):
#   MVN_MAX_LINE (240)   max length of a regular line
#   MVN_MAX_CAUSE (120)  max length of a wrapping "Caused by:" line
#   MVN_MAX_ROOT (300)   max length of the root-cause "Caused by:" line
#   MVN_MAX_FRAMES (8)   max stack frames printed per failed test

cd "$(dirname "${BASH_SOURCE[0]}")/../backend" || exit 1
ROOT="$(pwd)"

if [ ! -x ./mvnw ]; then
  echo "mvnw not found at $ROOT/mvnw (or not executable: run chmod +x mvnw)"
  exit 1
fi

# Interactive mode (colors + clickable links) only when printing to a terminal.
# Disable: NO_COLOR=1. Force: FORCE_COLOR=1
if { [ -t 1 ] || [ -n "$FORCE_COLOR" ]; } && [ -z "$NO_COLOR" ]; then
  INTERACTIVE=1
  R=$'\033[31m'; G=$'\033[32m'; Y=$'\033[33m'; D=$'\033[2m'; B=$'\033[1m'; N=$'\033[0m'
else
  INTERACTIVE=""
  R=''; G=''; Y=''; D=''; B=''; N=''
fi

# A line is colored by the first rule it matches; later rules are skipped (-e t)
colorize() {
  sed -E \
    -e "s/^(.*<<< (FAILURE|ERROR)!.*)\$/${B}${R}\1${N}/" -e t \
    -e "s/^(BUILD FAILED.*)\$/${B}${R}\1${N}/" -e t \
    -e "s/^(\[ERROR\].*)\$/${R}\1${N}/" -e t \
    -e "s/^(\[INFO\].*)\$/${G}\1${N}/" -e t \
    -e "s/^([[:space:]]*expected:.*)\$/${G}\1${N}/" -e t \
    -e "s/^([[:space:]]*but was:.*)\$/${R}\1${N}/" -e t \
    -e "s/^([[:space:]]+at .*)\$/${D}\1${N}/" -e t \
    -e "s/^(Caused by:.*)\$/${Y}\1${N}/" -e t \
    -e "s/^([A-Za-z_][A-Za-z0-9_.\$]*(Error|Exception).*)\$/${B}${R}\1${N}/"
}

# Locate a source file: $1 = path relative to src/test/java or src/main/java
find_src() {
  local base
  for base in src/test/java src/main/java; do
    if [ -f "$ROOT/$base/$1" ]; then
      printf '%s\n' "$ROOT/$base/$1"
      return 0
    fi
  done
  return 1
}

# Append an absolute file path (clickable in terminals such as Warp) to test-name lines
# and stack-frame lines
linkify() {
  if [ -z "$INTERACTIVE" ]; then
    cat
    return
  fi
  local re_frame='^([[:space:]]+at )(([a-z0-9_]+\.)*)[A-Za-z0-9_$]+\.[^(]+\(([A-Za-z0-9_]+\.java):([0-9]+)\)'
  local re_head='^(([a-z0-9_]+\.)+)([A-Z][A-Za-z0-9_$]*)\.([A-Za-z0-9_$]+) -- '
  local line p f ln cls m c n
  while IFS= read -r line; do
    if [[ $line =~ $re_frame ]]; then
      p="${BASH_REMATCH[2]}"; p="${p//.//}"
      f="${BASH_REMATCH[4]}"; ln="${BASH_REMATCH[5]}"
      c=$(find_src "$p$f") && line="$line  $c:$ln"
    elif [[ $line =~ $re_head ]]; then
      p="${BASH_REMATCH[1]}"; p="${p//.//}"
      cls="${BASH_REMATCH[3]}"; cls="${cls%%\$*}"
      m="${BASH_REMATCH[4]}"
      if c=$(find_src "$p$cls.java"); then
        n=$(grep -n -F "$m(" "$c" | head -1 | cut -d: -f1)
        line="$line  $c${n:+:$n}"
      fi
    fi
    printf '%s\n' "$line"
  done
}

# Compact the failure report using general rules:
#   - long lines keep their head and tail; the middle is cut
#   - the root cause (last "Caused by:") is kept longer than wrapping causes
#   - at most MVN_MAX_FRAMES stack frames are printed per failed test
#   - a failure whose message is identical to an earlier one (ignoring object hashes
#     like @1a2b3c) is collapsed to its test name, a note and its first stack frame
compact() {
  awk -v maxline="${MVN_MAX_LINE:-240}" \
      -v maxcause="${MVN_MAX_CAUSE:-120}" \
      -v maxroot="${MVN_MAX_ROOT:-300}" \
      -v maxframes="${MVN_MAX_FRAMES:-8}" '
    # Keep the head and the tail, cut the middle
    function trunc(s, n,   h) {
      if (length(s) <= n) return s
      h = int(n * 0.6)
      return substr(s, 1, h) " ...[truncated]... " substr(s, length(s) - (n - h) + 1)
    }
    function flush(   i, line, sig, lastIdx, firstFrame, frames, extra) {
      if (nb == 0) return
      # Failure signature = all non-stack content, with object hashes (@1a2b3c) removed
      sig = ""; lastIdx = 0; firstFrame = ""
      for (i = 2; i <= nb; i++) {
        line = blk[i]
        if (line ~ /^[[:space:]]*$/) continue
        if (line ~ /^[[:space:]]+at /) { if (firstFrame == "") firstFrame = line; continue }
        gsub(/@[0-9a-f]+/, "@", line)
        sig = sig "\n" line
        if (blk[i] ~ /^Caused by:/) lastIdx = i
      }
      print blk[1]
      if (sig in seen) {
        print "  (same error as above)"
        if (firstFrame != "") print firstFrame
        print ""
        nb = 0
        return
      }
      seen[sig] = 1
      frames = 0; extra = 0
      for (i = 2; i <= nb; i++) {
        line = blk[i]
        if (line ~ /^[[:space:]]*$/) continue
        if (line ~ /^[[:space:]]+at /) {
          if (frames < maxframes) { print line; frames++ } else extra++
          continue
        }
        if (i == lastIdx)               print trunc(line, maxroot)
        else if (line ~ /^Caused by:/)  print trunc(line, maxcause)
        else                            print trunc(line, maxline)
      }
      if (extra > 0) print "  ... +" extra " more frames"
      print ""
      nb = 0
    }
    /<<< (FAILURE|ERROR)!/ { flush(); nb = 1; blk[nb] = $0; next }
    { if (nb > 0) blk[++nb] = $0; else print }
    END { flush() }
  '
}

# Usage: run_mvn <goal and extra arguments>; returns Maven's exit code
run_mvn() {
  local LOG="target/mvn-last.log" MARK="target/.mvn-mark" code details f summary clean_out

  # Optional clean step (MVN_CLEAN=1, set by the --clean flag). It runs as a separate Maven
  # invocation: a combined "clean verify" would delete target/ together with the log file
  # and the marker created below.
  if [ -n "$MVN_CLEAN" ]; then
    if ! clean_out=$(./mvnw -B -ntp -q clean 2>&1); then
      echo "BUILD FAILED during clean" | colorize
      printf '%s\n' "$clean_out" | tail -20
      return 1
    fi
  fi

  mkdir -p target
  touch "$MARK"   # timestamp marker, so only reports from this run are read

  ./mvnw -B -ntp "$@" -Dmaven.test.redirectTestOutputToFile=true -DtrimStackTrace=true > "$LOG" 2>&1
  code=$?

  if [ $code -eq 0 ]; then
    grep -E 'Tests run:.*Skipped: [0-9]+$|BUILD SUCCESS' "$LOG" | tail -4 | colorize
    return 0
  fi

  echo "BUILD FAILED (exit $code) - full log: backend/$LOG" | colorize

  # 1) Failed/errored tests: details (expected/but was, trimmed stack) from the reports
  details=""
  for f in $(find target/surefire-reports target/failsafe-reports -name '*.txt' -newer "$MARK" 2>/dev/null); do
    if grep -qE '<<< (FAILURE|ERROR)!' "$f"; then
      details+="$(grep -vE '^-+$|^Test set:|^Tests run:.*-- in ' "$f")"$'\n'
    fi
  done

  if [ -n "$details" ]; then
    grep -E '^\[ERROR\] Tests run:.*Skipped: [0-9]+$' "$LOG" | tail -1 | colorize
    printf '%s\n' "$details" | compact | head -80 | linkify | colorize
    return $code
  fi

  # 2) No test reports (compile error, configuration error, ...): summarize the console log
  summary=$(grep -E '^\[ERROR\]|COMPILATION' "$LOG" \
    | sed -E 's/ ?-> \[Help [0-9]+\]//' \
    | grep -vE '^\[ERROR\] *$|^\[ERROR\] *(BUILD FAILURE|Re-run Maven|To see the full stack|For more information|\[Help|After correcting|mvn <args>)' \
    | awk '!seen[$0]++' \
    | head -60)
  if [ -n "$summary" ]; then
    if [ -n "$INTERACTIVE" ]; then
      # /abs/File.java:[10,5] -> /abs/File.java:10:5 so terminals recognize line and column
      summary=$(printf '%s\n' "$summary" | sed -E 's/(\.java):\[([0-9]+),([0-9]+)\]/\1:\2:\3/')
    fi
    printf '%s\n' "$summary" | colorize
  else
    echo "--- last 40 lines of log ---"
    tail -40 "$LOG"
  fi
  return $code
}