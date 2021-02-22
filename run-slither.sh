#!/usr/bin/env bash

cd resources/public/contracts/src

# slither has problems with imports in node_modules so we use symbolic link hack
ln -s ../../../../node_modules/openzeppelin-solidity "openzeppelin-solidity"
ln -s ../../../../node_modules/@ensdomains @ensdomains

# Execute slither (which should be installed) and exclude findings found in
# dependencies.
#
# Exclude output of some slither detectors using the following arguments
# --exclude-low
# --exclude-medium
# --exclude-high
# --exclude-informational
# --exclude-optimization
slither --exclude-low --exclude-medium --exclude-informational --exclude-high . 2>&1 | grep -E -v "openzeppelin|@ensdomains" | grep -E -B 1 "\.sol#"

rm openzeppelin-solidity
rm @ensdomains