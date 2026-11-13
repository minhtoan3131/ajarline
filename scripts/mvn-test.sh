#!/usr/bin/env bash
# Runs the test phase (*Test) with compact output.
#
# Usage:
#   scripts/mvn-test.sh                                   # all *Test
#   scripts/mvn-test.sh UserServiceTest OrderServiceTest  # several classes
#   scripts/mvn-test.sh UserServiceTest#shouldCreate      # a single method
#   scripts/mvn-test.sh UserServiceTest#a+b OrderTest     # several methods + a class
#   scripts/mvn-test.sh 'User*Test'                       # wildcard (needs quotes)
#   scripts/mvn-test.sh --clean [names...]                # run "clean" first (-c also works)

source "$(dirname "${BASH_SOURCE[0]}")/lib-mvn.sh"

tests=""
MVN_CLEAN=""
for a in "$@"; do
  case "$a" in
    --clean|-c) MVN_CLEAN=1 ;;
    *)          tests="${tests:+$tests,}$a" ;;
  esac
done

args=(test)
if [ -n "$tests" ]; then
  args+=("-Dtest=$tests")
fi

run_mvn "${args[@]}"
exit $?