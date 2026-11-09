#!/usr/bin/env bash
# Runs the verify phase (*Test first, then *IT) with compact output.
# A name ending in "IT" runs in the integration-test phase; any other name runs in the
# unit-test phase. With no names, everything runs.
#
# Usage:
#   scripts/mvn-verify.sh                                         # all *Test, then all *IT
#   scripts/mvn-verify.sh UserApiIT OrderApiIT                    # only these ITs
#   scripts/mvn-verify.sh UserServiceTest UserApiIT               # the given tests + ITs
#   scripts/mvn-verify.sh UserApiIT#shouldReturn404+shouldReturn201
#   scripts/mvn-verify.sh UserServiceTest#a+b UserApiIT#c         # mix of classes/methods
#   scripts/mvn-verify.sh --clean [names...]                      # run "clean" first (-c also works)

source "$(dirname "${BASH_SOURCE[0]}")/lib-mvn.sh"

unit=""
it=""
MVN_CLEAN=""
for a in "$@"; do
  case "$a" in
    --clean|-c) MVN_CLEAN=1; continue ;;
  esac
  cls="${a%%#*}"   # strip the #method part before checking the class name suffix
  if [[ "$cls" == *IT ]]; then
    it="${it:+$it,}$a"
  else
    unit="${unit:+$unit,}$a"
  fi
done

args=(verify)
if [ -n "$unit$it" ]; then
  # A phase with nothing specified is pointed at a dummy name so it runs nothing
  args+=("-Dtest=${unit:-__none__}" "-Dit.test=${it:-__none__}")
  args+=(-Dsurefire.failIfNoSpecifiedTests=false -Dit.failIfNoSpecifiedTests=false)
fi

run_mvn "${args[@]}"
exit $?