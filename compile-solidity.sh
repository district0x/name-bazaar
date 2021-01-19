#!/usr/bin/env bash
ENS=$(readlink -f ./node_modules/@ensdomains)

cd resources/public/contracts/src

function solc-err-only {
    solc @ensdomains=$ENS "$@" 2>&1 | grep -A 2 -i "Error"
}

# TODO use script for this

solc-err-only --overwrite --optimize --bin --abi OfferingRegistry.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi BuyNowOfferingFactory.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi AuctionOfferingFactory.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi District0xEmails.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi OfferingRequests.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi $ENS/ens/contracts/ENS.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi $ENS/ens/contracts/PublicResolver.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi $ENS/ens/contracts/ReverseRegistrar.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi Forwarder.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi NameBazaarRegistrar.sol -o ../build/

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
wc -c NameBazaarRegistrar.bin | awk '{print "NameBazaarRegistrar: " $1}'