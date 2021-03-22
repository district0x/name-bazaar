#!/usr/bin/env bash

# This script is used for deployment, because it generates the ABI of the contracts
# which truffle compile doesn't do. However, the ABI can be extracted from the
# generated JSON files by truffle with simple script.
# TODO: migrate compilation to `truffle compile`

ENS=$(readlink -f ./node_modules/@ensdomains)
OZS=$(readlink -f ./node_modules/openzeppelin-solidity)

cd resources/public/contracts/src

function solc-err-only {
    solc @ensdomains=$ENS "openzeppelin-solidity"=$OZS "$@" 2>&1 | grep -A 2 -i "Error"
}

mkdir -p ../build
solc-err-only --overwrite --optimize --bin --abi OfferingRegistry.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi BuyNowOfferingFactory.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi AuctionOfferingFactory.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi District0xEmails.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi OfferingRequests.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi Forwarder.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi NameBazaarDevContracts.sol -o ../build/

cd ../build
wc -c OfferingRegistry.bin | awk '{print "OfferingRegistry: " $1}'
wc -c BuyNowOfferingFactory.bin | awk '{print "BuyNowOfferingFactory: " $1}'
wc -c BuyNowOffering.bin | awk '{print "BuyNowOffering: " $1}'
wc -c AuctionOfferingFactory.bin | awk '{print "AuctionOfferingFactory: " $1}'
wc -c AuctionOffering.bin | awk '{print "AuctionOffering: " $1}'
wc -c OfferingRequests.bin | awk '{print "OfferingRequests: " $1}'
wc -c DelegateProxy.bin | awk '{print "DelegateProxy: " $1}'
wc -c Forwarder.bin | awk '{print "Forwarder: " $1}'
wc -c PublicResolver.bin | awk '{print "PublicResolver: " $1}'
wc -c ReverseRegistrar.bin | awk '{print "ReverseRegistrar: " $1}'
wc -c ENSRegistry.bin | awk '{print "ENSRegistry: " $1}'
wc -c NamebazaarDevPublicResolver.bin | awk '{print "NamebazaarDevPublicResolver: " $1}'
wc -c NamebazaarDevNameResolver.bin | awk '{print "NamebazaarDevNameResolver: " $1}'
wc -c NamebazaarDevReverseRegistrar.bin | awk '{print "NamebazaarDevReverseRegistrar: " $1}'
wc -c NameBazaarDevRegistrar.bin | awk '{print "NameBazaarDevRegistrar: " $1}'
