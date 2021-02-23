#!/usr/bin/env bash

cd resources/public/contracts/src

# slither has problems with imports in node_modules so we use symbolic link hack
ln -s ../../../../node_modules/openzeppelin-solidity "openzeppelin-solidity"
ln -s ../../../../node_modules/@ensdomains @ensdomains

# Executes slither and excludes findings found in dependencies.
#
# To exclude output of some slither detectors using the following arguments
# --exclude-low
# --exclude-medium
# --exclude-high
# --exclude-informational
# --exclude-optimization
#
# Many findings are false positives or only related to code style. Manual review
# is still necessary.
slither . --exclude-low --exclude-informational --exclude incorrect-equality 2>&1 | grep -E -v "openzeppelin|@ensdomains" | grep -E -B 1 "\.sol#"

rm openzeppelin-solidity
rm @ensdomains
